package rxtxio

import akka.actor._
import jssc.{SerialPort, SerialPortList}
import rxtxio.Serial._

import scala.util.{Failure, Success, Try}

/**
  *  Opens the serial port and then starts a SerialOperator to handle the communication over
  *  that port.
  */
private[rxtxio] class SerialManager extends Actor {
  override def receive = {
    case ListPorts =>
      val ports = SerialPortList.getPortNames().toVector
      sender ! Ports(ports)

    case c @ Open(handler, port, baudRate, dataBits, parity, stopBits, flowControl) =>
      Try {
        val serialPort = new SerialPort(port)
        val data = dataBits match {
          case DataBits5 => SerialPort.DATABITS_5
          case DataBits6 => SerialPort.DATABITS_6
          case DataBits7 => SerialPort.DATABITS_7
          case DataBits8 => SerialPort.DATABITS_8
        }
        val stop = stopBits match {
          case OneStopBit => SerialPort.STOPBITS_1
          case OneAndHalfStopBits => SerialPort.STOPBITS_1_5
          case TwoStopBits => SerialPort.STOPBITS_2
        }
        val par = parity match {
          case NoParity => SerialPort.PARITY_NONE
          case EvenParity => SerialPort.PARITY_EVEN
          case OddParity => SerialPort.PARITY_ODD
          case MarkParity => SerialPort.PARITY_MARK
          case SpaceParity => SerialPort.PARITY_SPACE
        }
        val fc = flowControl match {
          case NoFlowControl => SerialPort.FLOWCONTROL_NONE
          case RtsFlowControl => SerialPort.FLOWCONTROL_RTSCTS_IN | SerialPort.FLOWCONTROL_RTSCTS_OUT
          case XonXoffFlowControl => SerialPort.FLOWCONTROL_XONXOFF_IN | SerialPort.FLOWCONTROL_XONXOFF_OUT
        }
        serialPort.openPort()
        serialPort.setParams(baudRate, data, stop, par)
        serialPort.setFlowControlMode(fc)
        serialPort
      } match {
        case Success(serialPort) =>
          val operator = context.actorOf(SerialOperator.props(serialPort, handler))
          handler ! Opened(operator, port)
        case Failure(error) =>
          handler ! CommandFailed(c, error)
      }
  }
}