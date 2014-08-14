package indexes

import common.Maps._
import com.gu.openplatform.contentapi.model.Tag
import java.text.Normalizer
import model.{TagDefinition, TagIndexPage}

import play.api.libs.iteratee.{Enumeratee, Iteratee}
import scala.concurrent.ExecutionContext.Implicits.global

object TagPages {
  /** To be curated by Peter Martin */
  val ValidSections = Map(
    ("artanddesign", "Art and design"),
    ("better-business", "Better Business"),
    ("books", "Books"),
    ("business", "Business"),
    ("cardiff", "Cardiff"),
    ("cities", "Cities"),
    ("commentisfree", "Comment is free"),
    ("community", "Community"),
    ("crosswords", "Crosswords"),
    ("culture", "Culture"),
    ("culture-network", "Culture Network"),
    ("culture-professionals-network", "Culture professionals network"),
    ("edinburgh", "Edinburgh"),
    ("education", "Education"),
    ("enterprise-network", "Guardian Enterprise Network"),
    ("environment", "Environment"),
    ("extra", "Extra"),
    ("fashion", "Fashion"),
    ("film", "Film"),
    ("football", "Football"),
    ("global-development", "Global development"),
    ("global-development-professionals-network", "Global Development Professionals Network"),
    ("government-computing-network", "Guardian Government Computing"),
    ("guardian-professional", "Guardian Professional"),
    ("healthcare-network", "Healthcare Professionals Network"),
    ("help", "Help"),
    ("higher-education-network", "Higher Education Network"),
    ("housing-network", "Housing Network"),
    ("info", "Info"),
    ("katine", "Katine"),
    ("law", "Law"),
    ("leeds", "Leeds"),
    ("lifeandstyle", "Life and style"),
    ("local", "Local"),
    ("local-government-network", "Local Leaders Network"),
    ("media", "Media"),
    ("media-network", "Media Network"),
    ("money", "Money"),
    ("music", "Music"),
    ("news", "News"),
    ("politics", "Politics"),
    ("public-leaders-network", "Public Leaders Network"),
    ("science", "Science"),
    ("search", "Search"),
    ("small-business-network", "Guardian Small Business Network"),
    ("social-care-network", "Social Care Network"),
    ("social-enterprise-network", "Social Enterprise Network"),
    ("society", "Society"),
    ("society-professionals", "Society Professionals"),
    ("sport", "Sport"),
    ("stage", "Stage"),
    ("teacher-network", "Teacher Network"),
    ("technology", "Technology"),
    ("theguardian", "From the Guardian"),
    ("theobserver", "From the Observer"),
    ("travel", "Travel"),
    ("travel/offers", "Guardian holiday offers"),
    ("tv-and-radio", "Television & radio"),
    ("uk-news", "UK news"),
    ("voluntary-sector-network", "Voluntary Sector Network"),
    ("weather", "Weather"),
    ("women-in-leadership", "Women in Leadership"),
    ("world", "World news")
  )

  def asAscii(s: String) =
    Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")

  def alphaIndexKey(s: String) = {
    val firstChar = asAscii(s).toLowerCase.charAt(0)

    if (firstChar.isDigit) {
      "1-9"
    } else {
      firstChar.toString
    }
  }

  private def mappedByKey(key: Tag => String) =
    Iteratee.fold[Tag, Map[String, Set[Tag]]](Map.empty) { (acc, tag) =>
      insertWith(acc, key(tag), Set(tag))(_ union _)
    }

  def toPages(tagsByKey: Map[String, Set[Tag]])(titleFromKey: String => String) = tagsByKey.toSeq.sortBy(_._1) map { case (id, tagSet) =>
    TagIndexPage(
      id,
      titleFromKey(id),
      tagSet.toSeq.sortBy(tag => asAscii(tag.webTitle)).map(TagDefinition.fromContentApiTag)
    )
  }

  val invalidSectionsFilter = Enumeratee.filter[Tag](_.sectionId.exists(ValidSections.contains))

  val byWebTitle = mappedByKey(tag => alphaIndexKey(tag.webTitle))

  val bySection = invalidSectionsFilter &>> mappedByKey(_.sectionId.get)
}

