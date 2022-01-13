package com.ipto.opdefx

import com.ipto.opdefx.ui.MainUI
import zio._

object RichClient extends {

  def main(args:Array[String]): Unit = {
    val runtime = Runtime.default
    val ui = new MainUI

    runtime.unsafeRun{
      ui.visible = true
      ui.build()
    }
  }
}
