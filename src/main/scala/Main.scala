import cats.effect.{ExitCode, IO, IOApp}
import com.typesafe.scalalogging.LazyLogging
import io.circe.generic.auto._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.middleware.Logger
import org.http4s.{HttpApp, HttpRoutes}

import scala.concurrent.ExecutionContext.global

/** Our HTTP server. */
object Main extends IOApp with LazyLogging {
  import org.http4s.implicits._
  private[this] val dsl = new Http4sDsl[IO]{}
  import dsl._

  val routes: HttpApp[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "words" => WordCounter.lastCounts.get() match {
      case null => NoContent()
      case m    => Ok(m)
    }
  }.orNotFound

  private[this] val httpApp = Logger.httpApp(logHeaders = true, logBody = true)(routes)

  def run(args: List[String]): IO[ExitCode] = {
    startGeneratorProcess()

    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "0.0.0.0")
      .withHttpApp(httpApp)
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }

  /** Start the process that generates our input. */
  private[this]
  def startGeneratorProcess(): Unit = {
    import cats.effect.unsafe.implicits.global

    import scala.sys.process._

    /* I don't like running the IO like this, but I don't see how else yet, since this function is running in its
     * own thread. */
    //FIXME The thread isn't reporting its exceptions.
    Seq("blackbox.amd64") run
      new ProcessIO(_.close(), in => WordCounter.collectOutput(in).unsafeRunSync(), _.close(), true)

    logger.info("Black box process started.")
  }
}
