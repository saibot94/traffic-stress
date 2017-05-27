package com.hacktm.dto

import play.api.libs.json._

/**
  * Created by darkg on 27-May-17.
  */
case class CarJsonDTO(speed: Double,
                      pedal: Double,
                      yawrate: Double,
                      rpm: Double,
                      latitude: Double,
                      longitude: Double,
                      trafficheight: List[Double],
                      trafficlength: List[Double],
                      trafficx: List[Double],
                      trafficy: List[Double],
                      drivetime: Long,
                      driver: Long,
                      recordingId: Long
                     )


object CarJsonDTO {
  implicit val readsCarJson: Reads[CarJsonDTO] = Json.reads[CarJsonDTO]
  implicit val writesCarJson: Writes[CarJsonDTO] = Json.writes[CarJsonDTO]
}