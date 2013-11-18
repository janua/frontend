package controllers

import play.api.mvc.{Action, Controller}
import common.{ExecutionContexts, Logging}
import service.SNS
import play.api.libs.json.Json

object SubscriptionRequest {
  def unapply(s: String): Option[SubscriptionRequest.type] = if (s == "SubscriptionRequest") Some(SubscriptionRequest) else None
}

object Notification {
  def unapply(s: String): Option[Notification.type] = if (s == "Notification") Some(Notification) else None
}


class NotifyController extends Controller with Logging with JsonTrails with ExecutionContexts {

  def receiveNotification = Action { request =>
    println("Received Notification")
    println("Body: " + request.body.toString)
    println("Mime Type: " + request.mediaType)
    val json = Json.parse(request.body.asText.get)
    println("JSON: " + json.toString)
    for {
      token <- (json \ "Token").asOpt[String]
      arn <- (json \ "TopicArn").asOpt[String]
    }
    {
      println("Confirming Subscription!")
      SNS.confirmSubscription(token)
    }
    Ok
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