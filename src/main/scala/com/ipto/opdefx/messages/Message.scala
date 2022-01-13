package com.ipto.opdefx.messages

import zio._

trait Message {
  def program:ZIO[Any, Throwable, Array[Byte]]
}
