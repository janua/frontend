package idapiclient

import com.gu.identity.model.{Subscriber, LiftJsonConfig, User}
import client.{Anonymous, Auth, Response, Parameters}
import client.connection.Http
import scala.concurrent.{Future, ExecutionContext}
import client.parser.{JodaJsonSerializer, JsonBodyParser}
import idapiclient.responses.{CookiesResponse, AccessTokenResponse}
import client.connection.util.{ApiHelpers, ExecutionContexts}
import net.liftweb.json.JsonAST.{JValue, JNothing}
import net.liftweb.json.Serialization.write
import utils.SafeLogging
import idapiclient.requests.TokenPassword


abstract class IdApi(val apiRootUrl: String, http: Http, jsonBodyParser: JsonBodyParser, val clientAuth: Auth)
  extends IdApiUtils with SafeLogging with ApiHelpers {

  implicit def executionContext: ExecutionContext
  implicit val formats = LiftJsonConfig.formats + new JodaJsonSerializer

  def jsonField(field: String)(json: JValue): JValue = json \ field

  // AUTH

  def authApp(auth: Auth, trackingData: TrackingData): Future[Response[AccessTokenResponse]] = {
    val params = buildParams(Some(auth), Some(trackingData))
    val headers = buildHeaders(Some(auth))
    val response = http.GET(apiUrl("auth"), params, headers)
    response map jsonBodyParser.extract(jsonField("accessToken"))
  }

  def authBrowser(userAuth: Auth, trackingData: TrackingData): Future[Response[CookiesResponse]] = {
    val params = buildParams(Some(userAuth), Some(trackingData), Iterable("format" -> "cookies"))
    val headers = buildHeaders(Some(userAuth))
    val response = http.POST(apiUrl("auth"), None, params, headers)
    response map jsonBodyParser.extract(jsonField("cookies"))
  }

  def unauth(auth: Auth, trackingData: TrackingData): Future[Response[CookiesResponse]] = {
    val params = buildParams(Some(auth), Some(trackingData) )
    val headers = buildHeaders(Some(auth))
    val response = http.POST(apiUrl("unauth"), None, params, headers)
    response map jsonBodyParser.extract[CookiesResponse](jsonField("cookies"))
  }

  // USERS

  def user(userId: String, auth: Auth = Anonymous): Future[Response[User]] = {
    val apiPath = urlJoin("user", userId)
    val params = buildParams(Some(auth))
    val headers = buildHeaders(Some(auth))
    val response = http.GET(apiUrl(apiPath), params, headers)
    response map jsonBodyParser.extract(jsonField("user"))
  }

  def saveUser(userId: String, user: UserUpdate, auth: Auth): Future[Response[User]] = {
    val apiPath = urlJoin("user", userId)
    val params = buildParams(Some(auth))
    val headers = buildHeaders(Some(auth))
    val response = http.POST(apiUrl(apiPath), Some(write(user)), params, headers)
    response map jsonBodyParser.extract(jsonField("user"))
  }

  def me(auth: Auth): Future[Response[User]] = {
    val apiPath = urlJoin("user", "me")
    val params = buildParams(Some(auth))
    val response = http.GET(apiUrl(apiPath), params, buildHeaders(Some(auth)))
    response map jsonBodyParser.extract(jsonField("user"))
  }

  /**
   * data to save to a subdocument in the user's record
   * The path param provides the subdocument to be saved to e.g. prefs.myApp
   */
  def updateUser(userId: String, auth: Auth, trackingData: TrackingData, path: String, data: JValue): Future[Response[JValue]] = {
    val params = buildParams(tracking = Some(trackingData), auth = Some(auth))
    val headers = buildHeaders(auth = Some(auth))
    val pathParts = path.split('.').toList
    val response = http.POST(apiUrl(urlJoin("user" :: userId :: pathParts : _*)), Some(write(data)), params, headers)
    response map jsonBodyParser.extract(jsonField(pathParts.head))
  }

  def updateUser(user: User, auth: Auth, trackingData: TrackingData): Future[Response[User]] = {
    val params = buildParams(tracking = Some(trackingData), auth = Some(auth))
    val headers = buildHeaders(auth = Some(auth))
    val response = http.POST(apiUrl("user"), Some(write(user)), params, headers)
    response map jsonBodyParser.extract(jsonField("user"))
  }

  def register(user: User, trackingParameters: TrackingData): Future[Response[User]] = {
    val userData = write(user)
    val params = buildParams(tracking = Some(trackingParameters))
    val headers = buildHeaders(extra = trackingParameters.ipAddress.map(ip => Iterable("X-GU-ID-REMOTE-IP" -> ip)))
    val response = http.POST(apiUrl("user"), Some(userData), params, headers)
    response map jsonBodyParser.extract(jsonField("user"))
  }

  // PASSWORD RESET

  def userForToken( token : String ): Future[Response[User]] = {
    val apiPath = urlJoin("pwd-reset", "user-for-token")
    val params = buildParams(extra = Iterable("token" -> token))
    val response = http.GET(apiUrl(apiPath), params, buildHeaders())
    response map jsonBodyParser.extract(jsonField("user"))
  }

  def resetPassword( token : String, newPassword : String ): Future[Response[Unit]] = {
    val apiPath = urlJoin("pwd-reset", "reset-pwd-for-user")
    val postBody = write(TokenPassword(token, newPassword))
    val response = http.POST(apiUrl(apiPath), Some(postBody), clientAuth.parameters, clientAuth.headers)
    response map jsonBodyParser.extractUnit
  }

  def sendPasswordResetEmail(emailAddress : String, trackingParameters: TrackingData): Future[Response[Unit]] = {
    val apiPath = urlJoin("pwd-reset","send-password-reset-email")
    val params = buildParams(tracking = Some(trackingParameters), extra = Iterable("email-address" -> emailAddress, "type" -> "reset"))
    val response = http.GET(apiUrl(apiPath), params, buildHeaders())
    response map jsonBodyParser.extractUnit
  }

  // EMAILS

  def userEmails(userId: String, trackingParameters: TrackingData): Future[Response[Subscriber]] = {
    val apiPath = urlJoin("useremails", userId)
    val params = buildParams(tracking = Some(trackingParameters))
    val response = http.GET(apiUrl(apiPath), params, buildHeaders())
    response map jsonBodyParser.extract(jsonField("result"))
  }

  def updateUserEmails(userId: String, subscriber: Subscriber, auth: Auth, trackingParameters: TrackingData): Future[Response[Unit]] = {
    val apiPath = urlJoin("useremails", userId)
    val params = buildParams(tracking = Some(trackingParameters), auth = Some(auth))
    val headers = buildHeaders(auth = Some(auth))
    val response = http.POST(apiUrl(apiPath), Some(write(subscriber)), params, headers)
    response map jsonBodyParser.extractUnit
  }

  def resendEmailValidationEmail(auth: Auth, trackingParameters: TrackingData): Future[Response[Unit]] = {
    val apiPath = urlJoin("user","resend-validtion-email")
    val params = buildParams(tracking = Some(trackingParameters), auth = Some(auth))
    val response = http.GET(apiUrl(apiPath), params, buildHeaders())
    response map jsonBodyParser.extractUnit
  }  
}

class SynchronousIdApi(apiRootUrl: String, http: Http, jsonBodyParser: JsonBodyParser, clientAuth: Auth)
  extends IdApi(apiRootUrl, http, jsonBodyParser, clientAuth) {
  implicit def executionContext: ExecutionContext = ExecutionContexts.currentThreadContext
}

trait IdApiUtils {
  val apiRootUrl: String
  val clientAuth: Auth

  implicit object ParamsOpt2Params extends (Option[Parameters] => Parameters) {
    def apply(paramsOpt: Option[Parameters]): Parameters = paramsOpt.getOrElse(Iterable.empty)
  }

  protected def buildParams(auth: Option[Auth] = None,
                            tracking: Option[TrackingData] = None,
                            extra: Parameters = Iterable.empty): Parameters = {
    extra ++ clientAuth.parameters ++
      auth.map(_.parameters) ++
      tracking.map({ trackingData =>
        trackingData.parameters ++ trackingData.ipAddress.map(ip => "ip" -> ip)
      })
  }

  protected def buildHeaders(auth: Option[Auth] = None, extra: Parameters = Iterable.empty): Parameters = {
    extra ++ clientAuth.headers ++ auth.map(_.headers)
  }

  protected def apiUrl(path: String) = urlJoin(apiRootUrl, path)

  protected def urlJoin(pathParts: String*) = {
    pathParts.filter(_.nonEmpty).map(slug => {
      slug.stripPrefix("/").stripSuffix("/")
    }) mkString "/"
  }
}