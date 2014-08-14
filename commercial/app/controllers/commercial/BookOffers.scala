package controllers.commercial

import common.ExecutionContexts
import model.commercial.books.{BestsellersAgent, Book}
import model.{Cached, NoCache}
import performance.MemcachedAction
import play.api.mvc._
import play.twirl.api.Html

import scala.concurrent.Future

object BookOffers extends Controller with ExecutionContexts with implicits.Collections {

  object lowRelevance extends Relevance[Book] {
    override def view(books: Seq[Book])(implicit request: RequestHeader): Html =
      views.html.books.bestsellers(books)
  }

  object mediumRelevance extends Relevance[Book] {
    override def view(books: Seq[Book])(implicit request: RequestHeader): Html =
      views.html.books.bestsellersMedium(books)
  }

  object highRelevance extends Relevance[Book] {
    override def view(books: Seq[Book])(implicit request: RequestHeader): Html =
      views.html.books.bestsellersHigh(books)
  }
  
  private def renderBestsellers(relevance: Relevance[Book], format: Format) =
    MemcachedAction { implicit request =>
      Future.successful {
        (BestsellersAgent.getSpecificBooks(specificIds) ++ BestsellersAgent.adsTargetedAt(segment))
          .distinctBy(_.isbn).take(5) match {
          case Nil => NoCache(format.nilResult)
          case books => Cached(componentMaxAge) {
            format.result(relevance.view(books))
          }
        }
      }
    }

  private def renderSingleBook(format: Format) = MemcachedAction { implicit request =>
    Future.successful {
      specificId.flatMap { isbn =>
        BestsellersAgent.getSpecificBook(isbn) map { book =>
          Cached(componentMaxAge) {
            format.result(views.html.books.bestsellersSuperHigh(book))
          }
        }
      }.getOrElse {
        NoCache(format.nilResult)
      }
    }
  }

  def bestsellersLowJson = renderBestsellers(lowRelevance, jsonFormat)
  def bestsellersLowHtml = renderBestsellers(lowRelevance, htmlFormat)

  def bestsellersMediumJson = renderBestsellers(mediumRelevance, jsonFormat)
  def bestsellersMediumHtml = renderBestsellers(mediumRelevance, htmlFormat)

  def bestsellersHighJson = renderBestsellers(highRelevance, jsonFormat)
  def bestsellersHighHtml = renderBestsellers(highRelevance, htmlFormat)

  def bestsellersSuperHighJson = renderSingleBook(jsonFormat)
  def bestsellersSuperHighHtml = renderSingleBook(htmlFormat)

}
