package test

import play.api.test.Helpers._
import org.scalatest.{Matchers, FlatSpec}

class MediaControllerTest extends FlatSpec with Matchers {

  val videoUrl = "uk/video/2012/jun/26/queen-enniskillen-northern-ireland-video"
  val callbackName = "aFunction"

  "Media Controller" should "200 when content type is video" in Fake {
    val result = controllers.MediaController.render(videoUrl)(TestRequest(videoUrl))
    status(result) should be(200)
  }

  it should "return JSONP when callback is supplied" in Fake {
    val fakeRequest = TestRequest(s"${videoUrl}?callback=$callbackName")
      .withHeaders("host" -> "localhost:9000")

    val result = controllers.MediaController.render(videoUrl)(fakeRequest)
    status(result) should be(200)
    header("Content-Type", result).get should be("application/javascript; charset=utf-8")
    contentAsString(result) should startWith(s"""${callbackName}({\"config\"""")
  }

  it should "return JSON when .json format is supplied" in Fake {
    val fakeRequest = TestRequest(s"${videoUrl}.json")
      .withHeaders("host" -> "localhost:9000")
      .withHeaders("Origin" -> "http://www.theorigin.com")

    val result = controllers.MediaController.render(videoUrl)(fakeRequest)
    status(result) should be(200)
    header("Content-Type", result).get should be("application/json; charset=utf-8")
    contentAsString(result) should startWith("{\"config\"")
  }

  it should "internal redirect when content type is not video" in Fake {
    val result = controllers.MediaController.render("uk/2012/jun/27/queen-martin-mcguinness-shake-hands")(TestRequest("/uk/2012/jun/27/queen-martin-mcguinness-shake-hands"))
    status(result) should be(200)
    header("X-Accel-Redirect", result).get should be("/type/article/uk/2012/jun/27/queen-martin-mcguinness-shake-hands")
  }

  it should "display an expired message for expired content" in Fake {
    val result = controllers.MediaController.render("world/video/2008/dec/11/guantanamo-bay")(TestRequest("/world/video/2008/dec/11/guantanamo-bay"))
    status(result) should be(410)
    contentAsString(result) should include("Sorry - this page has been removed.")
  }

}
