package com.ipto.opdefx.cron

import java.util.concurrent.{Executors, TimeUnit}

import com.ipto.opdefx.db.MessageDB
import com.ipto.opdefx.provider.ConfigProvider
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

class ScheduleExecutor(val schedule:Schedule,  val task: () => Unit, startImmediately:Boolean = true) {

  private val executor = Executors.newScheduledThreadPool(1)
  private var nextExecution:DateTime = schedule.getNextAfter(DateTime.now(DateTimeZone.forID("Europe/Athens")))
  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
  private var firstRun:Boolean = startImmediately


  def start():Unit = {

    executor.scheduleAtFixedRate(() => {
      if (firstRun){
        task()
        firstRun = false
      }
      println(s"Executor time check now: ${fmt.print(DateTime.now())} next execution: ${fmt.print(nextExecution)}")
      if (nextExecution.isBeforeNow) {
        task()
        nextExecution = schedule.getNextAfter(DateTime.now(DateTimeZone.forID("Europe/Athens")))
      }
    }, 5, 5, TimeUnit.SECONDS)

  }

  def shutdown():Unit = {
    println("Stopping scheduled executor")
    executor.shutdown()
  }

}
