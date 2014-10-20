package controllers.admin

import play.api.mvc.{Controller, Action}

object PressedHistory extends Controller {

  def index = Action {
    Ok(views.html.pressedhistory())
  }
}
