package controllers

import common.editions.{Au, Us, Uk}
import common.{Edition, ExecutionContexts}
import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import play.api.mvc.{RequestHeader, Action, Controller}

object WeatherController extends Controller with ExecutionContexts {
  import play.api.Play.current

  val London: CityId = CityId("328328")
  val NewYork: CityId = CityId("349727")
  val Sydney: CityId = CityId("22889")

  case class City(name: String) extends AnyVal
  case class CityId(id: String) extends AnyVal

  val weatherApiKey: String = "3e74092c580e46319d36f04e68734365"

  val weatherCityUrl: String = "http://api.accuweather.com/currentconditions/v1/"
  val weatherSearchUrl: String = "http://api.accuweather.com/locations/v1/cities/search.json"

  private def weatherUrlForCity(city: City): String =
   s"$weatherSearchUrl?apikey=$weatherApiKey&q=${city.name}"

  private def weatherUrlForCityId(cityId: CityId): String =
    s"$weatherCityUrl${cityId.id}.json?apikey=$weatherApiKey"

  private def getCityIdFromRequest(request: RequestHeader): CityId =
    Edition(request) match {
      case Uk => London
      case Us => NewYork
      case Au => Sydney
    }

  def getWeatherForCity(name: String) = Action.async { implicit request =>
    lazy val cityIdFromRequest: CityId = getCityIdFromRequest(request)
    for {
      cityJson <- WS.url(weatherUrlForCity(City(name))).get().map(_.json)
      cities = cityJson.asOpt[List[JsValue]].getOrElse(Nil)
      cityId = cities.map(j => (j \ "Key").as[String]).headOption.map(CityId).getOrElse(cityIdFromRequest)
      weatherJson <- WS.url(weatherUrlForCityId(cityId)).get().map(_.json)
    } yield Ok(weatherJson)
  }
}
