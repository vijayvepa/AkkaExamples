package upandrunning.service

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import upandrunning.actors.BoxOffice
import upandrunning.api.BoxOfficeRoutes

import scala.concurrent.ExecutionContext

class BoxOfficeService(system: ActorSystem, timeout: Timeout) extends BoxOfficeRoutes {
  implicit val requestTimeout: Timeout = timeout
  implicit def executionContext: ExecutionContext = system.dispatcher

  override def createBoxOffice(): ActorRef = {
    system.actorOf(BoxOffice.props, BoxOffice.name)
  }
}
