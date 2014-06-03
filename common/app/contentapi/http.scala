package contentapi

import com.gu.openplatform.contentapi.connection.{HttpResponse, Http}
import scala.concurrent.Future
import conf.Configuration
import conf.Configuration.contentApi.previewAuth
import common.{SimpleCountMetric, FrontendTimingMetric, ExecutionContexts}
import java.util.concurrent.TimeoutException
import play.api.libs.ws.{Response, WS}
import com.gu.management.TimingMetric
import common.ContentApiMetrics.ContentApi404Metric
import java.net.InetAddress
import scala.util.Try
import com.ning.http.client.Realm.AuthScheme
import java.lang.System._
import com.gu.openplatform.contentapi.connection.HttpResponse
import common.SimpleCountMetric
import performance.MemcachedWS._
import play.api.libs.ws.WS.WSRequestHolder
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import performance.{CacheMiss, CacheStale, CacheHit}

trait MyFirstWsHttp extends Http[Future] with ExecutionContexts {
  val httpTimingMetric: TimingMetric
  val httpTimeoutMetric: SimpleCountMetric

  def makeRequest(url: String, request: WSRequestHolder): Future[Response]

  override def GET(url: String, headers: Iterable[(String, String)]) = {

    //append with a & as there are always params in there already
    val urlWithDebugInfo = s"$url&${RequestDebugInfo.debugParams}"

    val contentApiTimeout = Configuration.contentApi.timeout

    val start = currentTimeMillis

    val baseRequest = WS.url(urlWithDebugInfo)
    val request = previewAuth.fold(baseRequest)(auth => baseRequest.withAuth(auth.user, auth.password, AuthScheme.BASIC))
    val response = makeRequest(url, request.withHeaders(headers.toSeq: _*).withRequestTimeout(contentApiTimeout))

    // record metrics
    response.onSuccess {
      case r if r.status == 404 => ContentApi404Metric.increment()
      case r if r.status == 200 => httpTimingMetric.recordTimeSpent(currentTimeMillis - start)
    }

    response.onFailure {
      case e: TimeoutException => httpTimeoutMetric.increment()
    }

    response map { r => HttpResponse(r.body, r.status, r.statusText) }
  }
}

class WsHttp(val httpTimingMetric: TimingMetric, val httpTimeoutMetric: SimpleCountMetric) extends MyFirstWsHttp {
  override def makeRequest(url: String, request: WSRequestHolder): Future[Response] = request.get()
}

class WsHttpMemcached(val httpTimingMetric: TimingMetric, val httpTimeoutMetric: SimpleCountMetric) extends MyFirstWsHttp {
  private val cachePrefix: String = "content-api:"
  private val staleAfter: Duration = 1.minutes
  private val doNotServeAfter: Duration = 1.hour
  override def makeRequest(url: String, request: WSRequestHolder): Future[Response] =
    request.withMemcachingOnKey((cachePrefix + url).take(200), staleAfter, doNotServeAfter).get().map {
      case CacheHit(response) => response
      case CacheStale(response) => response
      case CacheMiss(response) => response
    }
}

// allows us to inject a test Http
trait DelegateHttp extends Http[Future] with ExecutionContexts {

  val httpTimingMetric: FrontendTimingMetric
  val httpTimeoutMetric: SimpleCountMetric

  private var _http: Http[Future] = new WsHttp(httpTimingMetric, httpTimeoutMetric)

  def http = _http
  def http_=(delegateHttp: Http[Future]) { _http = delegateHttp }

  override def GET(url: String, headers: scala.Iterable[scala.Tuple2[String, String]]) = _http.GET(url, headers)
}

private object RequestDebugInfo {

  import java.net.URLEncoder.encode

  private lazy val host: String = Try(InetAddress.getLocalHost.getCanonicalHostName).getOrElse("unable-to-determine-host")
  private lazy val stage: String = Configuration.environment.stage
  private lazy val project: String = Configuration.environment.projectName

  lazy val debugParams = Seq(
    s"ngw-host=${encode(host, "UTF-8")}",
    s"ngw-stage=${encode(stage, "UTF-8")}",
    s"ngw-project=${encode(project, "UTF-8")}"
  ).mkString("&")

}