package jobs

import common.{ExecutionContexts, Logging}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import conf.Configuration
import com.amazonaws.services.sqs.model._
import com.amazonaws.regions.{Regions, Region}
import scala.collection.JavaConversions._
import services.{PorterConfigAgent, S3FrontsApi, FrontPress}
import scala.util.Success
import play.api.libs.json.Json
import scala.util.Success
import model.Config

object FrontPressJob extends ExecutionContexts with Logging with implicits.Collections {

  val queueUrl: String = "https://sqs.eu-west-1.amazonaws.com/642631414762/facia-static-queue"

  def client = {
    val c = new AmazonSQSAsyncClient(Configuration.aws.credentials)
    c.setRegion(Region.getRegion(Regions.EU_WEST_1))
    c
  }

  def run() {
    println("Running page presser job")

    try {
      val r = client.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(10))
      r.getMessages
        .map(getConfigFromMessage)
        .flatten
        .distinctBy(_.id)
        .map { config =>
          println(s"Pressing For ${config.id}")
          val f = FrontPress.generateJson(Option(config)).andThen{
          case Success(j) => {
            S3FrontsApi.putPressedJson((j \ "id").as[String], Json.prettyPrint(j))
          }
        }
        f.onSuccess {case _ =>
          val result = client.deleteMessageBatch(
            new DeleteMessageBatchRequest(queueUrl, r.getMessages.map{ m =>
              new DeleteMessageBatchRequestEntry(m.getMessageId, m.getReceiptHandle)
            }
            )
          )
          println(s"Succesfully deleted ${result.getSuccessful.mkString(",")}")
          println(s"Succesfully deleted ${result.getFailed.mkString(",")}")
        }
        f.onFailure{case t: Throwable => println(t)}
        f
      }
    } catch {
      case t: Throwable => println("++++" + t)
    }
    println("Page Presser Job Finished")
  }

  def getConfigFromMessage(message: Message): List[Config] = {
    val id = (Json.parse(message.getBody) \ "Message").as[String]
    val configIds: Seq[Config] = PorterConfigAgent.getConfigsUsingCollectionId(id).flatMap(PorterConfigAgent.getConfigForId).flatten
    println(configIds)
    val collectionConfigs: Seq[Config] = configIds.flatMap(c => PorterConfigAgent.getConfigForId(c.id)).flatten
    val x = ({PorterConfigAgent.getConfigForId(id).getOrElse(Nil)} ++ configIds ++ collectionConfigs).toList
    println(x)
    x
  }

}
