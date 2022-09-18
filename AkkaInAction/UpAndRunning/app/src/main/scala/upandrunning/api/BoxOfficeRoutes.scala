package upandrunning.api
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.*
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
trait BoxOfficeRoutes extends BoxOfficeApi with BoxOfficeMarshalling {

  def eventsRoute =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get{
          onSuccess(getEvents()) { events =>
            complete(OK, events)
          }
        }
      }
    }
}
