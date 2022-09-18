package upandrunning.actors

import akka.actor.{Actor, Props, PoisonPill}
import upandrunning.actors.BoxOffice.Event
import upandrunning.actors.TicketSeller.{GetEvent, Ticket}


object TicketSeller {

  def props(event:String): Props = Props(new TicketSeller(event))

  case class Add(tickets: Vector[Ticket])
  case class Buy(tickets: Int)

  case class Ticket(id: Int)
  case class Tickets(event: String, entries: Vector[Ticket] = Vector.empty[Ticket])
  case object GetEvent

  case object Cancel
}



class TicketSeller(event:String) extends Actor{

  import TicketSeller.{GetEvent, Cancel}

  val tickets = Vector.empty[Ticket]

  def receive: Receive = {
    case GetEvent => sender() ! Some(Event(event, tickets.size))
    case Cancel =>
      sender() ! Some(BoxOffice.Event(event, tickets.size))
      self! PoisonPill
  }
}
