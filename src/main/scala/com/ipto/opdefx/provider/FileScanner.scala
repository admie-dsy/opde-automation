package com.ipto.opdefx.provider

import java.io.File

import com.ipto.opdefx.{EQ, Empty, SSH, SV, TP, Token}

class FileScanner(val dir:File) extends Iterator[Token]{

  private var files = synchronized{
    dir.listFiles().to(LazyList).iterator
  }

  private var saml:String = _

  def setSaml(saml:String) = synchronized{
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
