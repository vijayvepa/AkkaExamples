package upandrunning.api

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import upandrunning.actors.BoxOffice

trait BoxOfficeRoutes extends BoxOfficeApi with BoxOfficeMarshalling {

  import StatusCodes._

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
        Directives.get {
          onSuccess(getEvents()) { events =>
            complete(OK, events)
          }
        }
      }
    }

  def routes : Route = eventsRoute ~ eventRoute

}
