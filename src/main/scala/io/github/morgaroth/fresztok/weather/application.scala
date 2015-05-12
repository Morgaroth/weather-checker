package io.github.morgaroth.fresztok.weather

import akka.actor.ActorSystem
import io.github.morgaroth.fresztok.weather.actors.WeatherActor
import spray.routing._

trait WebApi extends Directives {
  val routes: Route = get {
    complete("Hello from Weather Checker application")
  }
}

trait Backend {
  val system = ActorSystem("LOCAL")
  system actorOf(WeatherActor.props(), "Waether_Actor")
}
