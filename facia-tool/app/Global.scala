import common._
import conf.{Gzipper, Management}
import java.io.File
import play.api._
import play.api.mvc.WithFilters
import services.ConfigAgentLifecycle

object Global extends WithFilters(Gzipper)
  with GlobalSettings
  with CloudWatchApplicationMetrics
  with ConfigAgentLifecycle {
  lazy val devConfig = Configuration.from(Map("session.secure" -> "false"))

  override lazy val applicationName = Management.applicationName
  override def applicationMetrics: Map[String, FrontendMetric] = Map(
    ("api-usage", FrontendMetric(FaciaToolMetrics.ApiUsageCount.getAndReset.toDouble)),
    ("api-proxy-usage", FrontendMetric(FaciaToolMetrics.ProxyCount.getAndReset.toDouble)),
    ("content-api-put-failure", FrontendMetric(FaciaToolMetrics.ContentApiPutFailure.getAndReset.toDouble)),
    ("content-api-put-success", FrontendMetric(FaciaToolMetrics.ContentApiPutSuccess.getAndReset.toDouble)),
    ("draft-publish", FrontendMetric(FaciaToolMetrics.DraftPublishCount.getAndReset.toDouble)),
    ("auth-expired", FrontendMetric(FaciaToolMetrics.ExpiredRequestCount.getAndReset.toDouble)),
    ("elastic-content-api-calls", FrontendMetric(ContentApiMetrics.ElasticHttpTimingMetric.getAndReset.toDouble)),
    ("elastic-content-api-timeouts", FrontendMetric(ContentApiMetrics.ElasticHttpTimeoutCountMetric.getAndReset.toDouble)),
    ("content-api-404", FrontendMetric(ContentApiMetrics.ContentApi404Metric.getAndReset.toDouble)),
    ("content-api-client-parse-exceptions", FrontendMetric(ContentApiMetrics.ContentApiJsonParseExceptionMetric.getAndReset.toDouble)),
    ("content-api-client-mapping-exceptions", FrontendMetric(ContentApiMetrics.ContentApiJsonMappingExceptionMetric.getAndReset.toDouble)),
    ("content-api-invalid-content-exceptions", FrontendMetric(FaciaToolMetrics.InvalidContentExceptionMetric.getAndReset.toDouble)),
    ("s3-client-exceptions", FrontendMetric(S3Metrics.S3ClientExceptionsMetric.getAndReset.toDouble))
  )

  override def onLoadConfig(config: Configuration, path: File, classloader: ClassLoader, mode: Mode.Mode): Configuration = {
    val newConfig: Configuration = if (mode == Mode.Dev) config ++ devConfig else config
    super.onLoadConfig(newConfig, path, classloader, mode)
  }
}