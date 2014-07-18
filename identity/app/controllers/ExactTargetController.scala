package controllers

import play.api.mvc._
import common.{FrontendMetric, ExecutionContexts}
import services.{IdRequestParser, ReturnUrlVerifier}
import com.google.inject.{Inject, Singleton}
import utils.SafeLogging
import scala.collection.convert.wrapAsJava._
import conf.IdentityConfiguration
import play.api.libs.ws._
import model.diagnostics.CloudWatch
import exacttarget.{SubscriptionDef, DataExtId}

@Singleton
class ExactTargetController @Inject()(
                                       conf: IdentityConfiguration,
                                       returnUrlVerifier: ReturnUrlVerifier,
                                       idRequestParser: IdRequestParser,
                                       authAction: actions.AuthenticatedAction)
  extends Controller with ExecutionContexts with SafeLogging {

  def cloudWatchCount(id: String) { CloudWatch.put("ExactTarget", Map(id -> FrontendMetric(1d))) }

  def subscribe(subscriptionDefId: String, returnUrl: String) = authAction.apply {
    implicit request =>
      cloudWatchCount(s"sub-$subscriptionDefId-request")

      idRequestParser(request).returnUrl match {
        case Some(verifiedReturnUrl) =>
          val user = request.user
          for {
            exactTargetFactory <- conf.exacttarget.factory
            emailAddress: String <- Option(user.getPrimaryEmailAddress) if !emailAddress.isEmpty
            subscriptionDef <- SubscriptionDef.All.get(subscriptionDefId)
          } {
            val automaticParameters = Map("EmailAddress" -> emailAddress, "Field_A" -> user.getId)

            val dataExtId = subscriptionDef.dataExtension
            val parameters = subscriptionDef.parameters ++ automaticParameters

            val triggeredEmailRequest =
              exactTargetFactory.createRequest(emailAddress, parameters, "Create", dataExtId.businessUnitId, dataExtId.customerKey)

            WS.url(exactTargetFactory.endPoint.toString).withHeaders(
              "Content-Type" -> "text/xml; charset=utf-8",
              "SOAPAction" -> triggeredEmailRequest.getSoapAction
            ).post(triggeredEmailRequest.getSoapEnvelopeString).onSuccess {
              case resp =>
                (resp.xml \\ "CreateResponse" \ "Results") map {
                  subscriberNode =>
                    val statusCode = (subscriberNode \ "StatusCode").text.trim
                    val statusMessage = (subscriberNode \ "StatusMessage").text.trim
                    cloudWatchCount(s"sub-$subscriptionDefId-et-api-response-$statusCode")
                    logger.info(s"CreateResponse - $statusCode : $statusMessage")
                }
            }
          }

          SeeOther(verifiedReturnUrl)
        case None =>
          SeeOther(returnUrlVerifier.defaultReturnUrl)
      }
  }
}