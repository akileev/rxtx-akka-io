package rxtxio

import akka.actor._
import akka.util.ByteStringBuilder
import jssc.{SerialPort, SerialPortEvent, SerialPortEventListener}
import rxtxio.Serial._

private[rxtxio] class SerialOperator(port: SerialPort, commander: ActorRef) extends Actor {
  private case class DataAvailable(count: Int)

  context.watch(commander)

  override def preStart = {
    val toNotify = self
    port.addEventListener(new SerialPortEventListener() {
      override def serialEvent(event: SerialPortEvent) {
        event.getEventType match {
          case SerialPortEvent.RXCHAR => toNotify ! DataAvailable(event.getEventValue)
        }
      }
    })
    self ! DataAvailable //just in case
  }

  override def postStop = {
    commander ! Closed
    port.closePort()
  }

  override def receive = {
    case Close =>
      port.closePort()
      if (sender != commander) sender ! Closed
      context.stop(self)

    case Write(data, ack) =>
      if (ack != NoAck) sender ! ack

    case DataAvailable(count) =>
      val data = read(count)
      if (data.nonEmpty) commander ! Received(data)
  }

  private def read(count: Int) = {
    val bsb = new ByteStringBuilder

    val data = port.readBytes(count)

    bsb ++= data
    bsb.result
  }
}
private[rxtxio] object SerialOperator {
  def props(port: SerialPort, commander: ActorRef) = Props(new SerialOperator(port, commander))
}