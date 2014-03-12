package test

import play.api.test._
import play.api.test.Helpers._
import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import common.ExecutionContexts
import controllers.FaciaController
import conf.Switches

class FaciaControllerTest extends FlatSpec with Matchers with BeforeAndAfter with ExecutionContexts {

  val articleUrl = "/environment/2012/feb/22/capitalise-low-carbon-future"
  val callbackName = "aFunction"

  val responsiveRequest = FakeRequest().withHeaders("host" -> "www.theguardian.com")

  before {
    Switches.PressedFacia.switchOn()
  }

  "FaciaController" should "200 when content type is front" in Fake {
    val result = FaciaController.renderEditionFront("uk")(TestRequest())
    status(result) should be(200)
  }

  it should "redirect base page to edition page if on www.theguardian.com" in Fake {

    val result = FaciaController.editionRedirect("")(responsiveRequest.withHeaders("X-GU-Edition" -> "US"))
    status(result) should be(303)
    header("Location", result) should be (Some("/us"))

    val result2 = FaciaController.editionRedirect("culture")(responsiveRequest.withHeaders("X-GU-Edition" -> "AU"))
    status(result2) should be(303)
    header("Location", result2) should be (Some("/au/culture"))

  }

  it should "understand the editionalised network front" in Fake {
    val result2 = FaciaController.renderEditionFront("uk")(TestRequest())
    status(result2) should be(200)
  }

  it should "understand editionalised section fronts" in Fake {
    val result2 = FaciaController.renderEditionSectionFront("uk/culture")(TestRequest())
    status(result2) should be(200)
  }

  it should "return JSONP when callback is supplied to front" in Fake {
    val fakeRequest = FakeRequest(GET, s"?callback=$callbackName")
      .withHeaders("host" -> "localhost:9000")

    val result = FaciaController.renderEditionFront("uk")(fakeRequest)
    status(result) should be(200)
    contentType(result).get should be("application/javascript")
    contentAsString(result) should startWith(s"""$callbackName({\"html\"""")
  }

  it should "return JSON when .json format is supplied to front" in Fake {
    val fakeRequest = FakeRequest("GET", ".json")
      .withHeaders("Host" -> "localhost:9000")
      .withHeaders("Origin" -> "http://www.theorigin.com")

    val result = FaciaController.renderEditionFrontJson("uk")(fakeRequest)
    status(result) should be(200)
    contentType(result).get should be("application/json")
    contentAsString(result) should startWith("{\"html\"")
  }

  it should "200 when hitting the front" in Fake {
    val result = FaciaController.renderEditionFront("/")(TestRequest())
    status(result) should be(200)
  }
}
