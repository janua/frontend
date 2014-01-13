package jobs

import common.{ExecutionContexts, Logging}
import com.amazonaws.services.sqs.AmazonSQSAsyncClient
import conf.Configuration
import com.amazonaws.services.sqs.model.{DeleteMessageBatchRequestEntry, DeleteMessageBatchRequest, ReceiveMessageRequest, GetQueueAttributesRequest}
import com.amazonaws.regions.{Regions, Region}
import scala.collection.JavaConversions._
import services.{S3FrontsApi, PagePresser}
import scala.util.Success
import play.api.libs.json.Json

object PagePresserJob extends ExecutionContexts with Logging {

  val queueUrl: String = "https://sqs.eu-west-1.amazonaws.com/642631414762/facia-static-queue"

  def run() {
    println("Running page presser job")

    val client = new AmazonSQSAsyncClient(Configuration.aws.credentials)
    client.setRegion(Region.getRegion(Regions.EU_WEST_1))

    val r = client.receiveMessage(new ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(5))
    r.getMessages.map { m =>
      val f = PagePresser.generateJson(m.getBody).andThen{
        case Success(j) => S3FrontsApi.putPressedJson((j \ "id").as[String], Json.prettyPrint(j))

      }
      f.onSuccess {case _ =>
        client.deleteMessageBatch(
          new DeleteMessageBatchRequest(queueUrl, r.getMessages.map{ m =>
            new DeleteMessageBatchRequestEntry(m.getMessageId, m.getReceiptHandle)
          }
          )
        )
      }
      f.onFailure{case t: Throwable => println(t)}
      f
    }
    println("Page Presser")
  }

  def receive(t: Any) = {}
}
