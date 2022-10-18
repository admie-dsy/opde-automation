

package com.ipto.opdefx.concurrency

import com.ipto.opdefx.Token
import zio._
import zio.random._

case class TopicQueue(queue:Queue[Token], subscribers:Ref[Map[Int, List[Queue[Token]]]]) {

  def subscribe(sub:Queue[Token], group:Int):UIO[Unit] ={
    subscribers.update{map => map.get(group) match {
      case Some(value) => map + (group -> (value :+ sub))
      case None => map + (group -> List(sub))
    }
    }
  }

  private val loop =
    for {
      modelFile <- queue.take
      subs <- subscribers.get
      _ <- ZIO.foreach(subs.values){group =>
        for {
          idx <- nextIntBounded(group.length)
          _ <- group(idx).offer(modelFile)
        } yield ()
      }
    } yield ()

  def run = loop.forever.fork
}

object TopicQueue {
  def create(queue: Queue[Token]):UIO[TopicQueue] = Ref.make(Map.empty[Int, List[Queue[Token]]]) >>= (map => UIO(TopicQueue(queue, map)))
}