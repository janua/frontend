package model.diagnostics.analytics

import common.{FrontendMetric, Logging}
import model.diagnostics.CloudWatch

object UploadJob extends Logging {
  def run() {

    log.info("Uploading count metrics")

    val metrics = Metric.metrics.map{ case (prefix, metric) =>
      s"${metric.namespace}-${metric.name}" -> FrontendMetric(metric.count.getAndSet(0).toDouble)
    }

    // Cloudwatch will not take more than 20 metrics at a time
    metrics.grouped(20).map(_.toMap).foreach(CloudWatch.put("Diagnostics", _))
  }

}
