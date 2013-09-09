package common

import akka.agent.Agent
import play.api.libs.concurrent.{ Akka => PlayAkka }
import scala.concurrent.duration._
import play.api.Play
import scala.concurrent.Future

trait ExecutionContexts {
  implicit lazy val executionContext = play.api.libs.concurrent.Execution.Implicits.defaultContext
}

object AkkaAgent {
  def apply[T](value: T) = Agent(value)(PlayAkka.system(Play.current))
}

object AkkaAsync extends ExecutionContexts {
  def apply[T](delay: Int)(body: => T): Future[T] = {
    {
      Future {
        var t: Option[T] = None
        PlayAkka.system(Play.current).scheduler.scheduleOnce(delay.seconds) { t = Option(body) }
        t.get //Don't care if it throws
      }
    }
  }
}