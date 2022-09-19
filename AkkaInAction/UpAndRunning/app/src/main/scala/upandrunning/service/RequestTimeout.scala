package upandrunning.service

import akka.util.Timeout
import com.typesafe.config.Config

trait RequestTimeout {
  import scala.concurrent.duration._
  
  def requestTimeout(config: Config): Timeout = {
    val timeout = config.getString("akka.http.request-timeout")
    val duration = Duration(timeout)
    FiniteDuration(duration.length, duration.unit)
  } 

}
