package service

import com.amazonaws.services.sns._
import com.amazonaws.regions.{Region, Regions}
import conf.Configuration
import com.amazonaws.services.sns.model.{UnsubscribeRequest, ConfirmSubscriptionRequest, SubscribeRequest}
import common.AkkaAgent

trait SNS {

  val agent = AkkaAgent[Option[String]](None)

  val arn: String = Configuration.sns.faciaSns
  val protocol: String = "http"
  val endpoint: String = ""

  def subscribe = {
    val snsClient = new AmazonSNSClient(Configuration.aws.credentials)
    snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1))

    snsClient.subscribe(new SubscribeRequest()
      .withTopicArn(arn)
      .withProtocol(protocol)
      .withEndpoint(endpoint)
    )
  }

  def unsubscribe = {
    agent.get map { subscriptionId =>
      val snsClient = new AmazonSNSClient(Configuration.aws.credentials)
      snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1))

      snsClient.unsubscribe(
        new UnsubscribeRequest()
          .withSubscriptionArn(subscriptionId)
      )
      agent.send(None)
    }
  }

  def confirmSubscription(token: String): Unit = {
    val snsClient: AmazonSNSClient = new AmazonSNSClient(Configuration.aws.credentials)
    snsClient.setRegion(Region.getRegion(Regions.EU_WEST_1))

    val confirmSubscriptionResult = snsClient.confirmSubscription(
      new ConfirmSubscriptionRequest()
        .withToken(token)
        .withTopicArn(arn)
    )
    agent.send(Option(confirmSubscriptionResult.getSubscriptionArn))
  }
}

object SNS extends SNS