import io.circe.parser._
import org.scalatest.freespec.AnyFreeSpec

import java.io.ByteArrayInputStream
import java.time.Instant

/** Tests of the parser. */
class ParserSpec extends AnyFreeSpec {
  "An event JSON string" - {
    val eventType = "some type"
    val data = "123"

    "when valid" - {
      val timestamp = Instant.now().getEpochSecond
      val json = s"""{"event_type":"$eventType","data":"$data","timestamp":$timestamp}"""

      "should decode" in {
        val event = decode[Event](json)
        assert(event == Right(Event(EventType(eventType), data, Instant.ofEpochSecond(timestamp))))
      }
    }

    "when incomplete" - {
      val json = s"""{"event_type":"$eventType","data":"$data"}"""

      "should not decode" in {
        val event = decode[Event](json)
        assert(event.isInstanceOf[Left[_,_]])
      }
    }
  }

  "The event parser" - {
    import cats.effect.unsafe.implicits.global

    val parser = WordCounter.parseOutput

    "when given garbage" - {
      val garbage = Array[Byte]('{', ' ', '"', '.', '\b', -28, -64, -32, -91, 'X', 'H', '&', 4, '\n')

      "should skip over it" in {
        val out = parser(ByteArrayInputStream(garbage))
          .compile.toVector.attempt.unsafeRunSync()
        assert(out == Right(Vector.empty[Event]))
      }
    }
  }
}
