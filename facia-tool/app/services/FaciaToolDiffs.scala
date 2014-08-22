package services

import java.nio.ByteBuffer

import com.amazonaws.regions.{Region, Regions}

import scala.collection.JavaConversions._
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.kinesis.AmazonKinesisAsyncClient
import com.amazonaws.services.kinesis.model.{PutRecordResult, PutRecordRequest}
import common.Logging
import conf.Configuration

object FaciaToolDiffs {
  val streamName: String = "facia-tool-updates"
  val partitionKey: String = "k1"

  trait KinesisLoggingAsyncHandler extends AsyncHandler[PutRecordRequest, PutRecordResult] with Logging {
    def onError(exception: Exception) {
      log.info(s"Kinesis PutRecordRequest error: ${exception.getMessage}}")
    }
    def onSuccess(request: PutRecordRequest, result: PutRecordResult) {
      log.info(s"Put diff to stream:${request.getStreamName} Seq:${result.getSequenceNumber}")
      println(s"Put diff to stream:${request.getStreamName} Seq:${result.getSequenceNumber} ${new String(request.getData.array())}")
    }
  }

  object KinesisLoggingAsyncHandler extends KinesisLoggingAsyncHandler

  val client: AmazonKinesisAsyncClient = {
    val c = new AmazonKinesisAsyncClient(Configuration.aws.mandatoryCredentials)
    c.setRegion(Region.getRegion(Regions.EU_WEST_1))
    c
  }

  def putDiff(diff: String): Unit =
    client.putRecordAsync(
      new PutRecordRequest()
        .withData(ByteBuffer.wrap(diff.getBytes))
        .withStreamName(streamName)
        .withPartitionKey(partitionKey),
      KinesisLoggingAsyncHandler
    )

  def listStreams: Unit = {
    client.describeStream("facia-tool-updates")
    println(client.listStreams().getStreamNames.mkString(","))
  }
}
