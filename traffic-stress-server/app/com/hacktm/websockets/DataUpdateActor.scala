package com.hacktm.websockets

import javax.inject.Inject

import akka.actor.{Actor, ActorRef, ActorSystem, PoisonPill, Props}
import play.api.libs.json.Json

/**
  * Created by darkg on 27-May-17.
  */

object DataUpdateActor {
  def props(manager: ActorRef, out: ActorRef, id: Int) = Props(new DataUpdateActor(manager, out, id))

}

class DataUpdateActor (manager: ActorRef, out: ActorRef, id: Int) extends Actor {
  manager ! TrackWebsocket(id, self)

  def receive: PartialFunction[Any, Unit] = {
    case msg: String =>
      out ! ("I received the message " + msg)
    case NotifyData(userId, data) =>
      out ! Json.toJson(data)
  }

  def close: Unit = {
    self ! PoisonPill
  }
}
