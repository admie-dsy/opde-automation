package com.ipto.opdefx.provider

import java.io.File

import com.ipto.opdefx.db.MessageDB
import com.ipto.opdefx.{EQ, Empty, SSH, SV, TP, Token}
import zio._

class ReadyScanner(val dir:File, val conf:ConfigProvider) extends Iterator[Token] {

  private val runtime = Runtime.default
  private val db = new MessageDB(conf)
  private var saml:String = _

  def setSaml(saml:String): Unit = synchronized{
    this.saml = saml
  }

  private val files = runtime.unsafeRun{
    for {
      data <- db.getBatch()
    } yield data
  }.iterator

  def hasNext: Boolean = files.hasNext

  def next():Token = {

    val model = if (hasNext) {
      val filename = files.next()
      val absFilename = s"$dir${File.separatorChar}${filename}"
      if (filename.contains("EQ")) {
        EQ(absFilename, saml)
      } else if (filename.contains("SV")) {
        SV(absFilename, saml)
      } else if (filename.contains("TP")){
        TP(absFilename, saml)
      } else if (filename.contains("SSH")){
        SSH(absFilename, saml)
      } else {
        Empty()
      }
    } else {
      Empty()
    }
    model
  }
}

class FileScanner(val dir:File) extends Iterator[Token]{

  private val files = synchronized{
    dir.listFiles().sorted.to(LazyList).iterator
  }

  private var saml:String = _

  def setSaml(saml:String): Unit = synchronized{
    this.saml = saml
  }

  def hasNext: Boolean = files.hasNext

  def next():Token = {

    val model = if (hasNext) {
      val filename = files.next().getName
      val absFilename = s"$dir${File.separatorChar}${filename}"
      if (filename.contains("EQ")) {
        EQ(absFilename, saml)
      } else if (filename.contains("SV")) {
        SV(absFilename, saml)
      } else if (filename.contains("TP")){
        TP(absFilename, saml)
      } else if (filename.contains("SSH")){
        SSH(absFilename, saml)
      } else {
        Empty()
      }
    } else {
      Empty()
    }
    model
  }

}
