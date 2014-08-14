package controllers.admin

import play.api.mvc._
import football.services.GetPaClient
import pa.{StatsSummary, PlayerProfile, PlayerAppearances}
import common.{JsonComponent, ExecutionContexts}
import org.joda.time.LocalDate
import football.model.PA
import scala.concurrent.Future
import model.{Cors, NoCache, Cached}
import play.api.libs.json.{JsString, JsArray, JsObject}
import org.joda.time.format.DateTimeFormat


object PlayerController extends Controller with ExecutionContexts with GetPaClient {

  def playerIndex =AuthActions.AuthActionTest.async { request =>
    for {
      competitions <- client.competitions.map(PA.filterCompetitions)
      competitionTeams <- Future.traverse(competitions){comp => client.teams(comp.competitionId, comp.startDate, comp.endDate)}
      allTeams = competitionTeams.flatten.distinct
    } yield {
      Cached(600)(Ok(views.html.football.player.playerIndex(competitions, allTeams)))
    }
  }

  def redirectToCard =AuthActions.AuthActionTest { request =>
    val submission = request.body.asFormUrlEncoded.get
    val playerCardType = submission.get("playerCardType").get.head
    val playerId = submission.get("player").get.head
    val teamId = submission.get("team").get.head
    val result = (submission.get("competition"), submission.get("startDate")) match {
      case (Some(Seq(compId)), _) if !compId.isEmpty =>
        NoCache(SeeOther(s"/admin/football/player/card/competition/$playerCardType/$playerId/$teamId/$compId"))
      case (_, Some(Seq(startDate))) =>
        NoCache(SeeOther(s"/admin/football/player/card/date/$playerCardType/$playerId/$teamId/$startDate"))
      case _ => NoCache(NotFound(views.html.football.error("Couldn't find competition or start date in submission")))
    }
    result
  }

  def playerCardCompetition(cardType: String, playerId: String, teamId: String, competitionId: String) =AuthActions.AuthActionTest.async { implicit request =>
    client.competitions.map(PA.filterCompetitions).flatMap { competitions =>
      competitions.find(_.competitionId == competitionId).fold(Future.successful(NoCache(NotFound(views.html.football.error(s"Competition $competitionId not found"))))) { competition =>
        for {
          playerProfile <- client.playerProfile(playerId)
          playerStats <- client.playerStats(playerId, competition.startDate, LocalDate.now(), teamId, competitionId)
          playerAppearances <- client.appearances(playerId, competition.startDate, LocalDate.now(), teamId, competitionId)
        } yield {
          val result = renderPlayerCard(cardType, playerId, playerProfile, playerStats, playerAppearances)
          Cors(NoCache(result))
        }
      }
    }
  }

  def playerCardDate(cardType: String, playerId: String, teamId: String, startDateStr: String) =AuthActions.AuthActionTest.async { implicit request =>
    val startDate = LocalDate.parse(startDateStr, DateTimeFormat.forPattern("yyyyMMdd"))
    for {
      playerProfile <- client.playerProfile(playerId)
      playerStats <- client.playerStats(playerId, startDate, LocalDate.now(), teamId)
      playerAppearances <- client.appearances(playerId, startDate, LocalDate.now(), teamId)
    } yield {
      val result = renderPlayerCard(cardType, playerId, playerProfile, playerStats, playerAppearances)
      Cors(NoCache(result))
    }
  }

  private def renderPlayerCard(cardType: String, playerId: String, playerProfile: PlayerProfile, playerStats: StatsSummary, playerAppearances: PlayerAppearances) = {
    cardType match {
      case "attack" => Ok(views.html.football.player.cards.attack(playerId, playerProfile, playerStats, playerAppearances))
      case "assist" => Ok(views.html.football.player.cards.assist(playerId, playerProfile, playerStats, playerAppearances))
      case "discipline" => Ok(views.html.football.player.cards.discipline(playerId, playerProfile, playerStats, playerAppearances))
      case "defence" => Ok(views.html.football.player.cards.defence(playerId, playerProfile, playerStats, playerAppearances))
      case "goalkeeper" => Ok(views.html.football.player.cards.goalkeeper(playerId, playerProfile, playerStats, playerAppearances))
      case _ => NotFound(views.html.football.error("Unknown card type"))
    }
  }

  def squad(teamId: String) =AuthActions.AuthActionTest.async { implicit request =>
    for {
      squad <- client.squad(teamId)
    } yield {
      val responseJson = JsObject(Seq("players" -> JsArray(
        squad.map { squadMember =>
          JsObject(Seq(
            "label" -> JsString(squadMember.name),
            "value" -> JsString(squadMember.playerId)
          ))
        }
      )))
      Cached(600)(JsonComponent(responseJson))
    }
  }
}
