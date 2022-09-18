package upandrunning.rest

import akka.http.scaladsl.server.Directives.{get, onSuccess, pathEndOrSingleSlash, pathPrefix}
import akka.http.scaladsl.server.Route
import upandrunning.api.BoxOfficeApi

trait RestRoutes extends BoxOfficeApi with EventMarshalling {
  def routes: Route = eventsRoute


  def eventsRoute = pathPrefix("event") {
    pathEndOrSingleSlash {
      get {
        onSuccess(getEvents()) {

        }
      }
    }
  }

}
