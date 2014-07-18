import common.{FrontendMetric, ContentApiMetrics, CloudWatchApplicationMetrics}
import conf.{Management, Filters}
import dev.DevParametersLifecycle
import dfp.DfpAgentLifecycle
import ophan.SurgingContentAgentLifecycle
import play.api.mvc.WithFilters

object Global
  extends WithFilters(Filters.common: _*)
  with DevParametersLifecycle
  with DfpAgentLifecycle
  with CloudWatchApplicationMetrics
  with SurgingContentAgentLifecycle {
  override lazy val applicationName = Management.applicationName

  override def applicationMetrics: Map[String, FrontendMetric] = super.applicationMetrics ++ Map(
    ("elastic-content-api-calls", FrontendMetric(ContentApiMetrics.ElasticHttpTimingMetric.getAndReset.toDouble)),
    ("elastic-content-api-timeouts", FrontendMetric(ContentApiMetrics.ElasticHttpTimeoutCountMetric.getAndReset.toDouble))
  )
}
