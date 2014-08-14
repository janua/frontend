import common.CloudWatchApplicationMetrics
import conf.{Configuration, Filters}
import dev.DevParametersLifecycle
import feed.{MostReadLifecycle, OnwardJourneyLifecycle}
import play.api.mvc.WithFilters

object Global extends WithFilters(Filters.common: _*) with OnwardJourneyLifecycle
                                                                           with DevParametersLifecycle
                                                                           with CloudWatchApplicationMetrics
                                                                           with MostReadLifecycle {
  override lazy val applicationName = "frontend-onward"
}