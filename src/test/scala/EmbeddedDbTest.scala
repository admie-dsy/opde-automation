import com.ipto.opdefx.db.MessageDB
import com.ipto.opdefx.provider.ConfigProvider
import org.scalatest.funsuite.AnyFunSuite
import zio.Runtime
import zio._
import zio.console._

class EmbeddedDbTest  extends AnyFunSuite {

  val runtime:Runtime[Any] = Runtime.default
  val conf: ConfigProvider = ConfigProvider.newInstance("app.properties")

  test("create db table"){

    val db = new MessageDB(conf)

    runtime.unsafeRunSync(db.createTable.catchAll(t => ZIO.succeed(putStrLn(t.getMessage))))

  }

  test("insert dummy record") {
    val db = new MessageDB(conf)

    //runtime.unsafeRun(db.insert("20220519T0430Z_07_IPTO_TP_001.zip").catchAll(t => ZIO.succeed(putStrLn(t.getMessage))))
    runtime.unsafeRun(db.insert(Some("2022-05-11"), "20220515T2230Z_01_IPTO_EQ_001.zip").catchAll(t => ZIO.succeed(putStrLn(t.getMessage))))
  }

  test("get publication date for message") {
    val db = new MessageDB(conf)

    runtime.unsafeRun{

      for {
        date <- db.publicationDate("20220515T2230Z_02_IPTO_EQ_001.zip")
        d = date
      } yield (println(d))

    }
  }

  test("insert new group") {
    val db = new MessageDB(conf)
    val filenames = Seq("20220515T2230Z_02_IPTO_EQ_001.zip", "20220515T2230Z_02_IPTO_SSH_001.zip", "20220515T2230Z_02_IPTO_SV_001.zip", "20220515T2230Z_02_IPTO_TP.zip")
    runtime.unsafeRun{
      for {
        _ <- ZIO.foreach(filenames)(f => db.insertReady(f))
      } yield ()
    }
  }

  test("insert batch") {
    val db = new MessageDB(conf)
    val filenames = Seq("20220515T2230Z_02_IPTO_EQ_001.zip", "20220515T2230Z_02_IPTO_SSH_001.zip", "20220515T2230Z_02_IPTO_SV_001.zip", "20220515T2230Z_02_IPTO_TP.zip")
    runtime.unsafeRun{
      for {
        _ <- db.insertReadyBatch(filenames)

      } yield ()
    }

  }

  test("next group") {
    val db = new MessageDB(conf)
    runtime.unsafeRun{
      for {
        data <- db.getBatch()
        _ <- ZIO.succeed(data.foreach(println))
      } yield ()
    }
  }

  test("update state"){
    val filename = "20220515T2230Z_02_IPTO_EQ_001.zip"
    val db = new MessageDB(conf)
    runtime.unsafeRun{
      for {
        _ <- db.stateProcessed(filename)
      } yield ()
    }
  }

}
