package common
 
import org.joda.time.{Period, DateTime}
import akka.agent.Agent
import java.util.concurrent.atomic.AtomicLong
import akka.util.Timeout
import scala.concurrent.duration._
import scala.math.ceil
import com.google.common.cache.{CacheLoader, CacheBuilder, LoadingCache}

trait ResettingCountMetric {
     val name: String
     def increment(): Long
     def getAndReset: Long
     def getCurrentCount: Long
   }
 
trait ConsecutiveErrorsHealthCheck extends implicits.Dates {
  val startTime: DateTime
  val period: Period
  val expiringCache: LoadingCache[Int, Int] = CacheBuilder
    .newBuilder()
    .maximumSize(100)
    .build(
      new CacheLoader[Int, Int]() {
        override def load(key: Int): Int = 0
      }
    )

  def calculatePeriod(dateTime: DateTime): Int = {
    val things = Stream.iterate(dateTime){ d => if (d.minus(period) < startTime) None else Some(1)}
    ceil(new Period(startTime, dateTime).)
  }


}

case class ConsecutiveErrorsHealthCheckCountMetric(name: String, period: Period, errorThreshold: Int, consecutivePeriodThreshold: Int)

}
