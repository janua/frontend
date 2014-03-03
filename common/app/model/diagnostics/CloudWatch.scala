package model.diagnostics

import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.handlers.AsyncHandler
import conf.Configuration
import com.amazonaws.services.cloudwatch.model._
import scala.collection.JavaConversions._
import common.{FrontendMetric, Logging}
import Configuration._

trait CloudWatch extends Logging {

  lazy val stage = new Dimension().withName("Stage").withValue(environment.stage)

  lazy val cloudwatch = {
    val client = new AmazonCloudWatchAsyncClient(Configuration.aws.credentials)
    client.setEndpoint("monitoring.eu-west-1.amazonaws.com")
    client
  }

  object asyncHandler extends AsyncHandler[PutMetricDataRequest, Void] with Logging
  {
    def onError(exception: Exception)
    {
      log.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
    }
    def onSuccess(request: PutMetricDataRequest, result: Void )
    {
      log.info("CloudWatch PutMetricDataRequest - sucess")
    }
  }

  def put(namespace: String, metrics: Seq[FrontendMetric[_]], dimensions: Seq[Dimension]): Any = {
    val request = new PutMetricDataRequest().
      withNamespace(namespace).
      withMetricData(metrics.map{ metric =>
      new MetricDatum()
        .withValue(metric.getAndReset.toDouble)
        .withMetricName(metric.name)
        .withUnit(metric.countUnit)
        .withDimensions(dimensions)
    })

    cloudwatch.putMetricDataAsync(request, asyncHandler)
  }

  def put(namespace: String, metrics: Seq[FrontendMetric[_]]): Any =
    put(namespace, metrics, Seq(stage))

  def putWithDimensions(namespace: String, metrics: Seq[FrontendMetric[_]], dimensions: Seq[Dimension]) =
    put(namespace, metrics, Seq(stage) ++ dimensions)

}

object CloudWatch extends CloudWatch
