package upandrunning.actors

import akka.actor._
import upandrunning.actors.BoxOffice.Event
import upandrunning.actors.TicketSeller.{GetEvent, Ticket}

object TicketSeller {
  case class Ticket(id: Int)
  case object GetEvent
}

class TicketSeller(event:String) extends Actor{

  val tickets = Vector.empty[Ticket]

  def receive = {
    case GetEvent => sender() ! Some(Event(event, tickets.size))
  }
}
