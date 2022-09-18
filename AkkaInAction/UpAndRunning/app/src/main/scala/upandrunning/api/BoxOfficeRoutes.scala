package upandrunning.api

import akka.actor.*
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport.*
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.*
import akka.http.scaladsl.server.Directives.*
import akka.pattern.ask
import akka.util.Timeout
import upandrunning.actors.BoxOffice

trait BoxOfficeRoutes extends BoxOfficeApi with BoxOfficeMarshalling {

  import StatusCodes.*

  def eventRoute: Route =
    pathPrefix("events" / Segment) { event =>
      pathEndOrSingleSlash {
        post {
          // POST /events/:event
          entity(as[EventDescription]) { ed =>
            onSuccess(createEvent(event, ed.tickets)) {
              case BoxOffice.EventCreated(event) => complete(Created, event)
              case BoxOffice.EventExists =>
                val err = Error(s"$event event exists already.")
                complete(BadRequest, err)
            }
          }
        } ~
          get {
            // GET /events/:event
            onSuccess(getEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          } ~
          delete {
            // DELETE /events/:event
            onSuccess(cancelEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          }
      }
    }

  def eventsRoute: Route =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get {
          onSuccess(getEvents()) { events =>
            complete(OK, events)
          }
        }
      }
    }


}
