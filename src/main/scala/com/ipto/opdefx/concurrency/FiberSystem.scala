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
    (sshQueue, _) = ctxSSH

    svConsumer <- Consumer.create("SV", messageQueue)
    ctxSV <- svConsumer.run
    (svQueue, _) = ctxSV

    tpConsumer <- Consumer.create("TP", messageQueue)
    ctxTP <- tpConsumer.run
    (tpQueue, _) = ctxTP

    eqConsumer <- Consumer.create("EQ1", messageQueue)
    ctxEQ <- eqConsumer.run
    (eqQueue, _) = ctxEQ

    eqConsumer1 <- Consumer.create(name = "EQ", messageQueue)
    ctxEQ1 <- eqConsumer1.run
    (eqQueue1, _) = ctxEQ1

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
    _ <- topicQueueEQ.subscribe(eqQueue1, group = 4)
    _ <- topicQueueEQ.run

    _ <- producerFiber.join

  } yield ()
}
