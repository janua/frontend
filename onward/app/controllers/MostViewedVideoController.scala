package controllers

import common.{JsonComponent, Logging, ExecutionContexts}
import feed.MostViewedVideoAgent
import model.Cached
import play.api.mvc.{ Controller, Action }

object MostViewedVideoController extends Controller with Logging with ExecutionContexts {

  def renderMostViewed() = Action { implicit request =>

    val size = request.getQueryString("size").getOrElse("6").toInt
    val videos = MostViewedVideoAgent.mostViewedVideo().take(size)

    Cached(900) {
      JsonComponent(
        "html" -> views.html.fragments.mostViewedVideo(videos)
      )
    }
  }
}
