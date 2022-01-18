package com.ipto.opdefx.concurrency

import java.io.IOException

import com.ipto.opdefx.{EQ, Empty, SSH, SV, TP, Token}
import zio._
import zio.console._

case class Distribute() {

  def run: ZIO[Console, Nothing, (Queue[Token], TopicQueue, TopicQueue, TopicQueue, TopicQueue, Fiber.Runtime[IOException, Nothing])] = for {

    jobQueue <- Queue.bounded[Token](100)

    queueSSH <- Queue.bounded[Token](100)
    topicQueueSSH <- TopicQueue.create(queueSSH)

    queueSV <- Queue.bounded[Token](100)
    topicQueueSV <- TopicQueue.create(queueSV)

    queueTP <- Queue.bounded[Token](100)
    topicQueueTP <- TopicQueue.create(queueTP)

    queueEQ <- Queue.bounded[Token](100)
    topicQueueEQ <- TopicQueue.create(queueEQ)

    loop = for {
      job <- jobQueue.take
      _ <- job match {
        case model:EQ =>
          println(s"Distributing EQ: ${model.message}")
          queueEQ.offer(model)
        case model:TP =>
          println(s"Distributing TP: ${model.message}")
          queueTP.offer(model)
        case model:SV =>
          println(s"Distributing SV: ${model.message}")
          queueSV.offer(model)
        case model:SSH =>
          println(s"Distributing SSH: ${model.message}")
          queueSSH.offer(model)
        case _:Empty =>
          putStrLn("Nothing to distribute")
      }
    } yield ()
    fiber <- loop.forever.fork
  } yield (jobQueue, topicQueueSSH, topicQueueSV, topicQueueTP, topicQueueEQ, fiber)

}

object Distribute {
  def create() = UIO(Distribute())
}