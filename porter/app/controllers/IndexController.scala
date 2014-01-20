package controllers

import play.api.mvc.{ Action, Controller }
import services.{PorterConfigAgent, S3FrontsApi, FrontPress}
import common.{AkkaAsync, ExecutionContexts}
import play.api.libs.json.Json
import jobs.FrontPressJob
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import conf.Configuration
import com.amazonaws.services.sns.model.PublishRequest
import com.amazonaws.regions.{Region, Regions}

object IndexController extends Controller with ExecutionContexts {
  def index() = Action { Ok("Ok") }

  def putStuffToSns = Action {
    val ids = Seq("uk", "uk/culture", "uk/commentisfree", "uk/sport", "uk/business",
                  "au", "au/culture", "au/commentisfree", "au/sport", "au/business",
                  "us", "us/culture", "us/commentisfree", "us/sport", "us/business",
                  "travel"
      )

    ids map send
    Ok
  }

  def send(message: String) {
    val request = new PublishRequest()
      .withTopicArn("arn:aws:sns:eu-west-1:642631414762:facia-push-2")
      .withMessage(message)

    AkkaAsync {
      client.publish(request)
    }
  }

  private def client = {
    val c = new AmazonSNSAsyncClient(Configuration.aws.credentials)
    c.setRegion(Region.getRegion(Regions.EU_WEST_1))
    c
  }

  def pressId(id: String) = Action.async {
    val json = FrontPress.generateJson(PorterConfigAgent.getConfig(id))
    json.map{j =>
      S3FrontsApi.putPressedJson(id, Json.prettyPrint(j))
      Ok(Json.prettyPrint(j))
    }
  }

  def getQueue = Action {FrontPressJob.run(); Ok}

  def printId(id: String) = Action.async {
    FrontPress.pressPage(PorterConfigAgent.getConfig(id)).map(l => Ok(l.map(t => s"Config: ${t._1.id}\n${t._2.results.map(_.url).mkString("\n")}").mkString("\n")))
  }
}
