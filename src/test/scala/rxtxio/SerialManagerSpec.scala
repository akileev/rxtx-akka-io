package rxtxio

import akka.actor.ActorSystem
import akka.io._
import akka.testkit._
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike}
import rxtxio.Serial._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SerialManagerSpec extends TestKit(ActorSystem("SerialManagerSpec"))
  with FunSuiteLike
  with BeforeAndAfterAll
  with ImplicitSender {

  override def afterAll = {
    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)
  }

  test("list ports") {
    IO(Serial) ! ListPorts
    val Ports(ports) = expectMsgType[Ports]
    println("Found serial ports: " + ports.mkString(", "))
  }
}