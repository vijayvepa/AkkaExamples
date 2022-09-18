package upandrunning.api

import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask
import upandrunning.actors.TicketSeller

import scala.concurrent.{ExecutionContext, Future}

//noinspection AccessorLikeMethodIsEmptyParen
trait BoxOfficeApi {
  import upandrunning.actors.BoxOffice._

  def createBoxOffice(): ActorRef
  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val boxOffice: ActorRef = createBoxOffice()

  def createEvent(event: String, numberOfTickets: Int): Future[EventResponse] =
    boxOffice.ask(CreateEvent(event, numberOfTickets)).mapTo[EventResponse]

  def getEvents(): Future[Events] =
    boxOffice.ask(GetEvents).mapTo[Events]

  def getEvent(event: String): Future[Option[Event]] =
    boxOffice.ask(GetEvent(event)).mapTo[Option[Event]]

  def cancelEvent(event: String): Future[Option[Event]] =
    boxOffice.ask(CancelEvent(event)).mapTo[Option[Event]]

  def requestTickets(event: String, tickets: Int): Future[TicketSeller.Tickets] =
    boxOffice.ask(GetTickets(event, tickets)).mapTo[TicketSeller.Tickets]

}
