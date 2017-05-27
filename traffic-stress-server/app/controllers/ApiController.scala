package controllers

import javax.inject._

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import com.hacktm.websockets.{DataUpdateActor, MasterActor, NotifyData, TrackWebsocket}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams._
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class ApiController @Inject()(implicit system: ActorSystem, materializer: Materializer) extends Controller {

  val masterActor: ActorRef = system.actorOf(MasterActor.props)

  def index = Action { implicit request =>
    masterActor ! NotifyData(3, Json.toJson(3))
    Ok(Json.toJson(Map("hello" -> "world")))
  }

  def socket(id: Int): WebSocket = WebSocket.accept[String, String] { request =>
    println(s"I've got a connection with id: $id")
    ActorFlow.actorRef(out => DataUpdateActor.props(masterActor, out, id))
  }
}
