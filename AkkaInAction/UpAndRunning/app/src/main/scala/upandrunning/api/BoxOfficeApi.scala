package upandrunning.api


import akka.actor.ActorRef
import scala.concurrent.Future

trait BoxOfficeApi {

  import upandrunning.actors.BoxOffice._

  def createBoxOffice(): ActorRef

  lazy val boxOffice: ActorRef = createBoxOffice()

  def getEvents(): Future[Events] = boxOffice.ask(GetEvents).mapTo(Events)
}
