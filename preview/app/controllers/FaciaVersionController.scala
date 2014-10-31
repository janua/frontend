package controllers

import common.Logging
import controllers.front.FrontJson
import model.Cached
import play.api.mvc.{Action, Controller}
import services.S3FrontsApi
import views.support.TemplateDeduping

object FrontJsonWithStage extends FrontJson with implicits.Requests {
  override val bucketLocation: String = s"$stage/frontsapi/pressed/live"
}

object FaciaVersionController extends Controller with Logging {

  implicit def getTemplateDedupingInstance: TemplateDeduping = TemplateDeduping()

  def renderFrontPressResult(stage: String, path: String) = Action { implicit request =>
    val versionId: Option[String] = request.getQueryString("versionId")
    val pressedString: Option[String] = S3FrontsApi.getObjectHistoryVersion(stage, path, versionId)

    pressedString.flatMap(FrontJsonWithStage.parsePressedJson).map { faciaPage =>
      Cached(faciaPage) {
        Ok(views.html.front(faciaPage))
      }
    }.getOrElse(Cached(60)(NotFound))
  }
}
