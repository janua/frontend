package controllers

import play.api.mvc._
import common.{FaciaToolMetrics, ExecutionContexts}
import model.Cached
import conf.Switches
import play.api.libs.json.Json

object SwitchesProxy extends Controller with ExecutionContexts {

  def getSwitches() = Action { request =>
    FaciaToolMetrics.ProxyCount.increment()
    val r = Switches.all.map {switch =>
      switch.name -> switch.isSwitchedOn
    }.toMap
    Cached(60){Ok(Json.toJson(r)).as("application/json")}
  }

}
