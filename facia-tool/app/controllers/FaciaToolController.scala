package controllers

import util.SanitizeInput
import frontsapi.model._
import frontsapi.model.UpdateList
import play.api.mvc._
import play.api.libs.json._
import common.{FaciaToolMetrics, ExecutionContexts, Logging}
import conf.Configuration
import tools.FaciaApi
import model.{NoCache, Cached}
import com.gu.googleauth.{UserIdentity, Actions}
import play.api.mvc.Call
import org.joda.time.DateTime
import scala.concurrent.Future
import services._
import play.api.mvc.SimpleResult
import play.api.Play

object AuthActions extends Actions {
  val loginTarget: Call = routes.OAuthLoginController.login()
}

object ExpiringActions extends Actions with implicits.Dates with implicits.Requests {
  import play.api.mvc.Results.{Forbidden, Redirect}
  val loginTarget: Call = routes.OAuthLoginController.login()

  private def withinAllowedTime(session: Session): Boolean = session.get(Configuration.cookies.lastSeenKey).map(new DateTime(_)).exists(_.age < Configuration.cookies.sessionExpiryTime)

  case class ExpiringAuthAction[A](action: Action[A]) extends Action[A] {
    lazy val parser = action.parser
    def apply(request: Request[A]): Future[SimpleResult] = {
      if (withinAllowedTime(request.session)) {
        action(request).map(_.withSession(request.session + (Configuration.cookies.lastSeenKey , DateTime.now.toString)))(executionContext)
      }
      else {
        if (request.isXmlHttpRequest)
          Future.successful(Forbidden.withNewSession)
        else {
          Future.successful(Redirect(routes.OAuthLoginController.login()))
        }
      }
    }
  }

  object LoggingAction extends LoginAuthAction {
    override protected def composeAction[A](action: Action[A]) = ExpiringAuthAction(action)
  }
}



object FaciaToolController extends Controller with Logging with ExecutionContexts {

  def priorities() = AuthActions.AuthAction { request =>
    val identity = UserIdentity.fromRequestHeader(request).get
    Cached(60) { Ok(views.html.priority(Configuration.environment.stage, "", Option(identity))) }
  }

  def collectionEditor(priority: String) = AuthActions.AuthAction { request =>
    val identity = UserIdentity.fromRequestHeader(request).get
    Cached(60) { Ok(views.html.collections(Configuration.environment.stage, priority, Option(identity))) }
  }

  def configEditor(priority: String) = AuthActions.AuthAction { request =>
    val identity = UserIdentity.fromRequestHeader(request).get
    Cached(60) { Ok(views.html.config(Configuration.environment.stage, priority, Option(identity))) }
  }

  def listCollections = AuthActions.AuthAction { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache { Ok(Json.toJson(S3FrontsApi.listCollectionIds)) }
  }

  def getConfig = ExpiringActions.LoginAuthAction { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache {
      S3FrontsApi.getMasterConfig map { json =>
        Ok(json).as("application/json")
      } getOrElse NotFound
    }
  }

  def updateConfig(): Action[AnyContent] = AuthActions.AuthAction { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    val configJson: Option[JsValue] = request.body.asJson
    NoCache {
      configJson.flatMap(_.asOpt[Config]).map(SanitizeInput.fromConfigSeo).map {
        case update: Config => {

          //Only update if it is a valid Config object
          configJson.foreach { json =>
            ConfigAgent.refreshWith(json)
          }

          val identity = UserIdentity.fromRequestHeader(request).get
          UpdateActions.putMasterConfig(update, identity)
          Ok
        }
        case _ => NotFound
      } getOrElse NotFound
    }
  }

  def readBlock(id: String) = AuthActions.AuthAction { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache {
      S3FrontsApi.getBlock(id) map { json =>
        Ok(json).as("application/json")
      } getOrElse NotFound
    }
  }

  def publishCollection(id: String) = AuthActions.AuthAction { request =>
    val identity = UserIdentity.fromRequestHeader(request).get
    FaciaToolMetrics.DraftPublishCount.increment()
    val block = FaciaApi.publishBlock(id, identity)
    block foreach { b =>
      UpdateActions.archivePublishBlock(id, b, identity)
      FaciaPress.press(PressCommand.forOneId(id).withPressDraft().withPressLive())
    }
    ContentApiPush.notifyContentApi(Set(id))
    NoCache(Ok)
  }

  def discardCollection(id: String) = AuthActions.AuthAction { request =>
    val identity = UserIdentity.fromRequestHeader(request).get
    val block = FaciaApi.discardBlock(id, identity)
    block.foreach { b =>
      UpdateActions.archiveDiscardBlock(id, b, identity)
      FaciaPress.press(PressCommand.forOneId(id).withPressDraft())
    }
    NoCache(Ok)
  }

  def updateCollectionMeta(id: String): Action[AnyContent] = AuthActions.AuthAction { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache {
      request.body.asJson flatMap(_.asOpt[CollectionMetaUpdate]) map {
        case update: CollectionMetaUpdate => {
          val identity = UserIdentity.fromRequestHeader(request).get
          UpdateActions.updateCollectionMeta(id, update, identity)
          ContentApiPush.notifyContentApi(Set(id))
          Ok
        }
        case _ => NotFound
      } getOrElse NotFound
    }
  }

  def collectionEdits(): Action[AnyContent] = AuthActions.AuthAction { request =>
    FaciaToolMetrics.ApiUsageCount.increment()
    NoCache {
      request.body.asJson flatMap (_.asOpt[Map[String, UpdateList]]) map {
        case update: Map[String, UpdateList] =>
          val identity: UserIdentity = UserIdentity.fromRequestHeader(request).get
          val updatedCollections: Map[String, Block] = update.collect {
            case ("update", updateList) =>
              UpdateActions.updateCollectionList(updateList.id, updateList, identity).map(updateList.id -> _)
            case ("remove", updateList) =>
              UpdateActions.updateCollectionFilter(updateList.id, updateList, identity).map(updateList.id -> _)
          }.flatten.toMap

          val shouldUpdateLive: Boolean = update.exists(_._2.live)

          val collectionIds = updatedCollections.keySet

          FaciaPress.press(PressCommand(
            collectionIds,
            live = shouldUpdateLive,
            draft = (updatedCollections.values.exists(_.draft.isEmpty) && shouldUpdateLive) || update.exists(_._2.draft)
          ))
          ContentApiPush.notifyContentApi(collectionIds)

          if (updatedCollections.nonEmpty)
            Ok(Json.toJson(updatedCollections)).as("application/json")
          else
            NotFound
      } getOrElse NotFound
    }
  }

  def pressLivePath(path: String) = AuthActions.AuthAction { request =>
    FaciaPressQueue.enqueue(PressJob(FrontPath(path), Live))
    NoCache(Ok)
  }

  def pressDraftPath(path: String) = AuthActions.AuthAction { request =>
    FaciaPressQueue.enqueue(PressJob(FrontPath(path), Draft))
    NoCache(Ok)
  }
  
  def updateCollection(id: String) = AuthActions.AuthAction { request =>
    FaciaPress.press(PressCommand.forOneId(id).withPressDraft().withPressLive())
    ContentApiPush.notifyContentApi(Set(id))
    NoCache(Ok)
  }

  def getLastModified(path: String) = AuthActions.AuthAction { request =>
    val now: Option[String] = S3FrontsApi.getPressedLastModified(path)
    now.map(Ok(_)).getOrElse(NotFound)
  }
}
