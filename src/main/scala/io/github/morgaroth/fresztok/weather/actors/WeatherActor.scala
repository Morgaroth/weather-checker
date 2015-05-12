package io.github.morgaroth.fresztok.weather.actors

import akka.actor.{Actor, ActorLogging, Props}
import io.github.morgaroth.fresztok.weather.actors.WeatherActor._
import spray.httpx.SprayJsonSupport

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

object WeatherActor {

  case class CheckWeather(city: String)

  def props() = Props(classOf[WeatherActor])

  val api = "http://api.openweathermap.org/data/2.5/weather"
}

import spray.client.pipelining._

class WeatherActor extends Actor with SprayJsonSupport with ActorLogging {
  implicit val ex = context.system.dispatcher

  val pipe = sendReceive ~> unmarshal[OpenWeather]

  override def receive: Receive = {
    case CheckWeather(city) if city.startsWith("t") =>
      log.info(s"Got query for $city but had a sulk!")

    case CheckWeather(city) if city.startsWith("a") =>
      log.info(s"Got query for $city but throw exception")
      sender() ! Future(Try(throw new RuntimeException).failed.get)

    case CheckWeather(city) =>
      log.info(s"Got query for $city, querying openweather api.")
      val result = pipe(Get(s"$api?q=$city"))
      result.onComplete {
        case Success(ow) => log.info(ow.toString)
        case Failure(t) => log.error(t.getMessage)
      }
      val s  =sender()
      result.onComplete(s ! _)
  }
}
