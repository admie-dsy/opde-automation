import com.ipto.opdefx.messages.PublicationMessage
import com.ipto.opdefx.provider.{ConfigProvider, TokenProvider}
import org.scalatest.funsuite.AnyFunSuite
import zio._
import zio.console._
import java.nio.file.{Files, Paths}
import com.ipto.opdefx.util.FileUtils

import com.ipto.opdefx.requests.RequestBroker

class PublicationSpec extends AnyFunSuite {

  val runtime:Runtime[Any] = Runtime.default
  val conf: ConfigProvider = ConfigProvider.newInstance("app.properties")
  val provider = new TokenProvider(conf)

  val resource = "C:\\Users\\k.passadis\\opde-client-workdir\\out\\20220515T2230Z_01_IPTO_EQ_001.zip"
  val outputFile = "C:\\Users\\k.passadis\\opde-client-workdir\\out.txt"

  val tokenProgram:ZIO[Any, Throwable, String] = for {
    token <- provider.provide
  } yield token

  val createMessage: ZIO[Any, Throwable, Array[Byte]] = for {
    token <- tokenProgram
    message = new PublicationMessage(resource, token)
    m <- message.program
  } yield m

  val publishMessage = for {
    message <- createMessage
    response <- RequestBroker.sendRequest(conf.publicationEnpoint, conf.publicationAction, message)
  } yield response


  test("Acquire and print token"){
    val token = runtime.unsafeRun(tokenProgram)
    println(token)
  }

  test("Create Publication Message"){
    val message = runtime.unsafeRun(createMessage)
    Files.write(Paths.get(outputFile), message)
  }

  test("Publish Message"){
    val response = runtime.unsafeRun(publishMessage)
    println(response)
  }

  test("Copy files from shared folder") {
    val srcDir = conf.sourceDir
    val destDir = conf.out
    val processedDir = conf.processed

    val n = FileUtils.copyFiles(srcDir, destDir, processedDir, 10)

    println(n)
  }


}
