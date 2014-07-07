package common

import org.joda.time.{Period, DateTime}
import akka.agent.Agent
import java.util.concurrent.atomic.AtomicLong
import akka.util.Timeout
import scala.concurrent.duration._

trait ResettingCountMetric {
  val name: String
  def increment(): Long
  def getAndReset: Long
  def getCurrentCount: Long
}

trait ConsecutiveErrorsHealthCheck extends implicits.Dates {
  val period: Period
  val errorThreshold: Int
  val consecutivePeriodThreshold: Int

  implicit val agentTimeout: Timeout = Timeout.durationToTimeout(1.seconds)
  lazy val healthCheckState: Agent[HealthCheck] = AkkaAgent(createHealthCheck)

  case class HealthCheck(age: DateTime, errors: Int, consecutivePeriod: Int) {
    def isExpired: Boolean = age.isOlderThan(period)
    def isHealthy: Boolean = errors <= errorThreshold
    def addErrorsToHealthCheck(e: Int): HealthCheck = {
      val newAge = if (isExpired) DateTime.now else age
      val consecutivePeriodNumber =
        if (isExpired && isHealthy)
          0
        else
          if (isExpired)
            consecutivePeriod + 1
          else
            consecutivePeriod


      val newErrors = if (isExpired) 0 else errors + 1
      this.copy(
        errors = newErrors,
        consecutivePeriod = consecutivePeriodNumber,
        age = newAge
      )
    }
    def increment(): HealthCheck = addErrorsToHealthCheck(1)
  }

  def createHealthCheck: HealthCheck = HealthCheck(age = DateTime.now, errors = 0, consecutivePeriod = 0)

  def isHealthy: Boolean = {
    val healthCheck = healthCheckState.get()
    println(s"Consecutive Period: ${healthCheck.consecutivePeriod} Threshold: $consecutivePeriodThreshold")
    healthCheck.consecutivePeriod <= consecutivePeriodThreshold
  }
}

case class ConsecutiveErrorsHealthCheckCountMetric(name: String, period: Period, errorThreshold: Int, consecutivePeriodThreshold: Int)
  extends ConsecutiveErrorsHealthCheck with ResettingCountMetric
{
  private val currentCount: AtomicLong = new AtomicLong(0)

  def getCurrentCount: Long = currentCount.get()
  def getAndReset: Long = currentCount.getAndSet(0)
  override def increment(): Long = {
    healthCheckState.alter { healthCheck => healthCheck.increment() }
    currentCount.incrementAndGet()
  }
}