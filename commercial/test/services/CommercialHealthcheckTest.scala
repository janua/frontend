package services

import org.scalatest.{Matchers, FlatSpec}
import play.api.libs.ws.WS
import test._

import scala.concurrent.duration._
import scala.concurrent.Await

class CommercialHealthcheckTest extends FlatSpec with Matchers {

  import play.api.Play.current

  "Healthchecks" should "pass" in HtmlUnit("/"){ browser =>

    Await.result(WS.url(s"http://localhost:${HtmlUnit.port}/_healthcheck").get(), 10.seconds).status should be (200)
  }
}
