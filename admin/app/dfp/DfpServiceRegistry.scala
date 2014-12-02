package dfp

import com.google.api.ads.dfp.axis.factory.DfpServices
import com.google.api.ads.dfp.axis.v201411._
import com.google.api.ads.dfp.lib.client.DfpSession

case class DfpServiceRegistry(session: DfpSession) {

  private val dfpServices = new DfpServices()

  lazy val lineItemService =
    dfpServices.get(session, classOf[LineItemServiceInterface])

  lazy val customFieldService =
    dfpServices.get(session, classOf[CustomFieldServiceInterface])

  lazy val customTargetingService =
    dfpServices.get(session, classOf[CustomTargetingServiceInterface])

  lazy val inventoryService =
    dfpServices.get(session, classOf[InventoryServiceInterface])

  lazy val suggestedAdUnitService =
    dfpServices.get(session, classOf[SuggestedAdUnitServiceInterface])

  lazy val placementService =
    dfpServices.get(session, classOf[PlacementServiceInterface])

  lazy val creativeTemplateService =
    dfpServices.get(session, classOf[CreativeTemplateServiceInterface])

  lazy val creativeService =
    dfpServices.get(session, classOf[CreativeServiceInterface])

}
