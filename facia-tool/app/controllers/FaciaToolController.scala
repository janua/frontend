package controllers

import frontsapi.model._
import frontsapi.model.UpdateList
import jobs.FrontPressJob
import play.api.mvc.{AnyContent, Action, Controller}
import play.api.libs.json._
import common.{FaciaToolMetrics, ExecutionContexts, Logging}
import conf.{Switches, Configuration}
import tools.FaciaApi
import services.{ConfigAgent, ContentApiWrite}
import play.api.libs.ws.Response
import scala.concurrent.Future
import conf.Switches.ContentApiPutSwitch
import services.S3FrontsApi
import model.{NoCache, Cached}


object FaciaToolController extends Controller with Logging with ExecutionContexts {
  implicit val collectionRead = Json.reads[Collection]
  implicit val frontRead = Json.reads[Front]
  implicit val configRead = Json.reads[Config]
  implicit val collectionWrite = Json.writes[Collection]
  implicit val frontWrite= Json.writes[Front]
  implicit val configWrite = Json.writes[Config]

  implicit val updateListRead = Json.reads[UpdateList]
  implicit val collectionMetaRead = Json.reads[CollectionMetaUpdate]
  implicit val trailWrite = Json.writes[Trail]
  implicit val blockWrite = Json.writes[Block]

  def collectionsEditor() = ExpiringAuthentication { request =>
    val identity = Identity(request).get
    Cached(60) { Ok(views.html.collections(Configuration.environment.stage, Option(identity))) }
  }

  def configEditor() = ExpiringAuthentication { request =>
    val identity = Identity(request).get
    Cached(60) { Ok(views.html.config(Configuration.environment.stage, Option(identity))) }
  }

  def listCollections = AjaxExpiringAuthentication { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache { Ok(Json.toJson(S3FrontsApi.listCollectionIds)) }
  }

  def getConfig = AjaxExpiringAuthentication.async { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    S3FrontsApi.getMasterConfig map { response =>
      NoCache {
        Ok(response.body).as("application/json")
      }
    }
  }

  def updateConfig(): Action[AnyContent] = AjaxExpiringAuthentication { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache {
      request.body.asJson flatMap(_.asOpt[Config]) map {
        case update: Config => {
          val identity = Identity(request).get
          UpdateActions.putMasterConfig(update, identity)
          Ok
        }
        case _ => NotFound
      } getOrElse NotFound
    }
  }

  def readBlock(id: String) = AjaxExpiringAuthentication.async { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    S3FrontsApi.getBlock(id) map { response =>
      NoCache {
        Ok(response.body).as("application/json")
      }
    }
  }

  def publishCollection(id: String) = AjaxExpiringAuthentication { request =>
    val identity = Identity(request).get
    FaciaToolMetrics.DraftPublishCount.increment()
    for {
      blockOption <- FaciaApi.publishBlock(id, identity)
      block <- blockOption
    } {
      FaciaApi.archive(id, block, JsString("publish"), identity)
      pressCollectionId(id)
    }
    notifyContentApi(id)
    NoCache(Ok)
  }

  def discardCollection(id: String) = AjaxExpiringAuthentication { request =>
    val identity = Identity(request).get
    for {
      blockOption <- FaciaApi.discardBlock(id, identity)
    } yield for {
      block <- blockOption
    } {
      FaciaApi.archive(id, block, JsString("discard"), identity)
    }
    NoCache(Ok)
  }

  def updateCollectionMeta(id: String): Action[AnyContent] = AjaxExpiringAuthentication { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache {
      request.body.asJson flatMap(_.asOpt[CollectionMetaUpdate]) map {
        case update: CollectionMetaUpdate => {
          val identity = Identity(request).get
          UpdateActions.updateCollectionMeta(id, update, identity)
          notifyContentApi(id)
          Ok
        }
        case _ => NotFound
      } getOrElse NotFound
    }
  }

  def collectionEdits(): Action[AnyContent] = AjaxExpiringAuthentication.async { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
      request.body.asJson flatMap (_.asOpt[Map[String, UpdateList]]) map {
        case update: Map[String, UpdateList] => {
          val identity: Identity = Identity(request).get
          val updatedCollections: Future[Map[String, Block]] =
            Future.traverse(update) {
                case ("update", updateList) =>
                  UpdateActions.updateCollectionList(updateList.id, updateList, identity).map(_.map(updateList.id -> _))
                case ("remove", updateList) =>
                  UpdateActions.updateCollectionFilter(updateList.id, updateList, identity).map(_.map(updateList.id -> _))
              } //This collect turns into an Iterable[Future[Option[(String, Block)]]]
              .map(_.flatten)
              .map(_.toMap)

          updatedCollections.map { m =>
            pressCollectionIds(m.keySet)
            if (m.nonEmpty)
              Ok(Json.toJson(m)).as("application/json")
            else
              NotFound
          }
        }
        case _ => Future.successful(NotFound)
      } getOrElse Future.successful(NotFound)
  }

  def updateCollection(id: String) = AjaxExpiringAuthentication { request =>
    pressCollectionId(id)
    notifyContentApi(id)
    NoCache(Ok)
  }

  def notifyContentApi(id: String): Option[Future[Response]] =
    if (ContentApiPutSwitch.isSwitchedOn)
      ConfigAgent.getConfig(id)
        .map {config => ContentApiWrite.writeToContentapi(config)}
    else None

  def pressCollectionId(id: String): Unit = pressCollectionIds(Set(id))
  def pressCollectionIds(ids: Set[String]): Unit =
    if (Switches.FaciaToolPressSwitch.isSwitchedOn) {
      FrontPressJob.pressByCollectionIds(ids)
    }
}
