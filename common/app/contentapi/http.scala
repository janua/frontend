package contentapi

import com.gu.openplatform.contentapi.connection.{HttpResponse, Http}
import scala.concurrent.Future
import conf.Configuration
import common.{ContentApiMetrics, ExecutionContexts}
import java.util.concurrent.TimeoutException
import play.api.libs.ws.WS

trait WsHttp extends Http[Future] with ExecutionContexts {

  import System.currentTimeMillis
  import ContentApiMetrics._
  import Configuration.host
  import java.net.URLEncoder.encode

  override def GET(url: String, headers: Iterable[(String, String)]) = {
    val urlWithHost = url + s"&FRANCIS=true&host-name=${encode(host.name, "UTF-8")}"

    println(urlWithHost)

    val response = WS.url(urlWithHost).withHeaders(headers.toSeq: _*).withTimeout(2000).get()
    val start = currentTimeMillis

    // record metrics
    response.onSuccess {
      case _ => {
        println("Success: %s: %s".format(currentTimeMillis - start, urlWithHost))
        HttpTimingMetric.recordTimeSpent(currentTimeMillis - start)
      }
    }
    response.onFailure {
      case e: Throwable if isTimeout(e) => {
        println("Failed: %s: %s".format(currentTimeMillis - start, urlWithHost))
        HttpTimeoutCountMetric.increment
      }
    }
    response
  }.map {
    r => HttpResponse(r.body, r.status, r.statusText)
  }


  private def isTimeout(e: Throwable): Boolean = e match {
    case t: TimeoutException => true
    case _  => false
  }
}


// allows us to inject a test Http
trait DelegateHttp extends Http[Future] with ExecutionContexts {

  private var _http: Http[Future] = new WsHttp {}

  def http = _http
  def http_=(delegateHttp: Http[Future]) { _http = delegateHttp }

  override def GET(url: String, headers: scala.Iterable[scala.Tuple2[String, String]]) = _http.GET(url, headers)
}

