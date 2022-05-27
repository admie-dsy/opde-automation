

package com.ipto.opdefx.ui

import com.ipto.opdefx.concurrency.FiberSystem
import com.ipto.opdefx.cron.ScheduleExecutor
import com.ipto.opdefx.db.MessageDB
import com.ipto.opdefx.provider.ConfigProvider
import com.ipto.opdefx.requests.RequestBroker
import com.ipto.opdefx.util.FileUtils
//import javax.swing.table.DefaultTableModel
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

import scala.swing._
import zio._
import zio.console._
import zio.duration._

import sys.process._

class MainUI extends MainFrame{

  private val conf = ConfigProvider.newInstance("app.properties")
  private val script = conf.script
  private val runtime = Runtime.default
  private val db = new MessageDB(conf)

  title = "OPDE Automaton"
  preferredSize = new Dimension(1024, 384)

  private val schedule = com.ipto.opdefx.cron.Schedule("00,30 * * * *", DateTimeZone.forID("Europe/Athens"))
  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def build():ZIO[zio.clock.Clock, Nothing, Unit] = {
    for {
      flag <- Ref.make(0)
      messageQueue <- Queue.bounded[String](100)
      textArea = new TextArea() {
        editable = false
      }

      dbArea = new TextArea() {
        editable = false
      }

      updateArea = for {
        message <- messageQueue.take
        _ <- ZIO.succeed{
          textArea.append(message)
          if (textArea.text.split("\n").length > 50) textArea.text = ""
        }
        _ <- ZIO.sleep(2.seconds)
      } yield ()

      /*table = new Table(4, 3){
        peer.setModel(new DefaultTableModel {

          private val columnNames = Array[String]("Filename", "Process Time", "File Type")

          override def getRowCount: Int = 4

          override def getColumnCount: Int = 3

          override def getColumnName(columnIndex: Int): String = columnNames(columnIndex)

          override def isCellEditable(row: Int, column: Int): Boolean = false

        })
      }*/

      label = new Label(" Next run: ")

      nextRunField = new TextArea{
        columns = 40
        rows = 1
        text = s" ${fmt.print(schedule.getNextAfter(DateTime.now()))} "
        editable = false
      }

      //tableModel = table.peer.getModel.asInstanceOf[DefaultTableModel]

      executor = new ScheduleExecutor(schedule,
        () => {
          println("Doing stuff")
          //val scriptResult = s"python $script".!
          val filenames = FileUtils.copyFiles(conf.sourceDir, conf.out, conf.processed, offset = 20)
          runtime.unsafeRun{
            for {
              _ <- ZIO.foreach(filenames)(f=>db.insertReady(f))
            } yield ()
          }
          val nextRun = fmt.print(schedule.getNextAfter(DateTime.now()))
          nextRunField.text = nextRun
        }
      )

      _ <- ZIO.succeed {
        contents = new GridBagPanel {

          def constraints(x: Int, y: Int, gridwidth: Int = 1, gridheight: Int = 1, weightx: Double = 0.0, weighty: Double = 0.0, fill: GridBagPanel.Fill.Value = GridBagPanel.Fill.None): Constraints = {
            val c = new Constraints()
            c.gridx = x
            c.gridy = y
            c.gridwidth = gridwidth
            c.gridheight = gridheight
            c.weightx = weightx
            c.weighty = weighty
            c.fill = fill
            c
          }

          add(Button("Show Props") {
            val provider = ConfigProvider.newInstance("app.properties")
            Dialog.showMessage(contents.head, provider.toString)
          }, constraints(0, 0, fill = GridBagPanel.Fill.Both))

          add(Button("Start System"){
            runtime.unsafeRunAsync_(for {
              _ <- ZIO.succeed(textArea.append("Starting System\n"))
              _ <- flag.set(1)
              value <- flag.get
              _ <- putStrLn(s"$value")
              _ <- FiberSystem.run(flag, messageQueue)
            } yield ())
          }, constraints(1, 0, fill = GridBagPanel.Fill.Both))

          add(Button("Start Scheduler"){
            runtime.unsafeRunAsync_(for {
              _ <- ZIO.succeed{
                try {
                  executor.start()
                } catch {
                  case _: java.util.concurrent.RejectedExecutionException =>
                    Dialog.showMessage(contents.head, "Executor cannot be rescheduled. Please restart the application")
                }
              }
            } yield ())
          },
            constraints(2, 0, fill = GridBagPanel.Fill.Both)
          )

          add(Button("Stop System"){
            runtime.unsafeRunAsync_(for {
              _ <- ZIO.succeed(executor.shutdown())
              _ <- flag.set(0)
              value <- flag.get
              _ <- putStrLn(s"$value")
            } yield ())
          }, constraints(3, 0, fill = GridBagPanel.Fill.Both))

          add(label, constraints(4, 0, fill = GridBagPanel.Fill.Both))

          add(nextRunField, constraints(5, 0, fill = GridBagPanel.Fill.Both))

          add(Button("Show Progress"){

          },
            constraints(6, 0, fill = GridBagPanel.Fill.None))

          add(Button("Connectivity Test") {
            runtime.unsafeRunAsync_(for {
              response <- RequestBroker.getConnectionStatus(conf.connectivityEndpoint)
              _ = dbArea.append(response)
            } yield ())
          }, constraints(7, 0, fill = GridBagPanel.Fill.None))

          add(Button("Exit"){
            runtime.unsafeRunAsync_(for {
              flagValue <- flag.get
              _ = if (flagValue == 1) flag.set(0)
              _ = Thread.sleep(1000)
              _ <- ZIO.succeed(System.exit(0))
            } yield ())
          }, constraints(8, 0, fill = GridBagPanel.Fill.None))

          add(new ScrollPane(textArea), constraints(0, 1,
            gridheight = 3,
            gridwidth = 9,
            weighty = 1.0,
            weightx = 1.0,
            fill = GridBagPanel.Fill.Both))

          add(new Separator(), constraints(0, 2,
            gridwidth = 9,
            weighty = 1.0,
            weightx = 1.0,
            fill = GridBagPanel.Fill.Horizontal
          ))

          add(new ScrollPane(dbArea), constraints(0, 3,
            gridheight = 3,
            gridwidth = 9,
            weighty = 1.0,
            weightx = 1.0,
            fill = GridBagPanel.Fill.Both))
        }

      }

      updateFiber <- updateArea.forever.fork

      _ <- updateFiber.join

    } yield ()
  }

}

