package upandrunning.actors

import akka.actor._
import akka.util.Timeout

object BoxOffice {

  val name = "boxOffice"

  case class Event(name: String, tickets: Int)

  case class Events(vents: Vector[Event])

  case class GetEvent(name: String)
  
  case object GetEvents
}

class BoxOffice(implicit  timeout: Timeout) extends Actor {
  import BoxOffice._
  import context._
  
  def receive = {

    case GetEvent(event) =>
      def notFound() = sender() ! None
      def getEvent(child: ActorRef) = child forward TicketSeller.GetEvent
      context.child(event).fold(notFound())(getEvent)

      
      
  }
  
}