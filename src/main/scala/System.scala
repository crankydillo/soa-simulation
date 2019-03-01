package com.example

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._

import akka.actor._
import akka.pattern.ask
import akka.pattern.pipe

object System {

  import Server.timeout
  import Messages._

  object Messages {
    case class Echo(msg: String)
    case class Sleep(sleep: FiniteDuration, block: Boolean)
    case class Req(sleep: FiniteDuration, block: Boolean)

    sealed abstract class Resp(level: Int) {
      val count: Int
    }
    case class SimpleResp(level: Int, resp: String) extends Resp(level) {
      override val count = 1
    }
    case class NestedResp(level: Int, responses: List[Resp]) extends Resp(level) {
      override val count = 1 + responses.map(_.count).sum
    }
  }

  object Service {

    def props(name: String, level: Int) = Props(new Service(name, level))

    def props(name: String, level: Int, children: List[ActorRef]) =
      Props(new Service(name, level, children))
  }

  // I should made blocking and non-blocking classes, but what's done is done.
  class Service(
    name: String,
    val level: Int,
    children: List[ActorRef] = Nil  // want type safe actors? :(:(
  ) extends Actor {

    def indent(s: String) = " " * level + s
   
    /**
     * Process downstream requests synchronously, blocking on each one.
     */
    private var block = true

    implicit val ec: scala.concurrent.ExecutionContext = context.system.dispatcher

    def receive = {
      case Echo(msg) => sender() ! msg
      case Sleep(sleep, block) =>
        val s = sender()
        if (block) s ! SimpleResp(level, Await.result(io(sleep), Server.timeoutDuration))
        else io(sleep) foreach { resp => s ! SimpleResp(level, resp) }
      case r: Req =>
        val s = sender()
        if (children.isEmpty) {
          self ? Sleep(r.sleep, r.block) pipeTo s
        } else {
          println(s"I'm going issuing some child requests from level $level")
          val fn = 
            if (r.block) blocking(s, r) _
            else nonBlocking(s, r) _

          fn { resps => s ! NestedResp(level, resps) }
        }
    }

    private def blocking(
      sender: ActorRef, r: Req
    )(use: List[Resp] => Unit): Unit = {
      val resps = children.map { c =>
        Await.result((c ? r).mapTo[Resp], Server.timeoutDuration)
      }
      use(resps)
    }

    private def nonBlocking(
      sender: ActorRef, r: Req
    )(use: List[Resp] => Unit): Unit = {
      val futs = children.map { c => (c ? r).mapTo[Resp] }
      Future.sequence(futs).foreach { use }
    }

    private def io(sleep: FiniteDuration): Future[String] = {
      println(s"I'm going to do some IO!")
      val p = Promise[String]()
      context.system.scheduler.scheduleOnce(sleep) {
        println(s"It took me $sleep to do some IO!")
        p success name
      }
      p.future
    }
  }
}
