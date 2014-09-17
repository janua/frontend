package controllers

import common.ExecutionContexts
import play.api.mvc.{Action, Controller}
import services.{LiveCollections, ConfigAgent}

object Application extends Controller with ExecutionContexts {
  def index = Action {
    Ok("Hello, I am the Facia Press.")
  }
  
  def showCurrentConfig = Action {
    Ok(ConfigAgent.contentsAsJsonString).withHeaders("Content-Type" -> "application/json")
  }
}
