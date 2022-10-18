package com.ipto.opdefx.ui

import com.ipto.opdefx.concurrency.FiberSystem
import com.ipto.opdefx.cron.ScheduleExecutor
import com.ipto.opdefx.db.MessageDB
import com.ipto.opdefx.provider.ConfigProvider
import com.ipto.opdefx.requests.RequestBroker
import com.ipto.opdefx.util.FileUtils
import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.DateTimeFormat

import java.awt.Color
import java.awt.Font
import javax.imageio.ImageIO
import scala.swing._
import zio._
import zio.console._
import zio.duration._


class MainUI extends MainFrame{

  private val conf = ConfigProvider.newInstance("app.properties")
  private val runtime = Runtime.default
  private val db = new MessageDB(conf)

  title = "OPDE Scheduler"
  preferredSize = new Dimension(900, 350)
  iconImage = ImageIO.read(getClass.getResource("/logo.png"))

  private val schedule = com.ipto.opdefx.cron.Schedule("00,30 * * * *", DateTimeZone.forID("Europe/Athens"))
  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")

  def build():ZIO[zio.clock.Clock, Nothing, Unit] = {
    for {
      flag <- Ref.make(0)
      messageQueue <- Queue.bounded[String](100)
      textArea = new TextArea() {
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

      nextRunField = new TextArea{
        columns = 1
        rows = 1
        text = s" ${fmt.print(schedule.getNextAfter(DateTime.now()))} "
        editable = false
      }

      executor = new ScheduleExecutor(schedule,
        () => {
          println("Doing stuff")
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

          // GridBagPanel background
          val gridPanelBackground = new Color(206, 216, 225)
          background = gridPanelBackground


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

          // Button/Label Properties
          val buttonColor = new Color(93, 109, 126)
          val buttonForegroundColor: Color = Color.white
          val nextRunForegroundColor = new Color(52, 73, 94)
          val buttonFont = new Font("Dialog", Font.BOLD, 12)
          val labelFont = new Font("MONOSPACED", Font.BOLD, 14)

          // Show Props button
          var button = Button("Show Props") {
            val provider = ConfigProvider.newInstance("app.properties")
            Dialog.showMessage(contents.head, provider.toString)
          }
          button.background = buttonColor
          button.foreground = buttonForegroundColor
          button.font = buttonFont
          add(button, constraints(0, 0, fill = GridBagPanel.Fill.Both))

          // Start System button
          button = Button("Start System"){
            runtime.unsafeRunAsync_(for {
              _ <- ZIO.succeed(textArea.append("Starting System\n"))
              _ <- flag.set(1)
              value <- flag.get
              _ <- putStrLn(s"$value")
              _ <- FiberSystem.run(flag, messageQueue)
            } yield ())
          }
          button.background = buttonColor
          button.foreground = buttonForegroundColor
          button.font = buttonFont
          add(button, constraints(0, 1, fill = GridBagPanel.Fill.Both))

          // Start Scheduler button
          button = Button("Start Scheduler"){
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
          }
          button.background = buttonColor
          button.foreground = buttonForegroundColor
          button.font = buttonFont
          add(button, constraints(0, 2, fill = GridBagPanel.Fill.Both))

          // Stop System button
          button = Button("Stop System"){
            runtime.unsafeRunAsync_(for {
              _ <- ZIO.succeed(executor.shutdown())
              _ <- flag.set(0)
              value <- flag.get
              _ <- putStrLn(s"$value")
            } yield ())
          }
          button.background = buttonColor
          button.foreground = buttonForegroundColor
          button.font = buttonFont
          add(button, constraints(0, 3, fill = GridBagPanel.Fill.Both))

          // Show Progress button
          button = Button("Show Progress"){}
          button.background = buttonColor
          button.foreground = buttonForegroundColor
          button.font = buttonFont
          add(button, constraints(0, 4, fill = GridBagPanel.Fill.Both))

          // Connectivity Test button
          button = Button("Connectivity Test") {
            runtime.unsafeRunAsync_(for {
              response <- RequestBroker.getConnectionStatus(conf.connectivityEndpoint)
              _ = Dialog.showMessage(contents.head, response)
            } yield ())
          }
          button.background = buttonColor
          button.foreground = buttonForegroundColor
          button.font = buttonFont
          add(button, constraints(0, 5, fill = GridBagPanel.Fill.Both))

          // Exit button
          button = Button("Exit"){
            runtime.unsafeRunAsync_(for {
              flagValue <- flag.get
              _ = if (flagValue == 1) flag.set(0)
              _ = Thread.sleep(1000)
              _ <- ZIO.succeed(System.exit(0))
            } yield ())
          }
          button.background = buttonColor
          button.foreground = buttonForegroundColor
          button.font = buttonFont
          add(button, constraints(0, 6, fill = GridBagPanel.Fill.Both))

          // Next Run
          var label = new Label(" ")
          add(label, constraints(0, 7, fill = GridBagPanel.Fill.Both))
          label = new Label(" Next run ")
          label.foreground = nextRunForegroundColor
          label.font = labelFont
          add(label, constraints(0, 8, fill = GridBagPanel.Fill.Both))
          val gridpanelBackground = new Color(206, 216, 225)
          nextRunField.background = gridPanelBackground
          nextRunField.foreground = nextRunForegroundColor
          nextRunField.font = labelFont
          add(nextRunField, constraints(0, 9, fill = GridBagPanel.Fill.Both))

          // TextArea ScrollPanel
          add(new ScrollPane(textArea), constraints(1, 0,
            gridheight = 10,
            gridwidth = 10,
            weighty = 5.0,
            weightx = 5.0,
            fill = GridBagPanel.Fill.Both))
        }

      }

      updateFiber <- updateArea.forever.fork

      _ <- updateFiber.join

    } yield ()
  }

}