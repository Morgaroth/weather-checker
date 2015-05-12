package io.github.morgaroth.fresztok.weather.services

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import io.github.morgaroth.fresztok.weather.actors.OpenWeather
import io.github.morgaroth.fresztok.weather.actors.WeatherActor.CheckWeather
import spray.http.StatusCodes.{InternalServerError, OK, RequestTimeout}
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.routing.Directives

import scala.concurrent.duration._
import scala.util.{Try, Failure, Success}

class WeatherService(worker: ActorRef)(implicit as: ActorSystem) extends Directives with SprayJsonSupport {

  implicit def ex = as.dispatcher

  implicit val tm: Timeout = 10 seconds

  val route =
    (pathEndOrSingleSlash & get) {
      complete("Hello from Weatherc Checker Service!")
    } ~
    path(Segment) { city =>
      pathEndOrSingleSlash {
        get(complete(fetchWeatherFor(city)))
      }
    }


  def fetchWeatherFor(city: String): ToResponseMarshallable = {
    (worker ? CheckWeather(city)).mapTo[Try[OpenWeather]].collect[ToResponseMarshallable] {
      case Success(ow) => OK -> ow
      case Failure(timeout: AskTimeoutException) => RequestTimeout -> "Try in 5 minutes!"
      case Failure(anotherErr) => InternalServerError -> s"${anotherErr.getCause}${anotherErr.getStackTrace.map(_.toString).mkString("\n\t", "\n\t", "\n")}"
    }
  }
}
