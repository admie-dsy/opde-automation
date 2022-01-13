package com.ipto.opdefx.messages

import java.io.{ByteArrayOutputStream, StringReader}
import java.nio.file.{Files, Paths}

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.soap.{MessageFactory, SOAPMessage}
import org.apache.commons.codec.binary.Base64
import org.xml.sax.InputSource
import zio._

import scala.util.Try

class PublicationMessage(val resource:String, val token:String) extends Message {

  override def program:ZIO[Any, Throwable, Array[Byte]] = {

    val message = ZIO.succeed{
      val msg = MessageFactory.newInstance().createMessage()
      val sp = msg.getSOAPPart
      val envelope = sp.getEnvelope
      envelope.addNamespaceDeclaration("ser", "http://services.soap.interfaces.application.components.opdm.entsoe.eu/")
      msg
    }

    val putBody: SOAPMessage => Task[SOAPMessage] = msg => ZIO.fromTry{
      val body = msg.getSOAPPart.getEnvelope.getBody
      Try {
        val path = Paths.get(resource)
        val ba = Files.readAllBytes(path)
        val base64 = new Base64()
        val encodedString = new String(base64.encode(ba))
        val pbRequest = body.addChildElement("PublicationRequest", "ser")
        val datasetSelem = pbRequest.addChildElement("dataset")
        val idElem = datasetSelem.addChildElement("id")
        val filename = path.getFileName.toString
        idElem.setValue(filename)
        val typeElem = datasetSelem.addChildElement("type")
        typeElem.setValue("CGMES")
        val contentElem = datasetSelem.addChildElement("content")
        contentElem.setValue(encodedString)
        msg
      }
    }

    val putToken: SOAPMessage => Task[SOAPMessage] = msg => ZIO.fromTry{
      val header = msg.getSOAPHeader

      val securityElem = header.addChildElement("Security", "wsse", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd")
      securityElem.addNamespaceDeclaration("wsu", "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd")

      Try {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        documentBuilderFactory.setNamespaceAware(true)
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()
        val document = documentBuilder.parse(new InputSource(new StringReader(token)))
        val securityDoc = securityElem.getOwnerDocument
        val newNode = securityDoc.importNode(document.getFirstChild, true)
        securityElem.appendChild(newNode)
        msg.saveChanges()
        msg
      }
    }

    val asBinary:SOAPMessage => UIO[Array[Byte]] = msg => ZIO.succeed(new ByteArrayOutputStream()).bracket(out => ZIO.succeed(out.close())) {out =>
      ZIO.succeed{
        msg.writeTo(out)
        out.toByteArray}
    }

    val build = for {
      msg <- message
      withBody <- putBody(msg)
      withToken <- putToken(withBody)
      bytes <- asBinary(withToken)
    } yield bytes

    build

  }

}
