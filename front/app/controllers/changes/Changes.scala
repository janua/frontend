package controllers.changes

import common.{ExecutionContexts, AkkaSupport}
import conf.ContentApi
import com.gu.openplatform.contentapi.model.Content
import org.joda.time.DateTime
import play.api.mvc.{Controller, Action}
import common.editions.Uk
import scala.collection.SortedMap

class Changes extends AkkaSupport {

  implicit val jodaDateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isAfter _)
  var articles: SortedMap[DateTime, Content] = SortedMap[DateTime, Content]()
  var lastData: DateTime = DateTime.now

  def articleIds: Iterable[String] = articles.values.collect { case article => article.id }

  def refresh() {
    val ids = articleIds
    for {
      response <- ContentApi.search(Uk).orderBy("newest").response
      result <- response.results
    }
    if (!ids.exists(_ == result.id)) {
      articles += (result.webPublicationDate -> result)
    }
  }

  def all: SortedMap[DateTime, Content] = articles

  def since(time: DateTime) = articles collect { case e: (DateTime, Content) if time.toLocalDate.isBefore(e._1.toLocalDate) => e}

}

object ChangesEndpoint extends Controller {

  val changes = new Changes

  def refresh = Action {
    changes.refresh()
    Ok
  }

  def index = Action { implicit request =>
    Ok(views.html.fragments.changes(changes.all)(request))
  }

  def articleids = Action { implicit request =>
    Ok(changes.articleIds.toString)
  }

}