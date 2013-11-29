package controllers

import play.api.mvc.{Action, Controller}
import common.Logging
import service.SNS
import play.api.libs.json.{JsValue, Json}


object SubscriptionConfirmation {
  def unapply(s: String): Option[SubscriptionConfirmation.type] = if (s == "SubscriptionRequest") Some(SubscriptionConfirmation) else None
  def unapply(json: JsValue): Option[String] =
    if ((json \ "Type").asOpt[String].filter(_ == "SubscriptionConfirmation").isDefined)
      (json \ "Token").asOpt[String]
    else
      None
}

object Notification {
  def unapply(s: String): Option[Notification.type] = if (s == "Notification") Some(Notification) else None
  def unapply(json: JsValue): Option[JsValue] = if ((json \ "Type").asOpt[String].filter(_ == "Notification").isDefined) Some(json) else None
}


class NotifyController extends Controller with Logging {

  def receiveNotification = Action { request =>
    val json = Json.parse(request.body.asText.get)
    json match {
      case SubscriptionConfirmation(token) => {
        log.info(s"Confirming SNS Subscription for token $token")
        SNS.confirmSubscription(token)
        Ok
      }
      case Notification(js) => {
        log.info(s"Received notification for subject: ${(js \ "Subject").asOpt[String].getOrElse("")}")
        Ok
      }
      case _  => InternalServerError
    }
  }

  def explicitSubscribe = Action {
    SNS.subscribe
    Ok
  }

  def explicitUnsubscribe = Action {
    SNS.unsubscribe
    Ok
  }

}

object NotifyController extends NotifyController

