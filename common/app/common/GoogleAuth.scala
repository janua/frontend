package common

import scala.concurrent.ExecutionContext
import play.api.libs.ws.{Response, WS}
import scala.language.postfixOps
import java.math.BigInteger
import java.security.SecureRandom
import org.apache.commons.codec.binary.Base64
import play.api.libs.json.{JsValue, Format, Json}
import play.api.mvc._
import scala.concurrent.Future
import conf.Configuration
import org.joda.time.DateTime

case class UserIdentity(sub: String, email: String, firstName: String, lastName: String, exp: Long) {
  lazy val fullName = firstName + " " + lastName
  lazy val emailDomain = email.split("@").last
  lazy val asJson = Json.stringify(Json.toJson(this))
  lazy val isValid = System.currentTimeMillis < exp * 1000
}

object UserIdentity {
  implicit val userIdentityFormats: Format[UserIdentity] = Json.format[UserIdentity]
  val KEY = "identity"
  def fromJson(json: JsValue): Option[UserIdentity] = json.asOpt[UserIdentity]
  def fromRequestHeader(request: RequestHeader): Option[UserIdentity] = {
    request.session.get(KEY).flatMap(credentials => UserIdentity.fromJson(Json.parse(credentials)))
  }
  def fromRequest(request: Request[Any]): Option[UserIdentity] = fromRequestHeader(request)
}


case class DiscoveryDocument(authorization_endpoint: String, token_endpoint: String, userinfo_endpoint: String)
object DiscoveryDocument {
  val url = "https://accounts.google.com/.well-known/openid-configuration"
  implicit val discoveryDocumentReads = Json.reads[DiscoveryDocument]
  def fromJson(json: JsValue) = Json.fromJson[DiscoveryDocument](json).getOrElse(
    throw new IllegalArgumentException("Invalid discovery document")
  )
}

case class Token(access_token:String, token_type:String, expires_in:Long, id_token:String) {
  val jwt = JsonWebToken(id_token)
}
object Token {
  implicit val tokenReads = Json.reads[Token]
  def fromJson(json:JsValue):Token = Json.fromJson[Token](json).get
}

case class JwtClaims(iss: String, sub:String, azp: String, email: String, at_hash: String, email_verified: Boolean,
                     aud: String, hd: Option[String], iat: Long, exp: Long)
object JwtClaims {
  implicit val claimsReads = Json.reads[JwtClaims]
}

case class UserInfo(kind: String, gender: Option[String], sub: Option[String], name: String, given_name: String, family_name: String,
                    profile: Option[String], picture: Option[String], email: String, email_verified: String, locale: String, hd: String)
object UserInfo {
  implicit val userInfoReads = Json.reads[UserInfo]
  def fromJson(json:JsValue):UserInfo = json.as[UserInfo]
}

case class JsonWebToken(jwt: String) {
  val jwtParts: Array[String] = jwt.split('.')
  val Array(headerJson: JsValue, claimsJson: JsValue) = jwtParts.take(2).map(Base64.decodeBase64).map(Json.parse)
  val claims = claimsJson.as[JwtClaims]
}

case class ErrorInfo(domain: String, reason: String, message: String)
object ErrorInfo {
  implicit val errorInfoReads = Json.reads[ErrorInfo]
}
case class Error(errors: Seq[ErrorInfo], code: Int, message: String)
object Error {
  implicit val errorReads = Json.reads[Error]
}

case class GoogleAuthConfig(clientId: String, clientSecret: String, redirectUrl: String, domain: Option[String], maxAuthAge: Int)

class GoogleAuthException(val message: String, val throwable: Throwable = null) extends Exception(message, throwable)

object GoogleAuth {
  var discoveryDocumentHolder: Option[Future[DiscoveryDocument]] = None

  def discoveryDocument(implicit context: ExecutionContext): Future[DiscoveryDocument] =
    if (discoveryDocumentHolder.isDefined) discoveryDocumentHolder.get
    else {
      val discoveryDocumentFuture = WS.url(DiscoveryDocument.url).get().map(r => DiscoveryDocument.fromJson(r.json))
      discoveryDocumentHolder = Some(discoveryDocumentFuture)
      discoveryDocumentFuture
    }

  val random = new SecureRandom()

  def generateAntiForgeryToken() = new BigInteger(130, random).toString(32)

  def googleResponse[T](r: Response)(block: JsValue => T): T = {
    r.status match {
      case errorCode if errorCode >= 400 =>
        // try to get error if google sent us an error doc
        val error = (r.json \ "error").asOpt[Error]
        error.map {
          e =>
            throw new GoogleAuthException(s"Error when calling Google: ${e.message}")
        }.getOrElse {
          throw new GoogleAuthException(s"Unknown error when calling Google [status=$errorCode, body=${r.body}]")
        }
      case normal => block(r.json)
    }
  }

  def redirectToGoogle(config: GoogleAuthConfig, antiForgeryToken: String)
                      (implicit context: ExecutionContext): Future[SimpleResult] = {
    val queryString: Map[String, Seq[String]] = Map(
      "max_auth_age" -> Seq(config.maxAuthAge.toString),
      "client_id" -> Seq(config.clientId),
      "response_type" -> Seq("code"),
      "scope" -> Seq("openid email profile"),
      "redirect_uri" -> Seq(config.redirectUrl),
      "state" -> Seq(antiForgeryToken)
    ) ++ config.domain.map(domain => "hd" -> Seq(domain))

    discoveryDocument.map(dd => Redirect(s"${dd.authorization_endpoint}", queryString))
  }

  def validatedUserIdentity(config: GoogleAuthConfig, expectedAntiForgeryToken: String)
                           (implicit request: RequestHeader, context: ExecutionContext): Future[UserIdentity] = {
    if (!request.queryString.getOrElse("state", Nil).contains(expectedAntiForgeryToken)) {
      throw new IllegalArgumentException("The anti forgery token did not match")
    } else {
      discoveryDocument.flatMap {
        dd =>
          val code = request.queryString("code")
          WS.url(dd.token_endpoint).post {
            Map(
              "code" -> code,
              "client_id" -> Seq(config.clientId),
              "client_secret" -> Seq(config.clientSecret),
              "redirect_uri" -> Seq(config.redirectUrl),
              "grant_type" -> Seq("authorization_code")
            )
          }.flatMap {
            response =>
              googleResponse(response) {
                json =>
                  val token = Token.fromJson(json)
                  val jwt = token.jwt
                  WS.url(dd.userinfo_endpoint)
                    .withHeaders("Authorization" -> s"Bearer ${token.access_token}")
                    .get().map {
                    response =>
                      googleResponse(response) {
                        json =>
                          println(s"Parsing UserInfo from $json")
                          val userInfo = UserInfo.fromJson(json)
                          println(s"UserInfo: $userInfo")
                          UserIdentity(
                            jwt.claims.sub,
                            jwt.claims.email,
                            userInfo.given_name,
                            userInfo.family_name,
                            jwt.claims.exp
                          )
                      }
                  }
              }
          }
      }
    }
  }
}

case class LoginExemptions(login: Option[Call], loginAction: Option[Call], oauthCallback: Option[Call]) {
  def isRequestExempt(request: RequestHeader): Boolean =
    login.exists(c => request.path.startsWith(c.url)) ||
      loginAction.exists(c => request.path.startsWith(c.url)) ||
      oauthCallback.exists(c => request.path.startsWith(c.url))
}

object AssetExemptions {
  def isRequestExempt(request: RequestHeader): Boolean = request.path.startsWith("/assets/")
}

object GoogleAuthFilters extends Logging with ExecutionContexts with implicits.Dates {

  def withinAllowedTime(session: Session): Boolean = {
    println(session.get(Configuration.cookies.lastSeenKey))
    session.get(Configuration.cookies.lastSeenKey).map(new DateTime(_)).exists(_.age < Configuration.cookies.sessionExpiryTime)
  }

  class AuthFilterWithExemptions(loginExemptions: LoginExemptions) extends Filter {
    def apply(nextFilter: (RequestHeader) => Future[SimpleResult])
             (requestHeader: RequestHeader): Future[SimpleResult] = {

      println(requestHeader.path)
      if (loginExemptions.isRequestExempt(requestHeader) ||
        AssetExemptions.isRequestExempt(requestHeader)) {
        println("Was Exempt")
        nextFilter(requestHeader)
      }
      else {
          if (withinAllowedTime(requestHeader.session))
            UserIdentity.fromRequestHeader(requestHeader) match {
              case Some(identity) if identity.isValid => {
                println(s"Valid! ${requestHeader.uri}")
                nextFilter(requestHeader).map(r => r.withSession(Configuration.cookies.lastSeenKey -> DateTime.now.toString()))
              }
              case otherIdentity =>
                Future.successful(loginExemptions.login.map(Redirect(_).withHeaders("Invalid" -> "Broke")).getOrElse(Ok("Broke")))
            }
          else {
            if (requestHeader.isXmlHttpRequest)
              Future.successful(Forbidden.withNewSession)
            else
              Future.successful(Redirect("/login").withNewSession)
          }
      }
    }
  }

}