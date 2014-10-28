package controllers.admin

import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}
import services.S3FrontsApi

object FaciaPressHistory extends Controller {

  def getHistoryFor(key: String) = Action {
    Ok(Json.prettyPrint(Json.toJson(S3FrontsApi.getHistoryForPath(key)))).as("application/json")
  }

  def getPressedHistoryFile(key: String, versionId: String) = Action {
    Ok(Json.prettyPrint(Json.toJson(S3FrontsApi.getPressedHistoryVersion(key, versionId))))
  }

  def restoreVersion(key: String, versionId: String) = Action {
    S3FrontsApi.restorePressedVersion(key, versionId)
    Ok(s"Restored $key to $versionId")
  }
}
