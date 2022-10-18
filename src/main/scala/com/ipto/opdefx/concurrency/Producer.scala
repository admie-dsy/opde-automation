package com.ipto.opdefx.concurrency

import com.ipto.opdefx.Token
import zio._
import zio.console._
import zio.duration._
import java.io.File

import com.ipto.opdefx.db.MessageDB
import com.ipto.opdefx.provider.{ConfigProvider, ReadyScanner, TokenProvider}

case class Producer(queue:Queue[Token], dir:File, conf:ConfigProvider, flag:Ref[Int]) {

  private val tokenProvider = new TokenProvider(conf)

  def run:URIO[clock.Clock with Console, Fiber.Runtime[Throwable, Unit]] = {
    //var scanner = new FileScanner(dir)

    var scanner = new ReadyScanner(dir, conf)
    var counter = 0

    val db = new MessageDB(conf)


    val loop = for {
        saml <- tokenProvider.provide
        _ = scanner.setSaml(saml)
        token <- ZIO.succeed(scanner.next())
        _ <- putStrLn(s"Producer producing ${token.message}")
        filename = token.message.split("\\\\").last

        count <- db.getFileStatus(filename)
        _ <- ZIO.when(count == 0) {

          for {
            _ <- putStrLn(s"Count of rows for $filename is $count")
            _ <- queue.offer(token)
            _ <- putStrLn(s"Offering to queue $filename")
            _ <- db.stateProcessed(filename)
            _ <- putStrLn(s"Changed state to PROCESSED")
          } yield ()
        }

        /*_ <- queue.offer(token)
        _ <- putStrLn(s"Offering to queue $filename")
        _ <- db.stateProcessed(filename)
        _ <- putStrLn(s"Changed state to PROCESSED")*/
        _ <- ZIO.sleep(5.seconds)
        _ <- ZIO.succeed{
          counter += 5
          //Allow 30 minutes before scanning the folder again. There must be sufficient time for the consumers to finish processing.
          //Otherwise the following situation may occur:
          //1. The producer scans the folder and puts an item in the queue of a consumer.
          //2. The worker will eventually remove the item from the queue and start processing it.
          //3. The producer scans the folder but the item is still present as its processing has not been completed. The producer distributes the same item again.
          //4. The worker will retrieve the item to process but since it has already been processed and the resource has been deleted an exception will be thrown and the fiber will crash.
          //Note that this is only a temporary solution.
          if (counter == 5 * 60) {
            //scanner = new FileScanner(dir)
            scanner = new ReadyScanner(dir, conf)
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
