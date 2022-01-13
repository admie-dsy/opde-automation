package com.ipto.opdefx.provider

import java.io.{BufferedWriter, File, FileNotFoundException, FileWriter, StringReader, StringWriter}

import javax.xml.parsers.DocumentBuilderFactory
import java.text.SimpleDateFormat
import java.util.Date

import com.ipto.opdefx.messages.RequestTokenMessage
import com.ipto.opdefx.requests.RequestBroker
import javax.xml.transform.{OutputKeys, TransformerFactory}
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.xml.sax.InputSource
import zio._

class TokenProvider(val conf:ConfigProvider) {

  val availableToken: IO[FileNotFoundException, File] = ZIO.cond(
    new File(conf.saml).listFiles().length > 0,
    new File(conf.saml)
      .listFiles()
      .toList
      .sortBy(f => f.lastModified())
      .reverse
      .head
    ,
    new FileNotFoundException("No available token found")
  )

  val writeTokenToFile: String => UIO[Unit] = token => ZIO.succeed{
    val filename = s"${conf.saml}/token.xml"
    new BufferedWriter(new FileWriter(filename, false))
  }.bracket(writer => ZIO.succeed(writer.close())){writer =>
    ZIO.succeed(writer.write(token))
  }

  val downloadToken:ZIO[Any, Throwable, String] =
    for {
      tokenRequest <- ZIO.succeed(new RequestTokenMessage(conf.username, conf.password))
      message <- tokenRequest.program
      newToken <- RequestBroker.sendRequest(conf.tokenEndpoint, conf.tokenAction, message)
      _ <- writeTokenToFile(newToken)
    } yield newToken

  val parseToken: File => IO[Throwable, String] = file => ZIO.succeed(scala.io.Source.fromFile(file)).bracket(source => ZIO.succeed(source.close())) {source =>
    val content = source.getLines().mkString("\n")
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    documentBuilderFactory.setNamespaceAware(true)
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()
    val doc = documentBuilder.parse(new InputSource(new StringReader(content)))
    val conditions = doc.getElementsByTagName("saml2:Conditions")
    val condition = conditions.item(0)
    val expirationDateStr = condition.getAttributes.getNamedItem("NotOnOrAfter").getNodeValue
    val sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS")
    val expirationDate = sdf.parse(expirationDateStr)
    val notExpired = expirationDate.after(new Date())
    if (notExpired) ZIO.succeed(content)
    else ZIO.fail(new FileNotFoundException("Expired Token"))
  }

  val extractToken:String => UIO[String] = msg => ZIO.succeed{
    val documentBuilderFactory = DocumentBuilderFactory.newInstance()
    documentBuilderFactory.setNamespaceAware(true)
    val documentBuilder = documentBuilderFactory.newDocumentBuilder()
    val doc = documentBuilder.parse(new InputSource(new StringReader(msg)))
    val saml = doc.getElementsByTagName("saml2:Assertion").item(0)
    val domSource = new DOMSource(saml)
    val writer = new StringWriter()
    val result = new StreamResult(writer)
    val tf = TransformerFactory.newInstance()
    val transformer = tf.newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "no")
    transformer.setOutputProperty(OutputKeys.METHOD, "xml")
    transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
    transformer.transform(domSource, result)
    writer
  }.bracket(writer => ZIO.succeed(writer.close())) {writer =>
    ZIO.succeed(writer.toString.replaceAll("<\\?xml(.+?)\\?>", "").trim())
  }


  def provide:ZIO[Any, Throwable, String] =  for {
    content <- availableToken
      .flatMap(parseToken)
      .catchSome{case _:FileNotFoundException =>
        downloadToken
      }
    token <- extractToken(content)
  } yield token


}
