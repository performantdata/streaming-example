import io.circe.{Decoder, HCursor, KeyEncoder}

import java.time.Instant
import scala.util.Try

implicit val instantDecoder: Decoder[Instant] = Decoder.decodeLong.emapTry { s => Try(Instant.ofEpochSecond(s)) }

/** An "event type".
 *
 * @param t the event type's label
 */
case class EventType(t: String) extends AnyVal
object EventType {
  implicit val eventTypeDecoder: Decoder[EventType] = Decoder.decodeString.emapTry { s => Try(EventType(s)) }
  implicit val eventTypeKeyEncoder: KeyEncoder[EventType] = new KeyEncoder[EventType] {
    override def apply(eventType: EventType): String = eventType.t
  }
}

/** An event from our input generator. */
case class Event(eventType: EventType, data: String, timestamp: Instant)
object Event {
  /** Special decoder to handle the snake case member name.
   *  (`circe-generic-extras` isn't available for Scala 3.)
   */
  implicit val eventDecoder: Decoder[Event] =
    Decoder.forProduct3("event_type", "data", "timestamp")((t, d, ts) => Event(t, d, ts))
}
