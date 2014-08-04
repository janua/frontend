package test

import conf.{LiveContentApi, Configuration}
import java.net.URLEncoder
import play.api.test._
import play.api.test.Helpers._
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import java.net.{ HttpURLConnection, URL }
import java.io.File
import com.gu.openplatform.contentapi.connection.Http
import recorder.ContentApiHttpRecorder
import play.api.GlobalSettings
import scala.concurrent.Future
import org.apache.commons.codec.digest.DigestUtils
import com.gargoylesoftware.htmlunit.BrowserVersion

trait TestSettings {
  def globalSettingsOverride: Option[GlobalSettings] = None
  def testPlugins: Seq[String] = Nil
  def disabledPlugins: Seq[String] = Seq(
    "conf.SwitchBoardPlugin"
  )

  val recorder = new ContentApiHttpRecorder {
    override lazy val baseDir = new File(System.getProperty("user.dir"), "data/database")
  }

  private def verify(property: String, hash: String, message: String) {
    if (DigestUtils.sha256Hex(property) != hash) {

      // the println makes it easier to spot what is wrong in tests
      println()
      println(s"----------- $message -----------")
      println()

      throw new RuntimeException(message)
    }
  }

  private def toRecorderHttp(http: Http[Future]) = new Http[Future] {

    val originalHttp = http

    verify(
      Configuration.contentApi.contentApiLiveHost,
      "37f3bee67d016a9fec7959aa5bc5e53fa7fdc688f987c0dea6fa0f6af6979079",
      "YOU ARE NOT USING THE CORRECT ELASTIC SEARCH LIVE CONTENT API HOST"
    )

    Configuration.contentApi.key.map { k =>
        verify(
          k,
          "a4eb3e728596c7d6ba43e3885c80afcb16bc24d22fc0215409392bac242bed96",
          "YOU ARE NOT USING THE CORRECT CONTENT API KEY"
        )
    }

    override def GET(url: String, headers: scala.Iterable[scala.Tuple2[java.lang.String, java.lang.String]]) = {
      recorder.load(url, headers.toMap) {
        originalHttp.GET(url, headers)
      }
    }
  }

  LiveContentApi.http = toRecorderHttp(LiveContentApi.http)
}

/**
 * Executes a block of code in a running server, with a test HtmlUnit browser.
 */
class EditionalisedHtmlUnit(val port: String) extends TestSettings {

  // the default is I.E 7 which we do not support
  BrowserVersion.setDefault(BrowserVersion.CHROME)

  val host = s"http://localhost:${port}"

  def apply[T](path: String)(block: TestBrowser => T): T = UK(path)(block)

  def UK[T](path: String)(block: TestBrowser => T): T = goTo(path, host)(block)

  def US[T](path: String)(block: TestBrowser => T): T = {
    val editionPath = if (path.contains("?")) s"$path&_edition=US" else s"$path?_edition=US"
    goTo(editionPath, host)(block)
  }

  protected def goTo[T](path: String, host: String)(block: TestBrowser => T): T = {

    running(TestServer(port.toInt,
      FakeApplication(additionalPlugins = testPlugins, withoutPlugins = disabledPlugins,
                      withGlobal = globalSettingsOverride)), HTMLUNIT) { browser =>

      // http://stackoverflow.com/questions/7628243/intrincate-sites-using-htmlunit
      browser.webDriver.asInstanceOf[HtmlUnitDriver].setJavascriptEnabled(false)

      browser.goTo(host + path)
      block(browser)
    }
  }

  def withHost(path: String) = s"http://localhost:$port$path"

  def classicVersionLink(path: String) = s"http://localhost:$port/preference/platform/classic?page=${URLEncoder.encode(s"$path?view=classic", "UTF-8")}"
}

/**
 * Executes a block of code in a FakeApplication.
 */
trait FakeApplication extends TestSettings {

  def apply[T](block: => T): T = running(
    FakeApplication(
      withoutPlugins = disabledPlugins,
      withGlobal = globalSettingsOverride,
      additionalPlugins = testPlugins,
      additionalConfiguration = Map("application.secret" -> "test-secret")
    )
  ) { block }
}

object Fake extends FakeApplication

object TestRequest {
  // MOST of the time we do not care what path is set on the request - only need to override where we do
  def apply(path: String = "/does-not-matter"): FakeRequest[play.api.mvc.AnyContentAsEmpty.type] = {
    FakeRequest("GET", if (!path.startsWith("/")) s"/$path" else path)
  }
}