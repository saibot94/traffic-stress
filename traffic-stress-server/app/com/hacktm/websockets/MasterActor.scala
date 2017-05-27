package com.hacktm.websockets

import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.JsValue

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by darkg on 27-May-17.
  */

case class NotifyData(id: Int, data: JsValue)
case class TrackWebsocket(id: Int, actor: ActorRef)

object MasterActor {
  val props = Props(new MasterActor)
}
class MasterActor extends Actor {

  var registeredActors: mutable.HashMap[Int, ListBuffer[ActorRef]] = mutable.HashMap()

  override def receive: Receive = {
    case NotifyData(id, data) =>
      println(s"got id and data: $id, $data")
      registeredActors.get(id) match {
        case None =>
          println("WARN: Could not find any socket to send data to")
        case Some(lb) =>
          lb.foreach {
            actor =>
              actor ! NotifyData(id, data)
          }
      }

    case TrackWebsocket(id, actorRef) =>
      registeredActors.get(id) match {
        case None =>
          val lb = new ListBuffer[ActorRef]()
          lb += actorRef
          registeredActors += id -> lb
        case Some(lb) =>
          lb += actorRef
      }


  }
}
