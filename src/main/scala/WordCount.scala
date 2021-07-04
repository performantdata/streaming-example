import java.time.Instant

/** Word count report.  */
case class WordCount(startTime: Instant, endTime: Instant, counts: Map[EventType, Long])
