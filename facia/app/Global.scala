import common.{FrontendMetric, ContentApiMetrics, FaciaMetrics, CloudWatchApplicationMetrics}
import conf.{Management, RequestMeasurementMetrics}
import controllers.front._
import dev.DevParametersLifecycle
import play.api.mvc.WithFilters


object Global extends WithFilters(RequestMeasurementMetrics.asFilters: _*) with FrontLifecycle
                                                        with DevParametersLifecycle with CloudWatchApplicationMetrics {
  override lazy val applicationName = Management.applicationName

  override def applicationMetrics: Seq[FrontendMetric[_]] = Seq(
    FaciaMetrics.S3AuthorizationError,
    FaciaMetrics.JsonParsingErrorCount,
    ContentApiMetrics.ElasticHttpTimeoutCountMetric,
    ContentApiMetrics.HttpTimeoutCountMetric,
    ContentApiMetrics.ContentApi404Metric
  )

}
