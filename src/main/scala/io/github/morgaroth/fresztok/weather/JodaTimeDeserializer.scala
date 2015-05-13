package io.github.morgaroth.fresztok.weather

import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import spray.http.Uri.Path
import spray.httpx.unmarshalling.{FromStringDeserializer, Deserialized, MalformedContent}
import spray.routing.PathMatcher.{Unmatched, Matched}
import spray.routing._

object JodaTimeDeserializer extends FromStringDeserializer[DateTime] {
  val formatter = ISODateTimeFormat.dateOptionalTimeParser()

  override def apply(v1: String): Deserialized[DateTime] =
    try {
      Right(formatter.parseDateTime(v1))
    } catch {
      case t: Throwable => Left(MalformedContent("no ISODate", t))
    }

}