package io.github.morgaroth.fresztok.weather

import akka.actor._
import akka.io.IO
import com.typesafe.config.ConfigFactory
import net.ceedubs.ficus.Ficus._
import spray.can.Http
import spray.routing._


object Server extends App with WebApi with Backend {
  val serviceActorProps = Props(new HttpServiceActor {
    override def receive: Actor.Receive = runRoute(routes)
  })

  val rootService = system.actorOf(serviceActorProps)

  val port = ConfigFactory.load().as[Int]("http.port")

  IO(Http)(system) ! Http.Bind(rootService, "0.0.0.0", port = port)
}