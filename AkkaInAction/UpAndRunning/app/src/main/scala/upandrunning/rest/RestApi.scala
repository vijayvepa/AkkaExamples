package upandrunning.rest

import akka.actor.ActorSystem
import akka.http.scaladsl.Route
import akka.util.Timeout

class RestApi(system: ActorSystem, timeout: Timeout) extends RestRoutes{

}
