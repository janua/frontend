package controllers

import play.api.mvc.{ Action, Controller }
import services.{S3FrontsApi, PagePresser}
import common.ExecutionContexts
import play.api.libs.json.Json
import jobs.PagePresserJob

object IndexController extends Controller with ExecutionContexts {
  def index() = Action { Ok("Ok") }

  def pressId(id: String) = Action.async {
    val json = PagePresser.generateJson(id)
    json.map{j =>
      S3FrontsApi.putPressedJson(id, Json.prettyPrint(j))
      Ok(Json.prettyPrint(j))
    }
  }

  def getQueue = Action {PagePresserJob.run(); Ok}

  def printId(id: String) = Action.async {
    PagePresser.pressPage(id).map(l => Ok(l.map(t => s"Config: ${t._1.id}\n${t._2.results.map(_.url).mkString("\n")}").mkString("\n")))
  }
}
