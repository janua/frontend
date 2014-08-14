package com.gu.integration.test.util

import com.gu.automation.support.{Config, TestLogging}
import com.gu.integration.test.pages.common.ParentPage
import org.openqa.selenium.WebDriver

/**
 * This class is for loading and initializing pages and page objects. Example usage: <code></code>
 */
object PageLoader extends TestLogging {

  val frontsBaseUrl = Config().getTestBaseUrl()
  val TestAttributeName = "data-test-id"

  /**
   * This method goes to a particular URL and then initializes the provided page object and returns it. To properly use it
   * provide a lazy val page object, otherwise Selenium initialized fields might occur before on the actual page.
   */
  def goTo[Page <: ParentPage](pageObject: => Page, absoluteUrl: String, useBetaRedirect: Boolean = true)
                              (implicit driver: WebDriver): Page = {
    driver.get(forceBetaSite(useBetaRedirect, turnOfPopups(absoluteUrl)))
    pageObject
  }

  def fromRelativeUrl(relativeUrl: String): String = {
    frontsBaseUrl + relativeUrl
  }

  def turnOfPopups(url: String): String = {
    url + "?test=true"
  }

  /**
   * This will append the request parameters needed to switch to beta site. However, for some reason, this does not work on
   * localhost so had to make a check
   */
  def forceBetaSite(useBetaRedirect: Boolean, url: String): String = {
    if (frontsBaseUrl.contains("localhost") || !useBetaRedirect) {
      url
    } else {
      frontsBaseUrl + "/preference/platform/mobile?page=" + url + "&view=mobile"
    }
  }
}