package controllers.admin

import common.{ExecutionContexts, Logging}
import play.api.mvc.{Action, Controller}
import services.ConfigAgent

object WhatIsDeduped extends Controller with Logging with ExecutionContexts {

  def index() = Action {
    val paths = ConfigAgent.getPathIds.sorted
    Ok(views.html.dedupePathsList(paths))
  }

}
