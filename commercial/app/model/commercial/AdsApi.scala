package model.commercial

import scala.concurrent.Future
import play.api.libs.json.{Json, JsValue}
import play.api.libs.ws.WS
import common.{ExecutionContexts, Logging}
import scala.xml.{XML, Elem}
import com.ning.http.util.AsyncHttpProviderUtils
import conf.Switch

trait AdsApi[F, T <: Ad] extends ExecutionContexts with Logging {

  protected val switch: Switch

  protected val adTypeName: String

  protected def url: Option[String]

  // following RFC-2616#3.7.1
  protected val characterEncoding: String = AsyncHttpProviderUtils.DEFAULT_CHARSET

  protected val loadTimeout: Int = 2000

  protected def transform(body: String): F

  def parse(feed: F): Seq[T]

  def loadAds(): Future[Seq[T]] = doIfSwitchedOn {
    url map {
      u =>
        val fads = WS.url(u) withRequestTimeout loadTimeout get() map {
          response => {
            val body = {
              // look at documentation of response.body to see why this is necessary
              if (characterEncoding == AsyncHttpProviderUtils.DEFAULT_CHARSET)
                response.body
              else
                response.getAHCResponse.getResponseBody(characterEncoding)
            }
            val feed = transform(body)
            parse(feed)
          }
        }

        fads onSuccess {
          case ads => log.info(s"Loaded ${ads.size} $adTypeName from $u")
        }
        fads onFailure {
          case e: Exception => log.error(s"Loading $adTypeName ads from $u failed: ${e.getMessage}")
        }

        fads

    } getOrElse {
      log.warn(s"Missing $adTypeName URL")
      Future(Nil)
    }
  }

  private def doIfSwitchedOn(action: => Future[Seq[T]]): Future[Seq[T]] = {
    if (switch.isSwitchedOn) {
      action
    } else {
      log.warn(s"Feed for $adTypeName switched off")
      Future(Nil)
    }
  }
}

trait JsonAdsApi[T <: Ad] extends AdsApi[JsValue, T] {

  final def transform(body: String): JsValue = Json.parse(body)

  def parse(json: JsValue): Seq[T]
}

trait XmlAdsApi[T <: Ad] extends AdsApi[Elem, T] {

  protected def cleanResponseBody(body: String): String = body

  final def transform(body: String): Elem = XML.loadString(cleanResponseBody(body))

  def parse(xml: Elem): Seq[T]
}
