package com.ipto.opdefx.concurrency

import java.io.File

import com.ipto.opdefx.provider.ConfigProvider
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.random.Random


object FiberSystem {

  private val conf = ConfigProvider.newInstance("app.properties")
  private val outDir = new File(conf.out)

  def run(flag:Ref[Int], messageQueue:Queue[String]): ZIO[Random with Clock with Console, Throwable, Unit] = for {

    sshConsumer <- Consumer.create("SSH", messageQueue)
    ctxSSH <- sshConsumer.run
    (sshQueue, sshFibber) = ctxSSH

    svConsumer <- Consumer.create("SV", messageQueue)
    ctxSV <- svConsumer.run
    (svQueue, svFibber) = ctxSV

    tpConsumer <- Consumer.create("TP", messageQueue)
    ctxTP <- tpConsumer.run
    (tpQueue, tpFibber) = ctxTP

    eqConsumer <- Consumer.create("EQ", messageQueue)
    ctxEQ <- eqConsumer.run
    (eqQueue, eqFibber) = ctxEQ

    distributor <- Distribute.create()
    ctxDistribute <- distributor.run
    (inputQueue, topicQueueSSH, topicQueueSV, topicQueueTP, topicQueueEQ, _) = ctxDistribute

    producer <- Producer.create(inputQueue, outDir, conf, flag)
    producerFiber <- producer.run

    _ <- topicQueueSSH.subscribe(sshQueue, group = 1)
    topicSHHFiber <- topicQueueSSH.run

    //_ <- topicSHHFiber.join

    _ <- topicQueueSV.subscribe(svQueue, group = 2)
    topicQueueSVFiber <- topicQueueSV.run

    //_ <- topicQueueSVFiber.join

    _ <- topicQueueTP.subscribe(tpQueue, group = 3)
    topicQueueTPFiber <- topicQueueTP.run

    //_ <- topicQueueTPFiber.join

    _ <- topicQueueEQ.subscribe(eqQueue, group = 4)
    topicQueueEQFiber <- topicQueueEQ.run

    //_ <- topicQueueEQFiber.join

    /*_ <- sshFibber.join

    _ <- svFibber.join

    _ <- tpFibber.join

    _ <- eqFibber.join*/

    _ <- producerFiber.join

    //_ <- distributeFiber.join

  } yield ()
}
