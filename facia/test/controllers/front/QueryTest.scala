package controllers.front

import scala.concurrent.Future
import model.{Collection, Config}
import common.Edition
import common.editions.Uk
import org.scalatest.{Matchers, FlatSpec}
import test.Fake
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import play.api.libs.ws.Response

class CustomException(message: String) extends Exception(message)

class FakeParseConfig(response: Future[Seq[Config]]) extends ParseConfig {
  override def getConfig(id: String): Future[Seq[Config]] = response
}

class FakeParseCollection(response: Future[Collection]) extends ParseCollection {
  override def getCollection(id: String, config: Config, edition: Edition, isWarmedUp: Boolean): Future[Collection]
    = response
}

class FailingConfigQuery(id: String) extends Query(id, Uk) {
  override def getConfig(id: String): Future[Seq[Config]] = Future.failed(new CustomException("Config Failed"))
  override def getCollection(id: String, config: Config, edition: Edition, isWarmedUp: Boolean): Future[Collection]
    = Future.successful(Collection(Nil, None))
}

class FailingCollectionQuery(id: String) extends Query(id, Uk) {
  override def getConfig(id: String): Future[Seq[Config]] = Future.successful(Seq(Config(id, None, None)))
  override def getCollection(id: String, config: Config, edition: Edition, isWarmedUp: Boolean): Future[Collection]
    = Future.failed(new Throwable("Collection Failed"))
}

class FailingHttpCollectionQuery(id: String) extends Query(id, Uk) with MockitoSugar {
  val response = mock[Response]

  when(response.status) thenReturn 403
  when(response.body) thenReturn "{}"

  override def requestCollection(id: String) = Future.successful(response)
}

class QueryTest extends FlatSpec with Matchers with ScalaFutures {

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(2, Seconds), interval = Span(5, Millis))

  "Query" should "start with minimal contents depending on id" in Fake {
    val query = new FailingConfigQuery("uk")
    query.queryAgent() should be (Some(List(FaciaDefaults.configTuple("uk"))))
    query.items should be (None)

    val query2 = new FailingConfigQuery("some/where")
    query2.queryAgent() should be (Some(List(FaciaDefaults.configTuple("some/where"))))
    query2.items should be (None)
  }

  it should "not bubble up the exception in getting config" in Fake {
    val query = new FailingConfigQuery("uk")
    whenReady(query.getItems){l =>
      l.length should be (1)
      l.forall(_._2.isRight)
      l.forall(_._1.id == "uk")
    }
  }

  it should "not bubble up the exception in getting collection" in Fake {
    val query = new FailingCollectionQuery("uk")
    whenReady(query.getItems){l =>
      l.length should be (1)
      l.forall(_._2.isLeft)
      l.forall(_._1.id == "uk")
    }
  }

  it should "return Nil for 403 errors" in Fake {
    val query = new FailingHttpCollectionQuery("uk")
    whenReady(query.getCollection("uk", Config("uk", None, None), Uk, isWarmedUp=true)) { collection =>
      collection.items.length should be (0)
    }
  }
}
