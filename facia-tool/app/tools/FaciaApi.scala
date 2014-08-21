package tools

import frontsapi.model.{Config, Block}
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json}
import services.S3FrontsApi
import scala.util.Try
import com.gu.googleauth.UserIdentity

trait FaciaApiRead {
  def getSchema: Option[String]
  def getBlock(id: String): Option[Block]
}

trait FaciaApiWrite {
  def putBlock(id: String, block: Block): Block
  def publishBlock(id: String, block: Block): Block
  def discardBlock(id: String, block: Block): Block
  def archive(id: String, block: Block, update: JsValue, identity: UserIdentity): Block
}

object FaciaApi extends FaciaApiRead with FaciaApiWrite {

  def getSchema = S3FrontsApi.getSchema
  def getBlock(id: String) = for {
    blockJson <- S3FrontsApi.getBlock(id)
    block <- Json.parse(blockJson).asOpt[Block]
  } yield block

  def putBlock(id: String, block: Block): Block = {
    Try(S3FrontsApi.putBlock(id, Json.prettyPrint(Json.toJson(block))))
    block
  }

  def publishBlock(id: String, block: Block): Block =
    block.draft.map{ draftList => putBlock(id, block.copy(live = draftList, draft = None))}.getOrElse(block)

  def discardBlock(id: String, block: Block): Block =
    putBlock(id, block.copy(draft = None))

  def archive(id: String, block: Block, update: JsValue, identity: UserIdentity): Block = {
    val newBlock: Block = block.copy(diff = Some(update))
    val lastBlock: Option[String] = S3FrontsApi.archive(id, Json.prettyPrint(Json.toJson(newBlock)), identity)
    newBlock.copy(lastBlock = lastBlock)
  }

  def putMasterConfig(config: Config): Option[Config] = {
    Try(S3FrontsApi.putMasterConfig(Json.prettyPrint(Json.toJson(config)))).map(_ => config).toOption
  }
  def archiveMasterConfig(config: Config, identity: UserIdentity): Unit = S3FrontsApi.archiveMasterConfig(Json.prettyPrint(Json.toJson(config)), identity)
}