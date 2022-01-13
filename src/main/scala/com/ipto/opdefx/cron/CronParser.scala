package com.ipto.opdefx.cron

import scala.util.parsing.combinator.RegexParsers

sealed trait Timing
case object TimingObject extends Timing
case class TimingSeq(list: Seq[Int]) extends Timing
case class Bounds(from: Int, to: Int) extends Timing
case class TimingObjectPar(par: Int) extends Timing
case class Fraction(top: Int, bottom: Int) extends Timing

case class Cron(min: Timing, hour: Timing, day: Timing, month: Timing, dayOfWeek: Timing)
case object Space

object CronParser extends RegexParsers {
  def repN[T, U](num: Int, p: Parser[T], q: Parser[U]): Parser[List[T]] = {
    p ~ repN(num, q ~> p) ^^ { case r ~ rs => r :: rs } | success(List())
  }
  def space: Parser[Space.type] = "[ \t]+".r ^^ (_ => Space)
  def decimalNumber: Parser[String] = "[0-9]+".r
  def timingObj: Parser[Timing] = "*"~opt("/"~decimalNumber) ^^ {
    case x~Some(y~z)=> TimingObjectPar(z.toInt)
    case x~None => TimingObject
  }
  def timingSeq: Parser[Timing] = decimalNumber~opt(","~repsep(decimalNumber, ",")) ^^ {
    case x~Some(","~ys) => TimingSeq((x::ys).map(_.toInt))
    case x~None => TimingSeq(List(x.toInt))
  }
  def bounds: Parser[Timing] = decimalNumber ~ "-" ~ decimalNumber ^^ { case f ~ x ~ t => Bounds(f.toInt, t.toInt) }
  def fraction: Parser[Timing] = decimalNumber ~ "/" ~ decimalNumber ^^ { case t ~ x ~ b => Fraction(t.toInt, b.toInt) }
  def timing: Parser[Timing] = timingObj | bounds | fraction | timingSeq
  def timings: Parser[List[Timing]] = repN(5, timing)
  def cron: Parser[Cron] = timings ^^ (xs => Cron(xs.head, xs(1), xs(2), xs(3), xs(4)))

  def apply(input: String): ParseResult[Cron] = parseAll(cron, input)
}
