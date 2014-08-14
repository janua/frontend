package views.support

import play.twirl.api.Html
import play.api.Play
import play.api.Play.current

object AdLink {

  def apply(html: Html): String = {
    val url: String = html.body.trim
    if (Play.isDev) url else s"%OASToken%$url%OmnitureToken%"
  }

}
