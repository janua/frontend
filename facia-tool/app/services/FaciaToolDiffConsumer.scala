package services

import java.net.InetAddress
import java.util
import java.util.UUID

import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.clientlibrary.interfaces.{IRecordProcessorFactory, IRecordProcessorCheckpointer, IRecordProcessor}
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.{InitialPositionInStream, KinesisClientLibConfiguration, Worker}
import com.amazonaws.services.kinesis.clientlibrary.types.ShutdownReason
import com.amazonaws.services.kinesis.metrics.impl.NullMetricsFactory
import com.amazonaws.services.kinesis.model.Record
import conf.Configuration
import scala.collection.JavaConversions._

object FaciaToolDiffConsumer extends IRecordProcessor {

  private var shardId: String = _

  override def initialize(p1: String): Unit = {
    println(s"Initializing Consumer with shardId $p1")
    shardId = p1
  }

  override def shutdown(p1: IRecordProcessorCheckpointer, p2: ShutdownReason): Unit = {
    println("Shutting Down Consumer")
  }

  override def processRecords(p1: util.List[Record], p2: IRecordProcessorCheckpointer): Unit = {
    val records: String = p1.map(m => new String(m.getData().array)).mkString(",")
    println(s"Got records: $records")
  }

}

object RecordProcessorFactory extends IRecordProcessorFactory {
  override def createProcessor: IRecordProcessor = FaciaToolDiffConsumer
}

object FaciaToolDiffWorker {
  val workerId = InetAddress.getLocalHost.getCanonicalHostName +
    ":" + UUID.randomUUID()

  val kinesisClientLibConfiguration = new KinesisClientLibConfiguration(
    "facia-tool",
    "facia-tool-updates",
    Configuration.aws.mandatoryCredentials,
    workerId)
    .withRegionName(Regions.EU_WEST_1.getName)
    .withInitialPositionInStream(InitialPositionInStream.TRIM_HORIZON)

  val worker = new Worker(
    RecordProcessorFactory,
    kinesisClientLibConfiguration,
    new NullMetricsFactory()
  )
}
