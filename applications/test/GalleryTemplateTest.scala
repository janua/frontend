package test

import org.scalatest.{Matchers,FlatSpec}
import scala.collection.JavaConversions._

class GalleryTemplateTest extends FlatSpec with Matchers {

  it should "render gallery headline" in HtmlUnit("/news/gallery/2012/may/02/picture-desk-live-kabul-burma") {
    browser =>
      browser.$("h1").first.getText should be("Picture desk live: the day's best news images")
  }

  it should "render gallery story package links" in HtmlUnit("/music/gallery/2012/jun/23/simon-bolivar-orchestra-dudamel-southbank-centre") { browser =>
    val linkUrls = browser.$("a").getAttributes("href")

    linkUrls should contain(HtmlUnit.withHost("/music/2010/sep/16/gustavo-dudamel-simon-bolivar-orchestra"))
  }

  it should "render caption and navigation on first image page" in HtmlUnit("/news/gallery/2012/may/02/picture-desk-live-kabul-burma") { browser =>
    import browser._
    $("p.caption").getTexts.firstNonEmpty.get should include("A TV grab from state-owned French television station France 2 showing the debate between Francois Hollande and Nicolas Sarkozy for the 2012 French presidential election campaign")

    $("p.gallery-nav a.gallery-prev").length should be(0)

    $("p.gallery-nav a.gallery-next").getTexts.toList should be(List("Next"))
    $("p.gallery-nav a.gallery-next").getAttributes("href").toList should be(List(HtmlUnit.withHost("/news/gallery/2012/may/02/picture-desk-live-kabul-burma?index=2")))
  }

  it should "render caption and navigation on second image page" in HtmlUnit("/news/gallery/2012/may/02/picture-desk-live-kabul-burma?index=2") { browser =>
    import browser._
    $("p.caption").getTexts.firstNonEmpty.get should include("Socialist Party supporters watch live TV debate as their presidential candiate Francois Hollande takes on current president Nicolas Sarkozy")

    $("p.gallery-nav a.gallery-prev").getTexts.toList should be(List("Previous"))
    $("p.gallery-nav a.gallery-prev").getAttributes("href").toList should be(List(HtmlUnit.withHost("/news/gallery/2012/may/02/picture-desk-live-kabul-burma?index=1")))

    $("p.gallery-nav a.gallery-next").getTexts.toList should be(List("Next"))
    $("p.gallery-nav a.gallery-next").getAttributes("href").toList should be(List(HtmlUnit.withHost("/news/gallery/2012/may/02/picture-desk-live-kabul-burma?index=3")))
  }

  it should "render caption and navigation on last image page" in HtmlUnit("/news/gallery/2012/may/02/picture-desk-live-kabul-burma?index=22") { browser =>
    import browser._
    $("p.caption").getTexts.toList.last should include("This little scout has been taking part in a parade marking International Workers' Day in Nigeria's commercial capital, Lagos")

    $("p.gallery-nav a.gallery-prev").getTexts.toList should be(List("Previous"))
    $("p.gallery-nav a.gallery-prev").getAttributes("href").toList should be(List(HtmlUnit.withHost("/news/gallery/2012/may/02/picture-desk-live-kabul-burma?index=21")))

    $("p.gallery-nav a.gallery-next").getTexts.toList should be(List("Next"))
    $("p.gallery-nav a.gallery-next").getAttributes("href").toList should be(List(HtmlUnit.withHost("/news/gallery/2012/may/02/picture-desk-live-kabul-burma?index=1")))
  }
   
  it should "show the twitter card meta-data" in HtmlUnit("/music/gallery/2012/jun/23/simon-bolivar-orchestra-dudamel-southbank-centre") { browser =>
    import browser._
    $("meta[property='twitter:card']").getAttributes("content").head should be ("gallery")
    $("meta[property='twitter:title']").getAttributes("content").head should be ("Southbank Centre's Sounds Venezuela festival - in pictures")
    $("meta[property='twitter:image3:src']").getAttributes("content").head should endWith ("/Bassoons-in-the-Symphony--003.jpg")
  }

  it should "include the index parameter in direct links" in HtmlUnit("/music/gallery/2012/jun/23/simon-bolivar-orchestra-dudamel-southbank-centre?index=2") { browser =>
    browser.findFirst("link[rel='canonical']").getAttribute("href") should endWith("/music/gallery/2012/jun/23/simon-bolivar-orchestra-dudamel-southbank-centre?index=2")
  }
}
