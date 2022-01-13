package com.ipto.opdefx.messages

import java.io.{StringReader, StringWriter}
import java.nio.charset.StandardCharsets

import com.ipto.opdefx.payload.TokenPayload
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.{OutputKeys, TransformerFactory}
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.xml.sax.InputSource
import zio._

import scala.util.Try

class RequestTokenMessage(val username:String, val password:String) extends Message {

  val document:Task[Document] = ZIO.fromTry{
    val payload = TokenPayload.payload
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    documentBuilderFactory.setNamespaceAware(true)
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()
    Try(documentBuilder.parse(new InputSource(new StringReader(payload))))
  }

  val injectCredentials: Document => Task[Document] = doc => ZIO.fromTry{
    Try {
      val nodeList = doc.getElementsByTagName("wsse:Username")
      val userElem = nodeList.item(0)
      userElem.setTextContent(username)
      val passElem = doc.getElementsByTagName("wsse:Password").item(0)
      passElem.setTextContent(password)
      doc
    }
  }

  val prettyString: Document => Task[String] = doc => ZIO.fromTry{
    Try {
      val domSource = new DOMSource(doc)
      val writer = new StringWriter()
      val result = new StreamResult(writer)
      val tf = TransformerFactory.newInstance()
      val transformer = tf.newTransformer()
      transformer.setOutputProperty(OutputKeys.INDENT, "no")
      transformer.setOutputProperty(OutputKeys.METHOD, "xml")
      transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
      transformer.transform(domSource, result)
      writer
    }
  }.bracket(writer => ZIO.succeed(writer.close())) {writer => ZIO.succeed(writer.toString)}

  override val program:ZIO[Any, Throwable, Array[Byte]] = for {
    doc <- document
    docWithCredentials <- injectCredentials(doc)
    str <- prettyString(docWithCredentials)
    bytes = str.getBytes(StandardCharsets.UTF_8)
  } yield bytes

}
