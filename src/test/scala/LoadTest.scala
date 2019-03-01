package computerdatabase

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class LoadTest extends Simulation {

  val httpProtocol = http
    .baseUrl("http://localhost:8080")

  val sleep = "3s"
  val concurrentUsers = 500

  val scn = 
    scenario("Load") // A scenario is a chain of requests and pauses
    .exec(http("Non-blocking")
      .get(s"/nb/sleep/$sleep"))
    .pause(3)
    .exec(http("Blocking")
      .get(s"/b/sleep/$sleep"))

  setUp(scn.inject(atOnceUsers(concurrentUsers)).protocols(httpProtocol))
}
