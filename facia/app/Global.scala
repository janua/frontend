import common._
import conf.{Management, Filters}
import dev.DevParametersLifecycle
import dfp.DfpAgentLifecycle
import ophan.SurgingContentAgentLifecycle
import play.api.mvc.WithFilters
import services.ConfigAgentLifecycle


object Global extends WithFilters(Filters.common: _*)
with ConfigAgentLifecycle
with DevParametersLifecycle
with CloudWatchApplicationMetrics
with DfpAgentLifecycle
with SurgingContentAgentLifecycle {
  override lazy val applicationName = Management.applicationName

  override def applicationMetrics: Map[String, FrontendMetric] = super.applicationMetrics ++ Map(
    ("s3-authorization-error", FrontendMetric(S3Metrics.S3AuthorizationError.getAndReset.toDouble)),
    ("json-parsing-error", FrontendMetric(FaciaMetrics.JsonParsingErrorCount.getAndReset.toDouble)),
    ("elastic-content-api-calls", FrontendMetric(ContentApiMetrics.ElasticHttpTimingMetric.getAndReset.toDouble)),
    ("elastic-content-api-timeouts", FrontendMetric(ContentApiMetrics.ElasticHttpTimeoutCountMetric.getAndReset.toDouble)),
    ("content-api-client-parse-exceptions", FrontendMetric(ContentApiMetrics.ContentApiJsonParseExceptionMetric.getAndReset.toDouble)),
    ("content-api-client-mapping-exceptions", FrontendMetric(ContentApiMetrics.ContentApiJsonMappingExceptionMetric.getAndReset.toDouble)),
    ("content-api-invalid-content-exceptions", FrontendMetric(FaciaToolMetrics.InvalidContentExceptionMetric.getAndReset.toDouble)),
    ("redirects-to-applications", FrontendMetric(FaciaMetrics.FaciaToApplicationRedirectMetric.getAndReset.toDouble))
  )

}
