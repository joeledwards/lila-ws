package controllers

import javax.inject._
import play.api.Configuration
import play.api.http.HeaderNames
import play.api.http.websocket.Message
import play.api.Logger
import play.api.mvc._
import scala.concurrent.{ Future, ExecutionContext }

import lila.ws._
import lila.ws.util.Util.{ reqName, flagOf, parseIntOption, reqString, reqInt }

@Singleton
class SocketController @Inject() (
    server: Server,
    config: Configuration,
    mongo: Mongo,
    auth: Auth,
    val controllerComponents: ControllerComponents
)(implicit ec: ExecutionContext) extends BaseController {

  def site(sriStr: String, apiVersion: Int) = LichessWebSocket(sriStr) { (sri, req) =>
    server.connectToSite(req, sri, flagOf(req)) map Right.apply
  }

  def lobby(sriStr: String, apiVersion: Int) = LichessWebSocket(sriStr) { (sri, req) =>
    server.connectToLobby(req, sri, flagOf(req)) map Right.apply
  }

  def simul(id: Simul.ID, sriStr: String, apiVersion: Int) =
    LichessWebSocket(sriStr) { (sri, req) =>
      mongo simulExists id flatMap {
        case false => Future successful Left(NotFound)
        case true => server.connectToSimul(req, Simul(id), sri, getSocketVersion(req)) map Right.apply
      }
    }

  def tournament(id: Simul.ID, sriStr: String, apiVersion: Int) =
    LichessWebSocket(sriStr) { (sri, req) =>
      mongo tourExists id flatMap {
        case false => Future successful Left(NotFound)
        case true => server.connectToTour(req, Tour(id), sri, getSocketVersion(req)) map Right.apply
      }
    }

  def study(id: Simul.ID, sriStr: String, apiVersion: Int) =
    LichessWebSocket(sriStr) { (sri, req) =>
      auth(req, None) flatMap { user =>
        mongo.studyExistsFor(id, user) flatMap {
          case false => Future successful Left(NotFound)
          case true => server.connectToStudy(req, Study(id), user, sri, getSocketVersion(req)) map Right.apply
        }
      }
    }

  def roundWatch(idS: String, color: String, sriStr: String, apiVersion: Int) =
    LichessWebSocket(sriStr) { (sri, req) =>
      val id = Game.Id(idS)
      auth(req, None) flatMap { user =>
        mongo.gameExists(id) flatMap {
          case false => Future successful Left(NotFound)
          case true =>
            val userTv = reqString(req, "userTv") map UserTv.apply
            server.connectToRoundWatch(req, id, user, sri, getSocketVersion(req), userTv) map Right.apply
        }
      }
    }

  def roundPlay(fullId: String, sriStr: String, apiVersion: Int) =
    LichessWebSocket(sriStr) { (sri, req) =>
      val id = Game.FullId(fullId)
      auth(req, None) flatMap { user =>
        mongo.player(id, user) flatMap {
          case None => Future successful Left(NotFound)
          case Some(player) => server.connectToRoundPlay(req, id, player, user, sri, getSocketVersion(req)) map Right.apply
        }
      }
    }

  def api: WebSocket = WebSocket { req =>
    server.connectToSite(req, Sri.random, Some(Flag.api)) map Right.apply
  }

  private type Response = Future[Either[Result, akka.stream.scaladsl.Flow[Message, Message, _]]]

  private val csrfDomain = config.get[String]("csrf.origin")
  private val appOrigins = Set(
    "ionic://localhost", // ios
    "capacitor://localhost", // capacitor (ios next)
    "http://localhost", // android
    "http://localhost:8080", // local dev
    "file://"
  )

  private def CsrfCheck(req: RequestHeader)(f: => Response): Response =
    req.headers get HeaderNames.ORIGIN match {
      case None => f // for exotic clients and acid ape chess
      case Some(origin) if origin == csrfDomain || appOrigins(origin) => f
      case Some(origin) =>
        logger.info(s"""CSRF origin: "$origin" ${reqName(req)}""")
        Future successful Left(Forbidden("Cross origin request forbidden"))
    }

  private def ValidSri(str: String, req: RequestHeader)(f: Sri => Response): Response =
    Sri from str match {
      case Some(validSri) => f(validSri)
      case None =>
        logger.info(s"""Invalid sri: "$str" ${reqName(req)}""")
        Future successful Left(BadRequest("Invalid sri"))
    }

  private def LichessWebSocket(sriStr: String)(f: (Sri, RequestHeader) => Response): WebSocket =
    WebSocket { req =>
      CsrfCheck(req) {
        ValidSri(sriStr, req) { sri =>
          f(sri, req)
        }
      }
    }

  protected def getSocketVersion(req: RequestHeader): Option[SocketVersion] =
    reqInt(req, "v") map SocketVersion.apply

  private val logger = Logger("Controller")
}
