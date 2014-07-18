package jobs

import common.{FrontendMetric, ExecutionContexts, Logging}
import services.{OphanApi, CloudWatch}
import scala.collection.JavaConversions._
import scala.concurrent.Future.sequence
import org.joda.time.DateTime

object AnalyticsSanityCheckJob extends ExecutionContexts with implicits.Futures with Logging {

  def run() {

    val rawPageViews = CloudWatch.rawPageViews.map(
      _.getDatapoints.headOption.map(_.getSum.doubleValue()).getOrElse(0.0)
    )

    val analyticsPageViews = CloudWatch.analyticsPageViews.map(
      _.getDatapoints.headOption.map(_.getSum.doubleValue()).getOrElse(0.0)
    )

    def sensible(what :Double) : Double = {
      if (what > 200) 200.0 else what
    }

    val omniture = sequence(Seq(rawPageViews, analyticsPageViews)).map{
      case (raw :: analytics:: Nil) => sensible(analytics / raw * 100)
    }

    val ophan = sequence(Seq(rawPageViews, ophanViews)).map{
      case (raw :: analytics:: Nil) => sensible(analytics / raw * 100)
    }

    sequence(Seq(omniture, ophan)).foreach{ case (omniture :: ophan :: Nil) =>
      model.diagnostics.CloudWatch.put("Analytics", Map(
        "omniture-percent-conversion" -> FrontendMetric(omniture),
        "ophan-percent-conversion" -> FrontendMetric(ophan),
        "omniture-ophan-correlation" -> FrontendMetric(omniture/ophan * 100)
      ))
    }
 }

  private def ophanViews = {
    val now = new DateTime().minusMinutes(15).getMillis
    OphanApi.getBreakdown("next-gen", hours = 1).map { json =>
      (json \\ "data").flatMap {
        line =>
          val recent = line.asInstanceOf[play.api.libs.json.JsArray].value.filter {
            entry =>
              (entry \ "dateTime").as[Long] > now
          }
          recent.map(r => (r \ "count").as[Long]).toSeq
      }.sum.toDouble
    }
  }
}