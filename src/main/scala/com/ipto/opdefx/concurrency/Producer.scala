package com.ipto.opdefx.concurrency

import com.ipto.opdefx.Token
import zio._
import zio.console._
import zio.duration._
import java.io.File

import com.ipto.opdefx.provider.{ConfigProvider, FileScanner, TokenProvider}

case class Producer(queue:Queue[Token], dir:File, conf:ConfigProvider, flag:Ref[Int]) {

  private val tokenProvider = new TokenProvider(conf)

  def run:URIO[clock.Clock with Console, Fiber.Runtime[Throwable, Unit]] = {
    var scanner = new FileScanner(dir)
    var counter = 0

    val loop = for {
        value <- flag.get
        //_ <- putStrLn(s"Flag value in producer $value")
        saml <- tokenProvider.provide
        _ = scanner.setSaml(saml)
        token <- ZIO.succeed(scanner.next())
        //_ <- putStrLn(s"Producer producing ${token.message}")
        _ <- queue.offer(token)
        _ <- ZIO.sleep(10.seconds)
        _ <- ZIO.succeed{
          counter += 5
          if (counter == 100) {
            scanner = new FileScanner(dir)
            counter = 0
            println("Resetting counter")
          }
        }
    } yield ()

    loop.repeatUntilM{_ => for {
      value <- flag.get
    } yield value == 0
    }.fork
  }

}

object Producer {
  def create(queue: Queue[Token], dir:File, conf:ConfigProvider, flag:Ref[Int]):UIO[Producer] = UIO(Producer(queue, dir, conf, flag))
}
