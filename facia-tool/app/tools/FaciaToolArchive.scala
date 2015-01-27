package tools

import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient
import com.amazonaws.services.dynamodbv2.model._
import common.{ExecutionContexts, Logging}
import conf.{Switches, Configuration}
import org.joda.time.{DateTimeZone, DateTime}
import play.api.libs.json._
import scala.collection.JavaConverters._
import awswrappers.dynamodb._
import com.amazonaws.services.dynamodbv2.document.{DynamoDB, Item}

import scala.util.{Failure, Success}

case class ArchiveRequest(email: String, updateJson: JsValue, diff: JsValue)

object FaciaToolArchive extends ExecutionContexts with Logging {
  val TableName = "FaciaToolUpdateHistoryFrancis"
  private val maybeDynamoClient: Option[AmazonDynamoDBAsyncClient] = {
    val c = new AmazonDynamoDBAsyncClient(Configuration.aws.credentials.get)
    c.setRegion(Region.getRegion(Regions.EU_WEST_1))
    Option(c)
  }

  def dayKey(date: DateTime) = date.toString("yyyy-MM-dd")
  def timeKey(date: DateTime) = date.toString("HH:mm:ss")

  def archive(archiveRequest: ArchiveRequest): Unit = {
    maybeDynamoClient match {
      case Some(dynamoAsyncClient) =>
        Json.toJson(archiveRequest.updateJson).transform[JsObject](Reads.JsObjectReads) match {
          case JsSuccess(result, _) =>
            val client = new DynamoDB(dynamoAsyncClient)
            val table = client.getTable(TableName)
            val now = DateTime.now().withZone(DateTimeZone.UTC)

            val archiveJson: String = Json.prettyPrint(result + ("diff", archiveRequest.diff))

            val item = new Item()
              .withString("date", dayKey(now))
              .withString("time", timeKey(now))
              .withString("email", archiveRequest.email)
              .withJSON("rawupdate", archiveJson)

            table.putItem(item)

          case JsError(errors)  => log.warn(s"Could not archive request from ${archiveRequest.email}: $errors")}
      //case Some(_) => log.warn(s"Did not archive to dynamo for ${archiveRequest.email}; switched OFF")
      case None    => log.warn(s"No client to archive record for ${archiveRequest.email}, is this PROD")
    }
  }
}
