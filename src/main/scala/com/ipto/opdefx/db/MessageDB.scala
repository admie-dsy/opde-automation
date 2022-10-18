package com.ipto.opdefx.db

import java.sql.{Connection, DriverManager}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.ipto.opdefx.provider.ConfigProvider
import org.h2.message.DbException
import zio._

import scala.util.{Success, Try}

class MessageDB(val conf:ConfigProvider) {

  private val dbDir:UIO[String] = ZIO.succeed(conf.db)

  private val url:ZIO[Any, Nothing, String] = dbDir.map(dir => s"jdbc:h2:$dir/message.db")

  private val connection: () => ZIO[Any, Nothing, Connection] = () => url.map(u => DriverManager.getConnection(u))

  private val createStatement: () => ZIO[Any, Throwable, Boolean] = () => connection().bracket(con => ZIO.succeed(con.close())) { con =>
    val sql = """create table if not exists message(filename varchar(50) primary key, publicationDate varchar(50), fileGroup varchar(50), state varchar(50))"""
    val stm = con.createStatement()

    ZIO.fromTry(Try(stm.execute(sql)))
  }

  private def insertReadyRow(filename:String):Task[Unit] = {
    connection().bracket(con => ZIO.succeed(con.close())) { con =>


      val now = LocalDateTime.now()
      val formatter = DateTimeFormatter.ISO_DATE
      val dateStr = now.format(formatter)
      val state = "READY"
      val fileGroup = filename.substring(0, 17)

      val sql = s"""MERGE INTO MESSAGE M
                   |USING (SELECT '$filename' FILENAME, '$dateStr' PUBLICATIONDATE, '$fileGroup' FILEGROUP, '$state' STATE) D
                   |ON (M.FILENAME = D.Filename)
                   |WHEN NOT MATCHED THEN
                   |INSERT (FILENAME, PUBLICATIONDATE, FILEGROUP, STATE) VALUES (D.FILENAME, D.PUBLICATIONDATE, D.FILEGROUP, D.STATE)""".stripMargin
      val stm = con.prepareStatement(sql)

      ZIO.fromTry(Try{
        stm.execute()
        con.commit()
      })
    }
  }

  private def insertReadyMultiple(filenames:Seq[String]):Task[Unit] =  connection().bracket(con => ZIO.succeed(con.close())) { con =>
    val sql = """MERGE INTO MESSAGE M
                |USING (SELECT ? FILENAME, ? PUBLICATIONDATE, ? FILEGROUP, ? STATE) D
                |ON (M.FILENAME = D.Filename)
                |WHEN NOT MATCHED THEN
                |INSERT (FILENAME, PUBLICATIONDATE, FILEGROUP, STATE) VALUES (D.FILENAME, D.PUBLICATIONDATE, D.FILEGROUP, D.STATE)""".stripMargin

    val stm = con.prepareStatement(sql)

    val now = LocalDateTime.now()
    val formatter = DateTimeFormatter.ISO_DATE
    val dateStr = now.format(formatter)
    val state = "READY"

    filenames.foreach{filename =>
      val fileGroup = filename.substring(0, 17)

      /*stm.setString(1, filename)
      stm.setString(2, dateStr)
      stm.setString(3, fileGroup)
      stm.setString(4, state)*/

      stm.addBatch()
    }

    ZIO.fromTry(Try{
      stm.executeLargeBatch()
      con.commit()
      })
  }

  def getFileStatus(filename:String): ZIO[Any, Throwable, Int] = connection().bracket(con => ZIO.succeed(con.close())) { con =>

    val rs = ZIO.fromTry(Try {
      val sql = """SELECT count(*) AS FILE_COUNT FROM MESSAGE WHERE FILENAME = ? AND STATE = 'PROCESSED'"""
      val stm = con.prepareStatement(sql)
      stm.setString(1, filename)
      stm.executeQuery()
    })

    for {
      results <- rs
      count <- ZIO.succeed{
        results.next()
        results.getLong("FILE_COUNT")
      }
    } yield count.toInt
  }



  private def getNextBatch:Task[Seq[String]] = {
    connection().bracket(con => ZIO.succeed(con.close())) { con =>
      val sql =
        """
          |SELECT filename FROM MESSAGE
          |WHERE state = 'READY'
          |ORDER BY FILEGROUP
          |""".stripMargin

      val filenames = new scala.collection.mutable.ListBuffer[String]()
      val stm = con.prepareStatement(sql)

      ZIO.succeed{
        val rs = stm.executeQuery()
        while (rs.next()) {
          val s = rs.getString(1)
          filenames.addOne(s)
        }
        filenames.toSeq
      }
    }

  }

  private def insertRow(dateOpt:Option[String], filename:String):Task[Unit] = {
    connection().bracket(con => ZIO.succeed(con.close())){con =>

      dateOpt match {
        case Some(filename) =>
          ZIO.fromTry(Success(println("filename already saved")))
        case None =>
          println(s"Saving new file to database ${filename}")
          val sql = """insert into message values (?, ?)"""
          val stm = con.prepareStatement(sql)

          val now = LocalDateTime.now()
          val formatter = DateTimeFormatter.ISO_DATE

          val dateStr = now.format(formatter)

          stm.setString(1, filename)
          stm.setString(2, dateStr)
          ZIO.fromTry(Try{
            stm.execute()
            con.commit()
          })
      }


    }
  }


  private def getPublicationDate(filename:String):ZIO[Any, Throwable, Option[String]] = connection().bracket(con => ZIO.succeed(con.close())) {con =>

    val sql = """select publicationDate from message where filename = ?"""
    val stm = con.prepareStatement(sql)
    stm.setString(1, filename)

    ZIO.succeed {
      val rs = stm.executeQuery()
      if (rs.next()) {
        val date = rs.getString(1)
        Some(date)
      } else {
        None
      }
    }
  }

  private def updateState(filename:String, state:String):ZIO[Any, Throwable, Unit] = connection().bracket(con => ZIO.succeed(con.close())){con =>

    val sql = s"""MERGE INTO MESSAGE M
                 |USING (SELECT '$filename' FILENAME, '$state' STATE) D
                 |ON (M.FILENAME = D.Filename)
                 |WHEN MATCHED THEN
                 |UPDATE SET M.STATE = D.STATE""".stripMargin

    val stm = con.prepareStatement(sql)

    ZIO.fromTry(Try{
      stm.execute()
      con.commit()
    })
  }


  val insert: (Option[String], String) => ZIO[Any, Throwable, Unit] = (dateOpt, filename) => for {
      _ <- insertRow(dateOpt, filename)
  } yield ()

  val createTable: ZIO[Any, Throwable, Unit] = for {
    _ <- createStatement()
  } yield ()

  val publicationDate:String => ZIO[Any, Throwable, Option[String]] = filename => for {
    date <- getPublicationDate(filename)
  } yield date

  val insertIfNotExists:String => ZIO[Any, Throwable, Option[String]] = filename => for {
    dateOpt <- getPublicationDate(filename)
    _ <- insert(dateOpt, filename)
  } yield dateOpt

  val insertReady:String => ZIO[Any, Throwable, Unit] = filename => for {
    _ <- insertReadyRow(filename)
  } yield ()

  val getBatch: Unit => ZIO[Any, Throwable, Seq[String]] = _ => for {
    data <- getNextBatch
  } yield data

  val insertReadyBatch:Seq[String] => ZIO[Any, Throwable, Unit] = filenames => for {
    _ <- insertReadyMultiple(filenames)
  } yield ()

  val stateProcessed:String => ZIO[Any, Throwable, Unit] = filename => for {
    _ <- updateState(filename, "PROCESSED")
  } yield ()

}
