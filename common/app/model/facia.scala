package model

import org.joda.time.DateTime

trait ConfigFields {
  val contentApiQuery: Option[String]
  val displayName: Option[String]
  val href: Option[String]
  val groups: Option[Seq[String]]
  val `type`: Option[String]
  val uneditable: Option[Boolean]
}

case class ConfigForJson(contentApiQuery: Option[String],
                        displayName: Option[String],
                        href: Option[String],
                        groups: Option[Seq[String]],
                        `type`: Option[String],
                        uneditable: Option[Boolean]
                         ) extends ConfigFields

case class Config(
                   id: String,
                   contentApiQuery: Option[String] = None,
                   displayName: Option[String]     = None,
                   href: Option[String]            = None,
                   groups: Option[Seq[String]]     = None,
                   `type`: Option[String]          = None,
                   uneditable: Option[Boolean]     = None)
  extends ConfigFields {
  def collectionType = `type`
}

object Config {
  def fromId(id: String): Config = Config(id, None, None, None, None, None, None)
}

case class Collection(curated: Seq[Content],
                      editorsPicks: Seq[Content],
                      mostViewed: Seq[Content],
                      results: Seq[Content],
                      displayName: Option[String],
                      href: Option[String],
                      lastUpdated: Option[String],
                      updatedBy: Option[String],
                      updatedEmail: Option[String]) extends implicits.Collections {

  lazy val items: Seq[Content] = (curated ++ editorsPicks ++ mostViewed ++ results).distinctBy(_.url)
}

object Collection {
  def apply(curated: Seq[Content]): Collection = Collection(curated, Nil, Nil, Nil, None, None, Option(DateTime.now.toString), None, None)
  def apply(curated: Seq[Content], displayName: Option[String]): Collection = Collection(curated, Nil, Nil, Nil, displayName, None, Option(DateTime.now.toString), None, None)
}

case class FaciaPage(
                   id: String,
                   collections: List[(Config, Collection)])
