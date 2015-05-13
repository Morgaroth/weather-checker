package io.github.morgaroth.fresztok.weather.services

import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.pattern.{AskTimeoutException, ask}
import akka.util.Timeout
import io.github.morgaroth.fresztok.weather.JodaTimeDeserializer
import io.github.morgaroth.fresztok.weather.actors.OpenWeather
import io.github.morgaroth.fresztok.weather.actors.WeatherActor.CheckWeather
import org.joda.time.DateTime
import shapeless.HNil
import spray.client.pipelining._
import spray.http.HttpEncodings.gzip
import spray.http.StatusCodes
import spray.http.StatusCodes._
import spray.httpx.SprayJsonSupport
import spray.httpx.marshalling.ToResponseMarshallable
import spray.json.DefaultJsonProtocol
import spray.routing.{Directive, Directives}
import us.bleibinha.spray.json.macros.lazyy.json

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

@json case class TODO(id: String, value: String)

class WeatherService(worker: ActorRef)(implicit as: ActorSystem) extends Directives with SprayJsonSupport with DefaultJsonProtocol {

  val log = Logging.apply(as, getClass)

  implicit def ex = as.dispatcher

  implicit val tm: Timeout = 10 seconds

  var cache = Map.empty[String, (DateTime, OpenWeather)]
  var todos = Map.empty[String, TODO]

  val pipe = sendReceive ~> unmarshal[OpenWeather]


  val logRequestResponseOwn: Directive[HNil] = mapRequest(req => {
    log.warning(req.toString)
    req
  }) & mapHttpResponse(res => {
    log.error(res.toString)
    res
  })

  val route =
    logRequestResponseOwn {
      (pathEndOrSingleSlash & get) {
        complete("Hello from Weatherc Checker Service!")
      } ~
        (responseEncodingAccepted(gzip) & compressResponse()) {
          parameters('newerThan.as(JodaTimeDeserializer) ?, 'newerThanTS.as[Long] ?, "value" ? "majtki", 'page ? 0, 'pageSize ? 100, 'format.as[Boolean].?) {
            (nt: Option[DateTime], ntUX: Option[Long], r: String, t: Int, y: Int, u: Option[Boolean]) =>
              path(Segment) { city =>
                pathEndOrSingleSlash {
                  get(complete {
                    val a: Option[DateTime] = nt
                    val aUX: Option[Long] = ntUX
                    val timeBoundary: DateTime = a orElse aUX.map(new DateTime(_)) getOrElse new DateTime(0)
                    fetchWeatherFor(city, timeBoundary)
                  })
                }
              }
          } ~
            pathPrefix("checkHeader" / Segment / IntNumber) { (name, value) =>
              headerValueByName(name) { h =>
                pathEndOrSingleSlash {
                  get(complete(
                    if (h.toInt == value) OK -> s"receiverd $h"
                    else StatusCodes.BadRequest -> s"invalid"
                  ))
                }
              }
            }
        } ~
        pathPrefix("todos") {
          pathEndOrSingleSlash {
            get(complete(todos.values.toList)) ~
              post(handleWith {
                (newTodo: TODO) =>
                  todos += newTodo.id -> newTodo
                  StatusCodes.Created -> newTodo
              })
          } ~
            pathPrefix(Segment) { id =>
              pathEndOrSingleSlash {
                get(complete(todos.get(id))) ~
                  delete(complete {
                    todos -= id
                    NoContent -> ""
                  })

              }
            }
        }
    }

  val api = "http://api.openweathermap.org/data/2.5/weather"

  def fetchWeatherFor(city: String, fresh: DateTime): Future[ToResponseMarshallable] = {
    cache.get(city) match {
      case Some((when, what)) if when isAfter fresh =>
        log.info("returning data from cache")
        Future[ToResponseMarshallable](OK -> what)
      case _ =>
        log.info(s"cache is too old or missing for $city")
        pipe(Get(s"$api?q=$city")).map[ToResponseMarshallable] { ow =>
          cache += city ->(DateTime.now(), ow)
          OK -> ow
        } recover[ToResponseMarshallable] {
          case timeout: AskTimeoutException => RequestTimeout -> "Try in 5 minutes!"
          case anotherErr => InternalServerError -> s"${anotherErr.getCause}${anotherErr.getStackTrace.map(_.toString).mkString("\n\t", "\n\t", "\n")}"
        }
    }
  }
}
