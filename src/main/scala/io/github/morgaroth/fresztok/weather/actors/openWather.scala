package io.github.morgaroth.fresztok.weather.actors

import us.bleibinha.spray.json.macros.json
import spray.json._
import spray.json.DefaultJsonProtocol._

@json case class Coordinates(lon: Double, lat: Double)

case class Main(temp: Double)
object Main extends DefaultJsonProtocol {
  implicit val rootFormat = jsonFormat(Main.apply _, "temp")
}

@json case class OpenWeather(name: String, id: Long, coord: Coordinates, main: Main)
