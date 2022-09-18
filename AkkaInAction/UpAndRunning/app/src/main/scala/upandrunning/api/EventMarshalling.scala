package upandrunning.api

import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import upandrunning.actors.TicketSeller

case class EventDescription(tickets: Int)  {
  require(tickets > 0)
}

case class TicketRequest(tickets: Int){
  require(tickets > 0)
}

case class Error(message: String)

trait EventMarshalling extends DefaultJsonProtocol{

  import upandrunning.actors.BoxOffice._

  implicit val eventDescriptionFormat: RootJsonFormat[EventDescription] = jsonFormat1(EventDescription)
  implicit val eventFormat: RootJsonFormat[Event] = jsonFormat2(Event)
  implicit val eventsFormat: RootJsonFormat[Events] = jsonFormat1(Events)
  implicit val ticketRequestFormat: RootJsonFormat[TicketSeller.Ticket] = jsonFormat1(TicketSeller.Ticket)
  implicit val ticketFormat: RootJsonFormat[TicketSeller.Ticket] = jsonFormat1(TicketSeller.Ticket)
  implicit val errorFormat: RootJsonFormat[Error] = jsonFormat1(Error)


}
