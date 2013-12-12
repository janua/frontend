package controllers

import play.api.mvc.{Action, Controller}
import common.{ExecutionContexts, AkkaAgent, Logging}
import service.SNS
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WS
import java.security.cert.{X509Certificate, Certificate, CertificateFactory}
import com.sun.org.apache.xml.internal.security.utils.Base64
import java.security.Signature
import controllers.front.CollectionAgent


object VerifySNSRequest extends ExecutionContexts with Logging {

  val certificateAgent = AkkaAgent[Option[Certificate]](None)

  def verifySubscription(json: JsValue): Boolean = verify(json, generateStringForSubscription(json))
  def verifyNotification(json: JsValue): Boolean = verify(json, generateStringForNotification(json))

  private def verify(json: JsValue, signedString: String): Boolean = {
    (for {
      certificate <- certificateAgent.get()
      signature <- (json \ "Signature").asOpt[String] map Base64.decode
    } yield {
      certificateAgent.get().exists{ cert =>
        try {
          val sig: Signature = Signature.getInstance("SHA1withRSA")
          sig.initVerify(cert.getPublicKey)
          sig.update(signedString.getBytes)
          sig.verify(signature)
        }
        catch {
          case t: Throwable => {
            log.error(s"SNS Signature Verification: $t")
            false
          }
        }
      }
    }).getOrElse {
      log.warn("No certificate to verify against")
      false
    }
  }

  def generateStringToSign(json: JsValue): Option[String] = {
    def getString(json: JsValue)(value: String): Option[String] = (json \ value).asOpt[String]
    val get: (String) => Option[String] = getString(json)
    for {
      message           <- get("Message")
      messageId         <- get("MessageId")
      subject           <- get("Subject")
      timestamp         <- get("Timestamp")
      topicArn          <- get("TopicArn")
      notificationType  <- get("Type")
    } yield {
      s"Message\n$message\n" +
      s"MessageId\n$messageId\n" +
      s"Subject\n$subject\n" +
      s"Timestamp\n$timestamp\n" +
      s"TopicArn\n$topicArn\n" +
      s"Type\n$notificationType\n"
    }
  }

  def generateStringForNotification(json: JsValue): String = {
    val values = Seq("Message", "MessageId", "Subject", "Timestamp", "TopicArn", "Type")
    val seq = values.foldLeft(Seq[String]()) {case (s, v) => s :+ v :+ (json \ v).asOpt[String].getOrElse("")}
    seq.mkString(start="", sep="\n", end="\n")
  }

  def generateStringForSubscription(json: JsValue): String = {
    val values = Seq("Message", "MessageId", "SubscribeURL", "Timestamp", "Token", "TopicArn", "Type")
    val seq = values.foldLeft(Seq[String]()) {case (s, v) => s :+ v :+ (json \ v).asOpt[String].getOrElse("")}
    seq.mkString(start="", sep="\n", end="\n")
  }

  def updateCertificate(json: JsValue) {
    for (certificateUrl <- (json \ "SigningCertURL").asOpt[String])
    {
      WS.url(certificateUrl).get() map { response =>
        val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        val certificate: X509Certificate = cf.generateCertificate(response.getAHCResponse.getResponseBodyAsStream).asInstanceOf[X509Certificate]
        certificateAgent.send(Option(certificate))
      }
    }
  }
}


object SubscriptionConfirmation {
  def unapply(json: JsValue): Option[String] =
    if ((json \ "Type").asOpt[String].exists(_ == "SubscriptionConfirmation"))
      (json \ "Token").asOpt[String]
    else
      None
}

object Notification {
  def unapply(json: JsValue): Option[JsValue] =
    if ((json \ "Type").asOpt[String].exists(_ == "Notification") && VerifySNSRequest.verifyNotification(json)) Some(json) else None
}


class NotifyController extends Controller with Logging {

  def receiveNotification = Action { request =>
    val json = Json.parse(request.body.asText.get)
    json match {
      case SubscriptionConfirmation(token) => {
        log.info(s"Confirming SNS Subscription for token $token")
        SNS.confirmSubscription(token)
        VerifySNSRequest.updateCertificate(json)
        Ok
      }
      case Notification(js) => {
        log.info(s"Received notification for subject: ${(js \ "Subject").asOpt[String].getOrElse("")}")
        (json \ "Message").asOpt[String].map { collectionId =>
          CollectionAgent.updateCollectionById(collectionId)
        }
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

