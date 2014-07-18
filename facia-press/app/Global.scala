import common._
import conf.{Configuration => GuardianConfiguration, Management}
import frontpress.{ToolPressQueueWorker, FrontPressCron}
import play.api.GlobalSettings
import services.ConfigAgentLifecycle

object Global extends GlobalSettings
  with ConfigAgentLifecycle
  with CloudWatchApplicationMetrics {
  val pressJobConsumeRateInSeconds: Int = GuardianConfiguration.faciatool.pressJobConsumeRateInSeconds

  private def getTotalPressSuccessCount: Long =
    FaciaPressMetrics.FrontPressLiveSuccess.getResettingValue() + FaciaPressMetrics.FrontPressDraftSuccess.getResettingValue()

  private def getTotalPressFailureCount: Long =
    FaciaPressMetrics.FrontPressLiveFailure.getResettingValue() + FaciaPressMetrics.FrontPressDraftFailure.getResettingValue()

  override def applicationName = Management.applicationName

  override def applicationMetrics = Map(
    ("front-press-failure", FrontendMetric(getTotalPressFailureCount.toDouble)),
    ("front-press-success", FrontendMetric(getTotalPressSuccessCount.toDouble)),
    ("front-press-draft-failure", FrontendMetric(FaciaPressMetrics.FrontPressDraftFailure.getAndReset.toDouble)),
    ("front-press-draft-success", FrontendMetric(FaciaPressMetrics.FrontPressDraftSuccess.getAndReset.toDouble)),
    ("front-press-live-failure", FrontendMetric(FaciaPressMetrics.FrontPressLiveFailure.getAndReset.toDouble)),
    ("front-press-live-success", FrontendMetric(FaciaPressMetrics.FrontPressLiveSuccess.getAndReset.toDouble)),
    ("front-press-cron-success", FrontendMetric(FaciaPressMetrics.FrontPressCronSuccess.getAndReset.toDouble)),
    ("front-press-cron-failure", FrontendMetric(FaciaPressMetrics.FrontPressCronFailure.getAndReset.toDouble)),
    ("elastic-content-api-calls", FrontendMetric(ContentApiMetrics.ElasticHttpTimingMetric.getAndReset.toDouble)),
    ("elastic-content-api-timeouts", FrontendMetric(ContentApiMetrics.ElasticHttpTimeoutCountMetric.getAndReset.toDouble)),
    ("content-api-404", FrontendMetric(ContentApiMetrics.ContentApi404Metric.getAndReset.toDouble)),
    ("content-api-client-parse-exceptions", FrontendMetric(ContentApiMetrics.ContentApiJsonParseExceptionMetric.getAndReset.toDouble)),
    ("content-api-client-mapping-exceptions", FrontendMetric(ContentApiMetrics.ContentApiJsonMappingExceptionMetric.getAndReset.toDouble)),
    ("content-api-invalid-content-exceptions", FrontendMetric(FaciaToolMetrics.InvalidContentExceptionMetric.getAndReset.toDouble)),
    ("s3-client-exceptions", FrontendMetric(S3Metrics.S3ClientExceptionsMetric.getAndReset.toDouble)),
    ("s3-authorization-errors", FrontendMetric(S3Metrics.S3AuthorizationError.getAndReset.toDouble)),
    ("content-api-seo-request-success", FrontendMetric(FaciaPressMetrics.ContentApiSeoRequestSuccess.getAndReset.toDouble)),
    ("content-api-seo-request-failure", FrontendMetric(FaciaPressMetrics.ContentApiSeoRequestFailure.getAndReset.toDouble)),
    ("content-api-fallbacks", FrontendMetric(FaciaPressMetrics.MemcachedFallbackMetric.getAndReset.toDouble)),
    ("front-press-latency", FrontendMetric(FaciaPressMetrics.FrontPressLatency.getAndReset.toDouble)),
    ("uk-network-front-press-latency", FrontendMetric(FaciaPressMetrics.UkFrontPressLatency.getAndResetTime.toDouble, Milliseconds)),
    ("us-network-front-press-latency", FrontendMetric(FaciaPressMetrics.UsFrontPressLatency.getAndResetTime.toDouble, Milliseconds)),
    ("au-network-front-press-latency", FrontendMetric(FaciaPressMetrics.AuFrontPressLatency.getAndResetTime.toDouble, Milliseconds))
  )

  def scheduleJobs() {
    Jobs.schedule("FaciaToolPressJob", s"0/$pressJobConsumeRateInSeconds * * * * ?") {
      FrontPressCron.run()
    }
  }

  def descheduleJobs() {
    Jobs.deschedule("FaciaToolPressJob")
  }

  override def onStart(app: play.api.Application) {
    super.onStart(app)
    ToolPressQueueWorker.start()
    descheduleJobs()
    scheduleJobs()
  }

  override def onStop(app: play.api.Application) {
    descheduleJobs()
    super.onStop(app)
  }
}
