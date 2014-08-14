package frontpress

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import common.SQSQueues._
import common._
import conf.Configuration
import metrics._
import org.joda.time.DateTime
import services.{Draft, FrontPath, Live, PressJob}

import scala.concurrent.Future
import scala.util.{Failure, Success}

object ToolPressQueueWorker extends JsonQueueWorker[PressJob] with Logging {
  override val queue = (Configuration.faciatool.frontPressToolQueue map { queueUrl =>

    val credentials = Configuration.aws.mandatoryCredentials

    JsonMessageQueue[PressJob](
      new AmazonSQSAsyncClient(credentials).withRegion(Region.getRegion(Regions.EU_WEST_1)),
      queueUrl
    )
  }) getOrElse {
    throw new RuntimeException("Required property 'frontpress.sqs.tool_queue_url' not set")
  }

  /** We record separate metrics for each of the editions' network fronts */
  val metricsByPath = Map(
    "uk" -> UkPressLatencyMetric,
    "us" -> UsPressLatencyMetric,
    "au" -> AuPressLatencyMetric
  )

  override def process(message: Message[PressJob]): Future[Unit] = {
    val PressJob(FrontPath(path), pressType, creationTime) = message.get

    log.info(s"Processing job from tool to update $path on $pressType")

    val pressFuture = pressType match {
      case Draft => FrontPress.pressDraftByPathId(path)
      case Live => FrontPress.pressLiveByPathId(path)
    }

    pressFuture onComplete {
      case Success(_) =>
        pressType match {
          case Draft => FaciaPressMetrics.FrontPressDraftSuccess.increment()
          case Live => FaciaPressMetrics.FrontPressLiveSuccess.increment()
        }

        val millisToPress: Long = DateTime.now.getMillis - creationTime.getMillis

        if (millisToPress < 0) {
          log.error(s"Tachyons messing up our pressing! (pressed in ${millisToPress}ms)")
        } else {
          AllFrontsPressLatencyMetric.recordDuration(millisToPress)

          metricsByPath.get(path) foreach { metric =>
            metric.recordDuration(millisToPress)
          }
        }

        log.info(s"Successfully pressed $path on $pressType after ${millisToPress}ms")

      case Failure(error) =>
        pressType match {
          case Draft => FaciaPressMetrics.FrontPressDraftFailure.increment()
          case Live => FaciaPressMetrics.FrontPressLiveFailure.increment()
        }
        log.error(s"Failed to press $path on $pressType", error)
    }

    pressFuture.map(_ => ())
  }
}
