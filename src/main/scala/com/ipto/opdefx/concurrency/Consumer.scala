package com.ipto.opdefx.concurrency

import java.io.File
import java.nio.file.{Files, Paths}

import com.ipto.opdefx.Token
import com.ipto.opdefx.messages.PublicationMessage
import com.ipto.opdefx.provider.ConfigProvider
import com.ipto.opdefx.requests.RequestBroker
import zio._
import zio.console._
import zio.duration._


case class Consumer(name:String, messageQueue:Queue[String]){

  def read(model:Token):UIO[Array[Byte]] = {
    UIO {
      val filename = model.message
      val content = Files.readAllBytes(Paths.get(filename))
      content
    }
  }

  def run: ZIO[console.Console with clock.Clock, Nothing, (zio.Queue[Token], Fiber.Runtime[Throwable, Nothing])] = for {
    queue <- Queue.bounded[Token](10)
    conf = ConfigProvider.newInstance("app.properties")
    loop = for {
      _ <- ZIO.sleep(2.seconds)
      token <- queue.take
      //content <- read(token)
      message = new PublicationMessage(token.message, token.saml)
      content <- message.program
      response <- RequestBroker.sendRequest(conf.publicationEnpoint, conf.publicationAction, content)
      _ <- putStrLn(response)
      _ <- putStrLn(s"[$name] Processed ${token.message} size: ${content.length}")
      _ <- ZIO.succeed(new File(token.message).delete())
      _ <- messageQueue.offer(s"[$name] finished processing ${token.message}, deleted file\n")
      _ <- putStrLn(s"[$name] finished processing ${token.message}, deleted file")
    } yield ()

    fiber <- loop.forever.fork

  } yield (queue, fiber)
}

object Consumer {
  def create[A](name:String, messageQueue:Queue[String]):UIO[Consumer] = UIO(Consumer(name, messageQueue))
}
