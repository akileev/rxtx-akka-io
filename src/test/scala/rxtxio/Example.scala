package rxtxio

import akka.actor.{Actor, ActorRef, ActorSystem, Props, Stash}
import akka.io.IO
import akka.util.ByteString
import rxtxio.Serial._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class Example(port: String) extends Actor with Stash {
  import context.system

  override def preStart = {
    IO(Serial) ! Open(self, port, 9600)
  }

  override def postStop = {
    println("Stopped")
    val whenTerminated = system.terminate()
    Await.result(whenTerminated, Duration.Inf)
  }

  override def receive = {
    case Opened(operator, _) =>
      println("Connected to port")
      context become open(operator)
      unstashAll()
    case CommandFailed(_, error) =>
      println(s"Could not connect to port: $error")
      context stop self
    case other => stash()
  }

  def open(operator: ActorRef): Receive = {
    case "close" =>
      println("Closing")
      operator ! Close

    case s: String => //external input
      operator ! Write(ByteString(s))

    case Received(data) =>
      print("Received data from serial port: ")
      println(data.decodeString("UTF-8"))

    case Closed =>
      println("Serial port closed")
      context stop self
  }
}

object Example extends App {
  val port = "/dev/cu.usbserial-A6008hNp"

  val system = ActorSystem("Example")
  val actor = system.actorOf(Props(new Example(port)), "e1")

  //The following lines are just for the sake of an example, never program in
  // akka like that.. normally your logic would be embedded in the example
  // actor
  actor ! "Hi from the computer"
  Thread.sleep(2000)
  actor ! "close"

  val whenTerminated = system.terminate()
  Await.result(whenTerminated, Duration.Inf)
}