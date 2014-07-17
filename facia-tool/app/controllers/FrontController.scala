package controllers

import frontsapi.model.{Collection, Front}
import play.api.mvc.Controller
import services.PressAndNotify
import util.Requests._
import play.api.libs.json.Json
import config.UpdateManager
import com.gu.googleauth.UserIdentity

object CreateFront {
  implicit val jsonFormat = Json.format[CreateFront]
}

case class CreateFront(
  id: String,
  navSection: Option[String],
  webTitle: Option[String],
  title: Option[String],
  description: Option[String],
  priority: Option[String],
  initialCollection: Collection
)

object FrontController extends Controller {
  def create = AuthActions.AuthAction { request =>
    request.body.read[CreateFront] match {
      case Some(createFrontRequest) =>
        val identity = UserIdentity.fromRequestHeader(request).get
        val newCollectionId = UpdateManager.createFront(createFrontRequest, identity)
        PressAndNotify(Set(newCollectionId))
        Ok

      case None => BadRequest
    }
  }

  def update(frontId: String) = AuthActions.AuthAction { request =>
    request.body.read[Front] match {
      case Some(front) =>
        val identity = UserIdentity.fromRequestHeader(request).get
        UpdateManager.updateFront(frontId, front, identity)
        Ok

      case None => BadRequest
    }
  }
}
