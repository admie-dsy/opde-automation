package com.ipto.opdefx.provider

import scala.io.Source

class ConfigProvider private (val workdir:String, val out:String, val saml:String, val username:String, val password:String,
                              val tokenEndpoint:String, val tokenAction:String, val publicationEnpoint:String, val publicationAction:String,
                              val processed:String, val script:String, val connectivityEndpoint:String) {

  override def toString: String = {
    List(workdir, out, saml, tokenEndpoint, tokenAction, publicationAction, publicationEnpoint, processed, script).mkString("\n")
  }

}

object ConfigProvider {

  private def appendToHomeDir(url:String, addTrailing:Boolean=true):String = {
    val user = System.getProperty("user.home").replace("\\", "/")
    s"$user/$url${if (addTrailing) "/" else ""}"
  }

  def newInstance(propertyFile:String):ConfigProvider = {
    val lines = Source.fromResource(propertyFile).getLines().toList
    val map = lines.map(_.split("=")).map(l => l(0).trim -> l(1).trim).toMap
    new ConfigProvider(workdir = appendToHomeDir(map("workdir")),
      out = appendToHomeDir(map("out")),
      saml = appendToHomeDir(map("saml")),
      username = map("username"),
      password = map("password"),
      tokenEndpoint = map("tokenEndpoint"),
      tokenAction = map("tokenAction"),
      publicationEnpoint = map("publicationEndpoint"),
      publicationAction = map("publicationAction"),
      processed = map("processed"),
      script = map("script"),
      connectivityEndpoint = map("connectivityEndpoint")
    )
  }

}
