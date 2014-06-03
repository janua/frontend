package conf

import common.Assets.Assets
import common.GuardianConfiguration
import com.gu.management.play.RequestMetrics
import contentapi.{ContentApiClient, WsHttpMemcached}
import play.api.mvc.EssentialFilter
import play.filters.gzip.GzipFilter

object Configuration extends GuardianConfiguration("frontend", webappConfDirectory = "env")

//object ContentApi extends ContentApiClient

object ContentApi extends ContentApiClient {
  http = new WsHttpMemcached(httpTimingMetric, httpTimeoutMetric)
}

object Static extends Assets(Configuration.assets.path)

object RequestMeasurementMetrics extends RequestMetrics.Standard

object Gzipper extends GzipFilter()

object Filters {
  lazy val common: List[EssentialFilter] = Gzipper :: RequestMeasurementMetrics.asFilters
}
