package com.ipto.opdefx.concurrency

import com.ipto.opdefx.{EQ, Empty, SSH, SV, TP, Token}
import zio._
import zio.console._

case class Distribute() {

  def run = for {

    jobQueue <- Queue.bounded[Token](10)

    queueSSH <- Queue.bounded[Token](10)
    topicQueueSSH <- TopicQueue.create(queueSSH)

    queueSV <- Queue.bounded[Token](10)
    topicQueueSV <- TopicQueue.create(queueSV)

    queueTP <- Queue.bounded[Token](10)
    topicQueueTP <- TopicQueue.create(queueTP)

    queueEQ <- Queue.bounded[Token](10)
    topicQueueEQ <- TopicQueue.create(queueEQ)

    loop = for {
      job <- jobQueue.take
      _ <- job match {
        case model:EQ => queueEQ.offer(model)
        case model:TP => queueTP.offer(model)
        case model:SV => queueSV.offer(model)
        case model:SSH => queueSSH.offer(model)
        case _:Empty => putStrLn("bla")
      }
    } yield ()
    fiber <- loop.forever.fork
  } yield (jobQueue, topicQueueSSH, topicQueueSV, topicQueueTP, topicQueueEQ, fiber)

}

object Distribute {
  def create() = UIO(Distribute())
}