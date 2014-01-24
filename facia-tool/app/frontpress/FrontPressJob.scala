package jobs

import common.{ExecutionContexts, Logging}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import conf.Configuration
import com.amazonaws.services.sqs.model._
import com.amazonaws.regions.{Regions, Region}
import scala.collection.JavaConversions._
import services.{PorterConfigAgent, S3FrontsApi, FrontPress}
import play.api.libs.json.Json
import scala.util.Success
import scala.concurrent.duration._
import scala.concurrent.Await

object FrontPressJob extends ExecutionContexts with Logging with implicits.Collections {

  val queueUrl: String = "https://sqs.eu-west-1.amazonaws.com/642631414762/facia-static-queue"

  def newClient = {
    val c = new AmazonSQSAsyncClient(Configuration.aws.credentials)
    c.setRegion(Region.getRegion(Regions.EU_WEST_1))
    c
  }

  def run() {
    val client = newClient
    try {
      val receiveMessageResult = client.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10))
      receiveMessageResult.getMessages
        .map(getConfigFromMessage)
        .flatten
        .distinct
        .map { config =>
          val f = FrontPress.generateJson(config).andThen {
            case Success(json) => {
              (json \ "id").asOpt[String].foreach(S3FrontsApi.putPressedJson(_, Json.prettyPrint(json)))
            }
          }
          f.onSuccess {
            case _ =>
              client.deleteMessageBatch(
                new DeleteMessageBatchRequest(
                  queueUrl,
                  receiveMessageResult.getMessages.map { msg => new DeleteMessageBatchRequestEntry(msg.getMessageId, msg.getReceiptHandle)}
                )
              )
          }
          f.onFailure {
            case t: Throwable => log.warn(t.toString)
          }
          Await.ready(f, 20.seconds) //Block until ready!
      }
    } catch {
      case t: Throwable => log.warn(t.toString)
    }
  }

  def pressByCollectionId(id: String): Unit = {
    for {
      path <- PorterConfigAgent.getConfigsUsingCollectionId(id)
      json <- FrontPress.generateJson(path)
    } {
      (json \ "id").asOpt[String].foreach(S3FrontsApi.putPressedJson(_, Json.prettyPrint(json)))
    }
  }

  def getConfigFromMessage(message: Message): List[String] = {
    val id = (Json.parse(message.getBody) \ "Message").as[String]
    val configIds: Seq[String] = PorterConfigAgent.getConfigsUsingCollectionId(id)
    configIds.toList
  }

}
