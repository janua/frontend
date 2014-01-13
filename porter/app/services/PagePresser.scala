package services

import model.{Trail, Collection}
import common.editions.Uk
import scala.concurrent.Future
import common.ExecutionContexts
import play.api.libs.json._
import model.Config

trait PagePresser extends ExecutionContexts {

  def pressPage(id: String): Future[List[(Config, Collection)]] = {
    val collectionIds: List[Config] = PorterConfigAgent.getConfigForId(id).getOrElse(Nil)

    val collections = collectionIds.map(config => PorterCollectionParser.getCollection(config.id, config, Uk, isWarmedUp=true).map((config, _)))
    Future.sequence(collections)
  }

  def generateJson(json: String) = {
    val id = (Json.parse(json) \ "Message").as[String]
    val x = pressPage(id).map(_.map{case (config, collection) =>
      Json.obj(
        ("id", id),
        ("collections", Json.arr(Json.obj(config.id -> generateJsonObject(config, collection))))
      )
    })
    x.map(_.foldLeft(Json.obj()){case (fj, j) => j ++ fj}) //Flatten the json using JsObject.++
  }

  def generateJsonObject(config: Config, collection: Collection): JsValue = {
    Json.obj(
      ("apiQuery", config.contentApiQuery),
      ("displayName", config.displayName),
      ("tone", config.collectionTone),
      ("curated", collection.curated.map(createResultJson)),
      ("editorsPicks", collection.editorsPicks.map(createResultJson)),
      ("results", collection.results.map(createResultJson))
      //TODO: lastModified, modifiedBy
    )
  }

  def createResultJson(trail: Trail): JsValue = {
    Json.obj(
      ("webTitle", trail.headline),
      ("webPublicationDate", trail.webPublicationDate),
      ("sectionName", trail.sectionName),
      ("sectionId", trail.section),
      ("id", trail.url),
      ("webUrl", trail.webUrl),
      ("meta", Json.obj()),
      ("trailText", trail.trailText),
      ("linkText", trail.linkText)
    )
  }

}

object PagePresser extends PagePresser
