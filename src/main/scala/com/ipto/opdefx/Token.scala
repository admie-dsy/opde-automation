package com.ipto.opdefx

sealed trait Token{
  val message:String
  val saml:String
}
case class Empty(override val message: String = "", override val saml: String="") extends Token
case class EQ(override val message:String, override val saml: String) extends Token
case class SSH(override val message:String, override val saml: String) extends Token
case class SV(override val message:String, override val saml: String) extends Token
case class TP(override val message:String, override val saml: String) extends Token
