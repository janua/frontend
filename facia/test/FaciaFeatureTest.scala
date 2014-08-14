package test

import org.scalatest.{BeforeAndAfter, Matchers, GivenWhenThen, FeatureSpec}
import collection.JavaConversions._
import conf.Switches

class FaciaFeatureTest extends FeatureSpec with GivenWhenThen with Matchers with BeforeAndAfter {

  feature("Facia") {

    // Scenarios

    ignore("Display the news container") {

      Given("I am on the UK network front")
      HtmlUnit("/uk") { browser =>

        browser.webDriver.getPageSource.length should be > 0
        Then("I should see the news container")
        browser.$("section[data-id='uk/news/regular-stories']").length should be(1)
      }
    }

    ignore("Render a tag page if it is not in config.json") {

      Given("I go to a tag page")
      HtmlUnit("/sport/cycling") { browser =>

        Then("I should see only the tag and most popular")
        browser.$("section[data-id='sport/cycling/news/regular-stories']").length should be(1)
      }
    }

  }
}
