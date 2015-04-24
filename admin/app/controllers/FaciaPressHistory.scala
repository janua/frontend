package controllers.admin

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.{ConfigAgent, S3FrontsApi}

object FaciaPressHistory extends Controller {

  def listPaths = AuthActions.AuthActionTest {
    Ok(Json.toJson(ConfigAgent.getPathIds.sorted)).as("application/json")
  }

  def getHistoryFor(key: String) = AuthActions.AuthActionTest {
    Ok(Json.prettyPrint(Json.toJson(S3FrontsApi.getHistoryForPath(key)))).as("application/json")
  }

  def getHistoryForWithVersion(key: String, versionIdMarker: String) = AuthActions.AuthActionTest {
    Ok(Json.prettyPrint(Json.toJson(S3FrontsApi.getHistoryForPathWithVersion(key, Option(versionIdMarker))))).as("application/json")
  }

  def getPressedHistoryFile(key: String, versionId: String) = AuthActions.AuthActionTest {
    Ok(Json.prettyPrint(Json.toJson(S3FrontsApi.getPressedHistoryVersion(key, versionId))))
  }

  def restoreVersion(key: String, versionId: String) = AuthActions.AuthActionTest {
    S3FrontsApi.restorePressedVersion(key, versionId)
    Ok(Json.toJson(s"Restored $key to $versionId"))
  }

  def index = AuthActions.AuthActionTest {
    Ok(views.html.pressedhistory())
  }
}
