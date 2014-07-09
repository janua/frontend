package controllers

import conf.Configuration
import common.{UserIdentity, GoogleAuth, GoogleAuthConfig, ExecutionContexts}
import play.api.mvc._
import scala.concurrent.duration._
import play.api.mvc.Results._
import play.api.mvc.SimpleResult
import play.api.libs.json.{JsNumber, JsString, JsObject, Json}
import play.Play
import scala.concurrent.Future
import org.joda.time.DateTime

object Authenticated extends AuthAction(routes.Login.login.url)

object ExpiringAuthentication extends ExpiringAuthAction("/login")

object AjaxExpiringAuthentication extends ExpiringAuthAction("/login") {
  lazy val errorResponse =
    JsObject(
      Seq("error" -> JsObject(
        Seq(
            "status" -> JsNumber(403),
            "message" -> JsString("Forbidden")
            )
        )
      )
    )
  override def authFailResult(request: Request[AnyContent]): SimpleResult = Forbidden(Json.toJson(errorResponse))
}

object Login extends Controller with ExecutionContexts {

  val LOGIN_ORIGIN_KEY = "loginOriginUrl"
  val ANTI_FORGERY_KEY = "antiForgeryToken"
  val googleAuthConfig =
    GoogleAuthConfig(
      "283258724824-gv88hsfbl2os5n9qlt2ocs8bu94cao5r.apps.googleusercontent.com",  // The client ID from the dev console
      "GMrS7OdO0cHCdb4pfRsm20wv",                  // The client secret from the dev console
      "http://localhost:9001/oauth2callback",      // The redirect URL Google send users back to (must be the same as
      //    that configured in the developer console)
      Some("guardian.co.uk"),                       // Google App domain to restrict login
      maxAuthAge = 0
    )

  // this is the only place we use LoginAuthAction - to prevent authentication redirect loops
  def login = Action { request =>
    val error = request.flash.get("error")
    Ok(views.html.auth.login(error, "Test"))
  }

  /*
  Redirect to Google with anti forgery token (that we keep in session storage - note that flashing is NOT secure)
   */
  def loginAction = Action.async { implicit request =>
    val antiForgeryToken = GoogleAuth.generateAntiForgeryToken()
    GoogleAuth.redirectToGoogle(googleAuthConfig, antiForgeryToken).map {
      _.withSession { session + (ANTI_FORGERY_KEY -> antiForgeryToken) }
    }
  }


  /*
  User comes back from Google.
  We must ensure we have the anti forgery token from the loginAction call and pass this into a verification call which
  will return a Future[UserIdentity] if the authentication is successful. If unsuccessful then the Future will fail.

   */
  def oauth2Callback = Action.async { implicit request =>
    println("OAuthCallback")
    session.get(ANTI_FORGERY_KEY) match {
      case None =>
        println("No AntiForgery")
        Future.successful(Redirect(routes.Login.login()).flashing("error" -> "Anti forgery token missing in session"))
      case Some(token) =>
        GoogleAuth.validatedUserIdentity(googleAuthConfig, token).map { identity =>
        // We store the URL a user was trying to get to in the LOGIN_ORIGIN_KEY in AuthAction
        // Redirect a user back there now if it exists
          val redirect = session.get(LOGIN_ORIGIN_KEY) match {
            case Some(url) => Redirect(url)
            case None => Redirect(routes.FaciaToolController.priorities())
          }
          // Store the JSON representation of the identity in the session - this is checked by AuthAction later
          redirect.withSession {
            session + (UserIdentity.KEY -> Json.toJson(identity).toString) - ANTI_FORGERY_KEY - LOGIN_ORIGIN_KEY + (Configuration.cookies.lastSeenKey , DateTime.now.toString)
          }
        } recover {
          case t =>
            println(s"Recovering from ${t.toString}")
            // you might want to record login failures here - we just redirect to the login page
            Redirect(routes.Login.login())
              .withSession(session - ANTI_FORGERY_KEY)
              .flashing("error" -> s"Login failure: ${t.toString}")
        }
    }
  }

  def logout = Action { implicit request =>
    Redirect(routes.Login.login()).withNewSession
  }
}
