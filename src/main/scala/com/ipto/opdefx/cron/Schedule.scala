package com.ipto.opdefx.cron

import org.joda.time._
import scala.annotation.tailrec

case class Schedule(cronExpression: String, timezone: DateTimeZone = DateTimeZone.UTC) {

  private val cron = CronParser( cronExpression ).get

  def getNextAfter( time: DateTime ): DateTime = {
    val originTimeZone = time.getZone()
    getNextAfterRec( time.withZone(timezone).plusMinutes( 1 ) ).withZone( originTimeZone ) // Plus one minute because same datetime is not good for this method because of "AFTER"
  }

  @tailrec
  private def getNextAfterRec( time: DateTime ): DateTime = {
    if( !isSatisfiedMinute( time ) ){
      getNextAfterRec( time.plusMinutes( 1 ) )
    } else if ( !isSatisfiedHour( time ) ) {
      getNextAfterRec( time.plusHours( 1 ).withMinuteOfHour(minMinuteNumber) )
    } else if ( !isSatisfiedDay( time ) || !isSatisfiedWeek( time ) ) {
      getNextAfterRec( time.plusDays( 1 ).withHourOfDay( minHourNumber ).withMinuteOfHour( minMinuteNumber ) )
    } else if ( !isSatisfiedMonth( time ) ){
      getNextAfterRec( time.plusMonths( 1 ).withDayOfMonth( 1 ).withHourOfDay( minHourNumber ).withMinuteOfHour( minMinuteNumber ) )
    } else {
      time
    }
  }

  private def isSatisfiedNumber( timing: Timing ): (Int) => Boolean = {
    timing match {
      case TimingObject =>
       _: Int => true
      case TimingSeq(list) =>
      num: Int => list.contains(num)
      case Bounds(from, to) =>
       num: Int => num >= from && num <= to
      case TimingObjectPar(par) =>
       num: Int => num % par == 0
      case Fraction(top, bottom) =>
       num: Int => num % bottom == top
    }
  }
  private val isSatisfiedMinuteNumber : (Int) => Boolean = isSatisfiedNumber( cron.min )
  private val isSatisfiedHourNumber : (Int) => Boolean = isSatisfiedNumber( cron.hour )
  private val isSatisfiedDayNumber : (Int) => Boolean = isSatisfiedNumber( cron.day )
  private val isSatisfiedMonthNumber : (Int) => Boolean = isSatisfiedNumber( cron.month )
  private val isSatisfiedWeekNumber : (Int) => Boolean = isSatisfiedNumber( cron.dayOfWeek )

  private val minMinuteNumber = {
    var num = 0
    while( !isSatisfiedMinuteNumber( num ) ){
      num = num +1
    }
    num
  }
  private val minHourNumber   = {
    var num = 0
    while( !isSatisfiedHourNumber( num ) ){
      num = num +1
    }
    num
  }
  private val minDayNumber = {
    var num = 1
    while( !isSatisfiedDayNumber( num ) ){
      num = num +1
    }
    num
  }

  private def isSatisfiedMinute(time: DateTime): Boolean = {
    isSatisfiedMinuteNumber( time.getMinuteOfHour )
  }

  private def isSatisfiedHour(time: DateTime): Boolean = {
    isSatisfiedHourNumber(time.getHourOfDay)
  }
  private def isSatisfiedDay(time: DateTime): Boolean = {
    isSatisfiedDayNumber(time.getDayOfMonth)
  }
  private def isSatisfiedMonth(time: DateTime): Boolean = {
    isSatisfiedMonthNumber(time.getMonthOfYear)
  }
  private def isSatisfiedWeek( time: DateTime ): Boolean = {
    isSatisfiedWeekNumber(time.getDayOfWeek)
  }
}
