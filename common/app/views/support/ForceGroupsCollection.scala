package views.support

import model.{CollectionItems, Collection, Content}
import play.api.libs.json.JsString

trait FirstTwoBigItems extends CollectionItems {
  override lazy val items: Seq[Content] = {
    super.items match {
      case x :: y :: tail =>
        Content(x.apiContent.copy(metaData = x.apiContent.metaData.map { meta =>
          meta.copy(group = Option("1"), imageAdjust = Option("boost"))
        })) ::
        Content(y.apiContent.copy(metaData = y.apiContent.metaData.map { meta =>
            meta.copy(group = Option("1"))})) ::
        tail
      case x => x.map{ content =>
        Content(content.apiContent.copy(metaData = content.apiContent.metaData.map { meta =>
          meta.copy(group = Option("1"))}))
      }
    }
  }
}

object ForceGroupsCollection {
  def firstTwoBig(c: Collection): Collection = {
    new Collection(c.curated, c.editorsPicks, c.mostViewed, c.results, c.displayName, c.href,
      c.lastUpdated, c.updatedBy, c.updatedEmail) with FirstTwoBigItems
  }
}