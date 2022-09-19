package upandrunning

import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http.ServerBinding
import com.typesafe.config.ConfigFactory
import upandrunning.service.{BoxOfficeService, RequestTimeout}
import akka.http.scaladsl.Http

import scala.concurrent.{ExecutionContextExecutor, Future}

object Main extends App with RequestTimeout{

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContextExecutor = system.dispatcher



  val boxOfficeService = new BoxOfficeService(system, requestTimeout(config))

  val bindingFuture: Future[ServerBinding] =
    Http().newServerAt(host, port).bind(boxOfficeService.routes)

  val log = Logging(system.eventStream, "go-ticks")

  bindingFuture.map{serverBinding =>
    log.info(s"BoxOfficeService bount to ${serverBinding.localAddress}")
  }.recoverWith({
    case ex: Exception =>
      log.error(ex, "Failed to bind to {}:{}!", host, port)
      system.terminate()
  })


}

