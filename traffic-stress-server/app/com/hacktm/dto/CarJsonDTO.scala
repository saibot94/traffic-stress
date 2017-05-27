package com.hacktm.dto

import play.api.libs.json._
import reactivemongo.bson.{BSONDocument, BSONDocumentWriter}

/**
  * Created by darkg on 27-May-17.
  */
case class CarJsonDTO(speed: Double,
                      pedal: Double,
                      yawRate: Double,
                      rpm: Double,
                      latitude: Double,
                      longitude: Double,
                      trafficHeading: List[Double],
                      trafficXSpeed: List[Double],
                      trafficYSpeed: List[Double],
                      trafficLength: List[Double],
                      trafficXPos: List[Double],
                      trafficYPos: List[Double],
                      timestamp: Long,
                      driver: Long,
                      recordingId: Long
                     )


object CarJsonDTO {
  implicit val readsCarJson: Reads[CarJsonDTO] = Json.reads[CarJsonDTO]
  implicit val writesCarJson: Writes[CarJsonDTO] = Json.writes[CarJsonDTO]


  implicit object PersonWriter extends BSONDocumentWriter[CarJsonDTO] {
    def write(carData: CarJsonDTO): BSONDocument =
      BSONDocument(
        "pedal" -> carData.pedal,
        "speed" -> carData.speed,
        "driver" -> carData.driver,
        "latitude" -> carData.latitude,
        "longitude" -> carData.longitude,
        "recordingId" -> carData.recordingId,
        "rpm" -> carData.rpm,
        "timestamp" -> carData.timestamp,
        "trafficHeading" -> carData.trafficHeading,
        "trafficLength" -> carData.trafficLength,
        "trafficXPos" -> carData.trafficXPos,
        "trafficYPos" -> carData.trafficYPos,
        "trafficYSpeed" -> carData.trafficYSpeed,
        "trafficXSpeed" -> carData.trafficXSpeed
      )
  }
}

