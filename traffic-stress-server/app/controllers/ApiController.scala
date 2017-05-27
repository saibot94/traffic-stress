package controllers

import javax.inject._

import akka.actor.Status
import akka.actor.Status.Failure
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.Materializer
import com.hacktm.dto.CarJsonDTO
import com.hacktm.dto.CarJsonDTO._
import com.hacktm.websockets.{DataUpdateActor, MasterActor, NotifyData}
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams._
import play.api.mvc._
import play.modules.reactivemongo._
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.api.commands.{MultiBulkWriteResult, WriteResult}
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future
import scala.util.Success


@Singleton
class ApiController @Inject()(implicit system: ActorSystem,
                              materializer: Materializer,
                              val reactiveMongoApi: ReactiveMongoApi,
                              configuration: Configuration) extends Controller
  with MongoController with ReactiveMongoComponents {

  val masterActor: ActorRef = system.actorOf(MasterActor.props)
  val userId: Int = configuration.getInt("user.id").getOrElse(3)

  def index = Action { implicit request =>
    Ok(Json.toJson(Map("hello" -> "world")))
  }


  def carDataCollection: Future[BSONCollection] = database.map {
    db => db.collection[BSONCollection](configuration.getString("db.collection").getOrElse("cardata"))
  }


  def socket(id: Int): WebSocket = WebSocket.accept[String, String] { request =>
    println(s"I've got a connection with id: $id")
    ActorFlow.actorRef(out => DataUpdateActor.props(masterActor, out, id))
  }

  def receiveData: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      request.body.validate[List[CarJsonDTO]].map {
        data =>
          carDataCollection.map {
            col =>
              masterActor ! NotifyData(userId, data)
              val bulkDocs = data.map(implicitly[col.ImplicitlyDocumentProducer](_))
              val writeRes: Future[MultiBulkWriteResult] = col.bulkInsert(ordered=false)(bulkDocs :_*)

              writeRes.onComplete { // Dummy callbacks
                case Success(writeResult) =>
                  println(s"successfully inserted document with result")
              }
              Ok("Inserted")
          }
      }.getOrElse(Future.successful {
        BadRequest(Json.toJson(Map("error" -> "invalid json")))
      })
  }

}

