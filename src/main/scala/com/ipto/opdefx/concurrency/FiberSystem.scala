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
    _ <- topicQueueSSH.run

    _ <- topicQueueSV.subscribe(svQueue, group = 2)
    _ <- topicQueueSV.run

    _ <- topicQueueTP.subscribe(tpQueue, group = 3)
    _ <- topicQueueTP.run

    _ <- topicQueueEQ.subscribe(eqQueue, group = 4)
    _ <- topicQueueEQ.run

    _ <- producerFiber.join

  } yield ()
}
