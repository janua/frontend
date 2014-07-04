package common

import org.scalatest.{BeforeAndAfter, Matchers, FlatSpec}
import org.scalatest.concurrent.Eventually
import org.joda.time.Period
import play.api.test._
import play.api.test.Helpers._
import org.scalatest.time.{Seconds, Millis, Span}

class ConsecutiveErrorsHealthCheckCountMetricTest extends FlatSpec with Eventually with Matchers {

  implicit val p: PatienceConfig = PatienceConfig()
  private def periodInSeconds(s: Int): Period = Period.seconds(s)

  "ConsecutiveErrorsHealthCheckCountMetric" should "be healthy and eventually unhealthy and eventually healthy" in {
    running(FakeApplication()) {
      val localPatience: PatienceConfig = PatienceConfig(scaled(Span(3, Seconds)))

      val metric = ConsecutiveErrorsHealthCheckCountMetric("test", periodInSeconds(2), 2, 1)

      metric.isHealthy should be(true)

      metric.increment()
      metric.isHealthy should be(true)

      eventually {
        metric.increment()
        metric.isHealthy should be(false)
      }(localPatience)

      eventually {
        metric.isHealthy should be(true)
      }(localPatience)
    }
  }

  it should "be healthy" in {
    running(FakeApplication()) {
      val metric = ConsecutiveErrorsHealthCheckCountMetric("test", periodInSeconds(0), 2, 1)

      metric.isHealthy should be (true)
    }
  }

  it should "be healthy when it has expired but passed it's threshold" in {
    running(FakeApplication()) {
      val localPatience: PatienceConfig = PatienceConfig(scaled(Span(3, Seconds)))
      val metric = ConsecutiveErrorsHealthCheckCountMetric("test", periodInSeconds(2), 1, 1)

      metric.isHealthy should be (true) //Because consecutive won't go up unless incremented

      eventually {
        metric.increment()
        metric.isHealthy should be (false)
      }

      eventually {
        metric.increment()
        metric.isHealthy should be (true)
      }(localPatience)
    }
  }

}
