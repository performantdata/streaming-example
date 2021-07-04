import cats.effect.IO
import com.typesafe.scalalogging.LazyLogging
import fs2.{INothing, Stream}

import java.io.InputStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

object WordCounter extends LazyLogging {
  /** Duration of the sampling intervals, in seconds. */
  val secondsPerInterval: Int = 30

  /** The counts from the last-completed time window. `null` if no window has completed yet. */
  /* This could probably just be a `volatile`, since reference updates are probably atomic on 64-bit systems,
   * but the overhead is tiny. */
  val lastCounts: AtomicReference[WordCount] = new AtomicReference[WordCount]

  /** Regex that matches whitespace, for word counting. */
  private[this] val whitespaceRegex = """\p{IsWhite_Space}""".r

  /** Return an effect that updates the counts from the given output of the JSON generator. */
  def collectOutput(in: InputStream): IO[Unit] = {
    /*TODO A downside to groupAdjacentBy() is that it requires a different group to begin before it emits the last one.
     * That next group may never arrive, or it may be greatly delayed.  Better to trigger off the clock. */

    parseOutput(in)
      .groupAdjacentBy(ev => ev.timestamp.getEpochSecond / secondsPerInterval)
      .map((t, ch) => t -> ch.toList                          // count the words by event type
        .groupMap(_.eventType)(ev => whitespaceRegex.split(ev.data).length.toLong)
        .map((k,v) => k -> v.sum)
      )
      .map((t,m) => WordCount(                                // create the reporting object
        startTime = Instant.ofEpochSecond(t * secondsPerInterval),
        endTime   = Instant.ofEpochSecond((t + 1) * secondsPerInterval),
        counts    = m
      ))
      .foreach(m => IO { lastCounts.set(m) })                 // ship it
      .compile
      .drain
  }

  /** Return a stream of events from the given output of the JSON generator.
   *
   * This function is separated for unit testing.
   */
  def parseOutput(in: InputStream): Stream[IO, Event] = {
    import fs2.io.readInputStream
    import fs2.text
    import io.circe.parser.decode

    readInputStream(IO(in), 0x1000)
      .through(text.utf8Decode)        // input is JSON, so presumably UTF-8
      .through(text.lines)             // newlines seem to delimit the events
      .map(decode[Event])
      .map { o => o.left.foreach(x => logger.info(s"decoding failure: $x")); o }
      .collect { case Right(e) => e }  // toss any unparseable lines
  }
}
