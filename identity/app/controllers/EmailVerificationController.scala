package controllers

import play.api.mvc.{Controller, Action}
import com.google.inject.{Inject, Singleton}
import idapiclient.IdApiClient
import services.{IdentityUrlBuilder, IdRequestParser}
import common.ExecutionContexts
import utils.SafeLogging
import model.IdentityPage
import play.api.libs.json._

@Singleton
class EmailVerificationController @Inject()( api : IdApiClient, idRequestParser: IdRequestParser, idUrlBuilder: IdentityUrlBuilder, authAction: actions.AuthAction )
  extends Controller with ExecutionContexts with SafeLogging {

  val page = IdentityPage("/verify-email", "Verify Email", "verify-email")

  def verify( token : String) = Action { implicit request =>
    val idRequest = idRequestParser(request)
    Ok(views.html.email_verified(page, idRequest, idUrlBuilder))
  }

  def resendVerificationEmail() = { 

    authAction.async { implicit request =>

        val idRequest = idRequestParser(request)

        api.resendEmailValidationEmail(request.auth, idRequest.trackingData) map {
          case Left(errors) =>
            val errorString = errors.mkString(",")
            Ok(Json.parse(s"{'error':'${errorString}'}"))
          case Right(apiOk) => Ok(Json.parse("{'status':'ok'}"))
        }

    }

  }

}
