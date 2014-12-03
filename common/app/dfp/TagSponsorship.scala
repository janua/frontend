package dfp

import common.Logging
import model.Tag
import play.api.libs.json._


object Sponsorship {

  implicit val sponsorshipWrites = new Writes[Sponsorship] {
    def writes(sponsorship: Sponsorship): JsValue = {
      Json.obj(
        "tags" -> sponsorship.tags,
        "sections" -> sponsorship.sections,
        "sponsor" -> sponsorship.sponsor,
        "countries" -> sponsorship.countries,
        "lineItemId" -> sponsorship.lineItemId
      )
    }
  }

  implicit val jsonReads = Json.reads[Sponsorship]

  val ANY_SECTION = ""
}

case class Sponsorship(tags: Seq[String],
                       sections: Seq[String],
                       sponsor: Option[String],
                       countries: Seq[String],
                       lineItemId: Long) {
  def hasTag(tagId: String): Boolean = tagId.split('/').lastOption.exists { endPart =>
    tags contains endPart
  }
}


object InlineMerchandisingTagSet {
  implicit val jsonReads = Json.reads[InlineMerchandisingTagSet]
}

case class InlineMerchandisingTagSet(keywords: Set[String] = Set.empty, series: Set[String] = Set.empty, contributors: Set[String] = Set.empty) {

  private def hasTagId(tags: Set[String], tagId: String): Boolean = tagId.split('/').lastOption exists { endPart =>
    tags contains endPart
  }

  def hasTag(tag: Tag): Boolean = tag.tagType match {
    case "keyword" => hasTagId(keywords, tag.id)
    case "series" => hasTagId(series, tag.id)
    case "contributor" => hasTagId(contributors, tag.id)
    case _ => false
  }

  def nonEmpty: Boolean = keywords.nonEmpty || series.nonEmpty || contributors.nonEmpty
}


object SponsorshipReport {

  implicit val sponsorshipReportWrites = new Writes[SponsorshipReport] {
    def writes(sponsorshipReport: SponsorshipReport): JsValue = {
      Json.obj(
        "updatedTimeStamp" -> sponsorshipReport.updatedTimeStamp,
        "sponsorships" -> sponsorshipReport.sponsorships
      )
    }
  }

  implicit val jsonReads = Json.reads[SponsorshipReport]

}

case class SponsorshipReport(updatedTimeStamp: String, sponsorships: Seq[Sponsorship])


object SponsorshipReportParser extends Logging {

  def apply(jsonString: String) = {
    val result: JsResult[SponsorshipReport] = Json.parse(jsonString).validate[SponsorshipReport]
    result match {
      case s: JsSuccess[SponsorshipReport] => Some(s.get)
      case e: JsError => log.error("Errors: " + JsError.toFlatJson(e).toString()); None
    }
  }
}


object InlineMerchandisingTargetedTagsReport {
  implicit val jsonReads = Json.reads[InlineMerchandisingTargetedTagsReport]
}

case class InlineMerchandisingTargetedTagsReport(updatedTimeStamp: String, targetedTags: InlineMerchandisingTagSet)


object InlineMerchandisingTargetedTagsReportParser extends Logging {
  def apply(jsonString: String): Option[InlineMerchandisingTargetedTagsReport] = {
    val json = Json.parse(jsonString)
    json.validate[InlineMerchandisingTargetedTagsReport] match {
      case s: JsSuccess[InlineMerchandisingTargetedTagsReport] => Some(s.get)
      case e: JsError => log.error("Errors: " + JsError.toFlatJson(e).toString()); None
    }
  }
}
