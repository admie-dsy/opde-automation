package com.ipto.opdefx.util

import java.nio.file.{Files, Path, Paths, StandardCopyOption}
import java.time.{LocalDateTime, ZoneId}

import com.ipto.opdefx.db.MessageDB
import com.ipto.opdefx.provider.ConfigProvider
import zio._

import scala.jdk.StreamConverters._

object FileUtils {

  def listFiles(srcDir:String, processedDir:String,  offset:Int = 1, useFilter:Boolean = true):Seq[Path] = {
    val path = Paths.get(srcDir)
    val files = Files.list(path).toScala(LazyList)
    /*val processedFiles = Files.list(Paths.get(processedDir)).toScala(LazyList)

    val fileFilter:Path => Boolean = path => {
      val dt = Files.getLastModifiedTime(path).toInstant.atZone(ZoneId.systemDefault()).toLocalDateTime
      val today = LocalDateTime.now()
      dt.isAfter(today.minusDays(offset))
    }

    val set = processedFiles.map(_.getFileName).toSet
    val pFilter:Path => Boolean = path => {
      val filename = path.getFileName
      !set.contains(filename)
    }


    if (useFilter) files.filter(pFilter).filter(fileFilter).toList else files.filter(pFilter).toList*/

    files.toList
  }

  def copyFiles(srcDir:String, destDir:String, processedDir:String, offset:Int, useFilter:Boolean = true):Seq[String] = {
    val source = Paths.get(srcDir)
    val sourceFileNames = listFiles(srcDir, processedDir, offset, useFilter).map(f => f.getFileName).map(_.toString)
    val dest = Paths.get(destDir)

    sourceFileNames.foldLeft(0){(acc, f) =>
      val destFile = Paths.get(dest.toString, f)
      val srcFile = Paths.get(source.toString, f)
      Files.copy(srcFile, destFile, StandardCopyOption.REPLACE_EXISTING)
      Files.delete(srcFile)
      acc+1
    }

    sourceFileNames
  }



}
