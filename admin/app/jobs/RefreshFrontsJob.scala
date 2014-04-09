package jobs

import services.{S3FrontsApi, FrontPressNotification}
import play.api.libs.json.{JsValue, Json}
import conf.Switches._
import conf.Configuration
import scala.concurrent.Future
import common.ExecutionContexts

object RefreshFrontsJob extends ExecutionContexts {

  def getPaths: Future[Option[Seq[String]]] = {
    for (pathsOption <- S3FrontsApi.getMasterConfig) yield {
      val json = Json.parse(pathsOption.body)
      (json \ "fronts").asOpt[Map[String, JsValue]].map(_.keys.toSeq)
    }
  }

  def run(): Unit = {
    if (FrontPressJobSwitch.isSwitchedOn && Configuration.aws.frontPressSns.filter(_.nonEmpty).isDefined) {
      getPaths map(_.map(_.map(FrontPressNotification.sendWithoutSubject)))
    }
  }
}
