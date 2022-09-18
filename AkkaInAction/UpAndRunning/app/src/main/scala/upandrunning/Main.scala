package upandrunning

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import upandrunning.rest.{RequestTimeout, RestApi}

import scala.concurrent.ExecutionContextExecutor

object Main extends App with RequestTimeout{

  val config = ConfigFactory.load()
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system: ActorSystem = ActorSystem()
  implicit val ec: ExecutionContextExecutor = system.dispatcher
  
  val api = new RestApi(system, requestTimeout(config)).routes

}

