package io.github.morgaroth.fresztok.weather.actors

import akka.actor.{ActorLogging, Props, Actor}

import io.github.morgaroth.fresztok.weather.actors.WeatherActor._
import spray.httpx.SprayJsonSupport

import scala.concurrent.Future
import scala.util.{Failure, Success}

object WeatherActor {
  case class CheckWeather(city: String)

  def props() = Props(classOf[WeatherActor])

  val api = "http://api.openweathermap.org/data/2.5/weather"
}

import spray.client.pipelining._

class WeatherActor extends Actor with SprayJsonSupport with ActorLogging {
  implicit val ex = context.system.dispatcher

  import OpenWeather._

  val pipe = sendReceive ~> unmarshal[OpenWeather]

  self ! CheckWeather("London")

  override def receive: Receive = {
    case CheckWeather(city) =>
      val result: Future[OpenWeather] = pipe(Get(s"$api?q=$city"))
      result.onComplete {
        case Success(ow) => log.info(ow.toString)
        case Failure(t) => log.error(t.getMessage)
      }
      sender() ! result
  }
}
