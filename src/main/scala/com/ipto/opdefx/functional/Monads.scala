package com.ipto.opdefx.functional

import java.io.File

object Monads {

  implicit class FileM(file:File) {
    def exists:Option[File] = if (file.exists()) Some(file) else None
    def remove:Option[File] = if (file.delete()) Some(file) else None
  }

}
