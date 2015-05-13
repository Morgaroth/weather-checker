package io.github.morgaroth.fresztok.weather

import akka.actor.ActorSystem
import io.github.morgaroth.fresztok.weather.actors.WeatherActor
import io.github.morgaroth.fresztok.weather.services.WeatherService
import spray.httpx.SprayJsonSupport
import spray.json.DefaultJsonProtocol
import spray.routing._

trait Backend {
  val system = ActorSystem("LOCAL")
  val weatherChecker = system actorOf(WeatherActor.props(), "Waether_Actor")
}

trait WebApi extends Directives with SprayJsonSupport with DefaultJsonProtocol {
  this: Backend =>

  val weatherService = new WeatherService(weatherChecker)(system)

  //@formatter:off
  val routes =
    (pathEndOrSingleSlash & get) {
      complete("Hello from Weather Checker application")
    } ~
    pathPrefix("weather") {
      weatherService.route
    } ~
    pathPrefix("list") {
      (pathEndOrSingleSlash & get) {
        complete(List("fdsfsd","fdsfsd","fdsfsd"))
      }
    }
  //@formatter:on
}
