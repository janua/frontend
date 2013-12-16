package model

import common.Jobs
import feed.{MostPopularExpandableAgent, MostPopularAgent}
import play.api.{ Application => PlayApp, GlobalSettings }


trait MostPopularLifecycle extends GlobalSettings {
  override def onStart(app: PlayApp) {
    super.onStart(app)

    Jobs.deschedule("MostPopularAgentRefreshJob")

    // fire every min
    Jobs.schedule("MostPopularAgentRefreshJob",  "0 * * * * ?") {
      MostPopularAgent.refresh()
      MostPopularExpandableAgent.refresh()
    }
  }

  override def onStop(app: PlayApp) {
    Jobs.deschedule("MostPopularAgentRefreshJob")
    super.onStop(app)
  }
}
