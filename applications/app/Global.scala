import common.CloudWatchApplicationMetrics
import conf.{Management, Filters}
import dev.DevParametersLifecycle
import dfp.DfpAgentLifecycle
import play.api.mvc.WithFilters

object Global extends WithFilters(Filters.common: _*)
                      with DevParametersLifecycle
                      with CloudWatchApplicationMetrics
                      with DfpAgentLifecycle {
  override lazy val applicationName = Management.applicationName
}
