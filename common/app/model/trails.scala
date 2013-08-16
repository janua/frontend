package model

import conf.{Configuration, ContentApi}
import common._
import contentapi.QueryDefaults
import org.joda.time.DateTime
import play.api.libs.ws.{ WS, Response }
import play.api.libs.json.Json._
import play.api.libs.json.JsObject
import scala.concurrent.Future
import tools.QueryParams
import views.support.Style


trait Trail extends Images with Tags {
  def webPublicationDate: DateTime
  def linkText: String
  def headline: String
  def url: String
  def trailText: Option[String]
  def section: String //sectionId
  def sectionName: String
  def thumbnail: Option[String] = None
  def thumbnailPath: Option[String] = None
  def isLive: Boolean
  def discussionId: Option[String] = None
  def leadingParagraphs: List[org.jsoup.nodes.Element] = Nil
  def byline: Option[String] = None
}

case class Trailblock(description: TrailblockDescription, trails: Seq[Trail])

trait TrailblockDescription extends ExecutionContexts {
  val id: String
  val name: String
  val numItemsVisible: Int
  val style: Option[Style]
  val section: String
  val showMore: Boolean
  val isConfigured: Boolean

  def query(): Future[Seq[Trail]]
}

class ItemTrailblockDescription(
    val id: String, val name: String,
    val numItemsVisible: Int,
    val style: Option[Style],
    val showMore: Boolean,
    val edition: Edition,
    val isConfigured: Boolean) extends TrailblockDescription with QueryDefaults
  {
    lazy val section = id.split("/").headOption.filterNot(_ == "").getOrElse("news")

  def query() = EditorsPicsOrLeadContentAndLatest(
    ContentApi.item(id, edition)
      .showEditorsPicks(true)
      .pageSize(20)
      .response
  )
}

object ItemTrailblockDescription {
  def apply(id: String, name: String, numItemsVisible: Int, style: Option[Style] = None, showMore: Boolean = false, isConfigured: Boolean = false)(implicit edition: Edition) =
    new ItemTrailblockDescription(id, name, numItemsVisible, style, showMore, edition, isConfigured)
}

private case class CustomQueryTrailblockDescription(
            id: String,
            name: String,
            numItemsVisible: Int,
            style: Option[Style],
            customQuery: () => Future[Seq[Trail]],
            isConfigured: Boolean)
    extends TrailblockDescription {

  // show more will not (currently) work with custom queries
  val showMore = false

  lazy val section = id.split("/").headOption.filterNot(_ == "").getOrElse("news")

  def query() = customQuery()
}

object CustomTrailblockDescription {
  def apply(id: String,
            name: String,
            numItemsVisible: Int,
            style: Option[Style] = None,
            showMore: Boolean = false,
            isConfigured: Boolean = false)
           (query: => Future[Seq[Trail]]): TrailblockDescription =
    CustomQueryTrailblockDescription(id, name, numItemsVisible, style, () => query, isConfigured)
}


trait ConfiguredTrailblockDescription extends TrailblockDescription {
  def query() = scala.concurrent.future {
    Nil
  }

  def configuredQuery(): Future[Option[TrailblockDescription]]
}

trait ResponseParsing extends ExecutionContexts with Logging {

  def parseResponse(response: Future[Response], description: TrailblockDescription, edition: Edition): Future[Option[TrailblockDescription]] = {
    response.flatMap { r =>
      r.status match {
        case 200 =>
          val bodyJson = parse(r.body)
          val numItems = (bodyJson \ "max").asOpt[Int] getOrElse description.numItemsVisible
          // extract the articles

          val articles: Seq[String] = (bodyJson \ "live").as[Seq[JsObject]] map { trail =>
            (trail \ "id").as[String]
          }

          val idSearch = {
            val response = ContentApi.search(edition).ids(articles.mkString(",")).pageSize(List(articles.size, 50).min).response
            val results = response map {r => r.results map{new Content(_)} }
            val sorted = results map { _.sortBy(t => articles.indexWhere(_ == t.id))}
            sorted fallbackTo Future(Nil)
          }

          val contentApiQuery = (parse(r.body) \ "contentApiQuery").asOpt[String] map { query =>
            val queryParams: Map[String, String] = QueryParams.get(query).mapValues{_.mkString("")}
            val queryParamsWithEdition = queryParams + ("edition" -> queryParams.getOrElse("edition", Edition.defaultEdition.id))
            val search = ContentApi.search(edition)
            val queryParamsAsStringParams = queryParamsWithEdition map {case (k, v) => k -> search.StringParameter(k, Some(v))}
            val newSearch = search.updated(search.parameterHolder ++ queryParamsAsStringParams)

            newSearch.response map { r =>
              r.results.map(new Content(_))
            }
          } getOrElse Future(Nil)

          val results = for {
            idSearchResults <- idSearch
            contentApiResults <- contentApiQuery
          } yield idSearchResults ++ contentApiResults

          results map {
            case l: List[Content] => Some(CustomTrailblockDescription(description.id, description.name, numItems, description.style, description.isConfigured) {
              results
            })
          } fallbackTo Future(None)

        case _ =>
          log.warn(s"Could not load running order: ${r.status} ${r.statusText}")
          // NOTE: better way of handling fallback
          Future(Some(ItemTrailblockDescription(description.id, description.name, description.numItemsVisible, description.style, description.showMore, description.isConfigured)(edition)))
      }
    }
  }

}

class RunningOrderTrailblockDescription(
  val id: String,
  val blockId: String,
  val name: String,
  val numItemsVisible: Int,
  val style: Option[Style],
  val showMore: Boolean,
  val edition: Edition,
  val isConfigured: Boolean
) extends ConfiguredTrailblockDescription with ResponseParsing with Logging {

  lazy val section = id.split("/").headOption.filterNot(_ == "").getOrElse("news")

  def configuredQuery() = {
    // get the running order from the apiwith
    val configUrl = s"${Configuration.frontend.store}/${S3FrontsApi.location}/collection/$blockId/collection.json"
    log.info(s"loading running order configuration from: $configUrl")
    val response: Future[Response] = WS.url(s"$configUrl").withTimeout(2000).get()
    parseResponse(response, this, edition)
  }
}

object RunningOrderTrailblockDescription {
  def apply(id: String, blockId: String, name: String, numItemsVisible: Int, style: Option[Style] = None, showMore: Boolean = false, isConfigured: Boolean = false)(implicit edition: Edition) =
    new RunningOrderTrailblockDescription(id, blockId, name, numItemsVisible, style, showMore, edition, isConfigured)
}

class ConfiguredRunningOrderTrailblockDescription(cid: String)(implicit edition: Edition) extends ConfiguredTrailblockDescription with ResponseParsing
  with Logging {

  lazy val numItemsVisible = 10
  val showMore = false
  val section = "sport"
  val name = "sport"
  val isConfigured = true
  val id = "sport"
  val style = None

  def configuredQuery: Future[Option[TrailblockDescription]] = getDescription(cid) flatMap {description =>
    val configUrl = s"${Configuration.frontend.store}/${S3FrontsApi.location}/collection/${description.blockId}/collection.json"
    log.info(s"loading running order configuration from: $configUrl")
    val response: Future[Response] = WS.url(s"$configUrl").withTimeout(2000).get()
    parseResponse(response, description, edition)
  }

  def getDescription(id: String): Future[RunningOrderTrailblockDescription] = getConfig(id) map {config =>
      RunningOrderTrailblockDescription(
        id,
        config.get("blockid") getOrElse "",
        config.get("name") getOrElse "",
        config.get("numItemsVisible") map (_.toInt) getOrElse 20
      )
    }

  def getConfig(id: String): Future[Map[String, String]] = {
    val configUrl = s"${Configuration.frontend.store}/${S3FrontsApi.location}/config/$id/config.json"
    WS.url(configUrl).withTimeout(2000).get map { r =>
      val json = parse(r.body)
      json.asOpt[Map[String, String]] getOrElse Map.empty
    }
  }
}

object ConfiguredRunningOrderTrailblockDescription {
  def apply(id: String)(implicit edition: Edition): ConfiguredRunningOrderTrailblockDescription = new ConfiguredRunningOrderTrailblockDescription(id)
}
