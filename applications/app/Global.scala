import common.CloudWatchApplicationMetrics
import conf.{Configuration, Filters}
import dev.DevParametersLifecycle
import dfp.DfpAgentLifecycle
import ophan.SurgingContentAgentLifecycle
import play.api.Application
import play.api.mvc.WithFilters
import services.{ContributorAlphaIndexAutoRefresh, KeywordSectionIndexAutoRefresh, KeywordAlphaIndexAutoRefresh}

object Global extends WithFilters(Filters.common: _*)
                      with DevParametersLifecycle
                      with CloudWatchApplicationMetrics
                      with DfpAgentLifecycle
                      with SurgingContentAgentLifecycle{
  override lazy val applicationName = "frontend-applications"

  override def onStart(app: Application): Unit = {
    super.onStart(app)
    KeywordSectionIndexAutoRefresh.start()
    KeywordAlphaIndexAutoRefresh.start()
    ContributorAlphaIndexAutoRefresh.start()
  }
}
