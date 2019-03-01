package com.example

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.StdIn

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout

import de.heikoseeberger.akkahttpjson4s.Json4sSupport
import org.json4s.{DefaultFormats, FieldSerializer, native}

object Server {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val timeoutDuration = 20.minutes
  implicit val timeout: Timeout = timeoutDuration
  implicit val serialization = native.Serialization
  implicit val formats = DefaultFormats + FieldSerializer[System.Messages.Resp]()

  def mkSystem(depth: Int, childrenPerNode: Int): ActorRef = {

    def mkChildren(currDepth: Int): List[ActorRef] = {
      if (currDepth == depth+1) {
        Nil
      } else {
        (0 to childrenPerNode-1).map { i =>
          system.actorOf(System.Service.props(s"svc-$i", currDepth, mkChildren(currDepth+1)))
        }.toList
      }
    }

    system.actorOf(System.Service.props("edge", 0, mkChildren(1)))
  }

  def main(args: Array[String]) {

    val (port, depth, childrenPerNode) = args.toList match {
      case p :: d :: cs :: Nil => (p.toInt, d.toInt, cs.toInt)
      case _ =>
        println("Usage: <port> <depth> <children-per-node>")
        sys.exit(1)
    }

    val edge = mkSystem(depth, childrenPerNode)

    import System.Messages

    def dur(s: String) = Duration(s).asInstanceOf[FiniteDuration]

    import Json4sSupport._


    val route = {
      path("hello") {
        get {
          complete(
            (edge ? Messages.Echo("hello")).mapTo[String]
          )
        }
      } ~ path(Segment / "sleep" / Segment) { (blockInd, str) =>
        val block = blockInd == "b"
        complete(
          (edge ? Messages.Sleep(dur(str), block)).mapTo[Messages.Resp]
        )
      } ~ path(Segment / "req" / Segment) { (blockInd, str) =>
        val block = blockInd == "b"
        complete(
          (edge ? Messages.Req(dur(str), block)).mapTo[Messages.Resp]
        )
      }
    }

    Http().bindAndHandle(route, "0.0.0.0", port)

    println(s"Server online at http://localhost:$port/")

    Await.result(system.whenTerminated, Duration.Inf)
  }
}
