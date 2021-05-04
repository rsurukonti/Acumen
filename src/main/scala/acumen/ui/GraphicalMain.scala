package acumen
package ui

import java.lang.Thread

import scala.actors._
import collection.JavaConversions._

import java.awt.Font
import java.awt.Color
import java.awt.RenderingHints
import java.awt.GraphicsEnvironment
import java.awt.Desktop
import java.io._
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.undo._
import javax.swing.text._
import javax.swing.KeyStroke
import javax.swing.event.DocumentListener
import javax.swing.event.DocumentEvent

import swing._
import swing.event._

object GraphicalMain extends SimpleSwingApplication {
  // Swing debugging
  //javax.swing.RepaintManager.setCurrentManager(new debug.CheckThreadViolationRepaintManager)
  debug.EventDispatchThreadHangMonitor.initMonitoring()

  // Approximation of the amount of heap available when setting heap size to 1G
  val MIN_MAX_MEM = (1024*7/8)*1024*1024

  def maybeFork(args: Array[String]) {
    val maxMem = Runtime.getRuntime().maxMemory()
    val shouldFork = maxMem < MIN_MAX_MEM
    val mayFork = !Main.dontFork
    if (shouldFork && mayFork) {
      val separator = System.getProperty("file.separator");
      val classpath = System.getProperty("java.class.path");
      val path = System.getProperty("java.home") + separator + "bin" + separator + "java";
      val realArgs = new java.util.ArrayList[String]();
      val bean = java.lang.management.ManagementFactory.getRuntimeMXBean();
      realArgs.add(path)
      realArgs.addAll(bean.getInputArguments())
      realArgs.add("-Xmx1g")
      realArgs.add("-cp")
      realArgs.add(classpath)
      realArgs.add("acumen.Main")
      realArgs.addAll(args.toList)
      realArgs.add("--dont-fork")
      val processBuilder = new ProcessBuilder(realArgs)
      System.err.println("Forking new JVM with: " + processBuilder.command.mkString(" "))
      System.err.println("  to avoid use --dont-fork or start java with -Xmx1g")
      var rv = 255
      try {
        val process = processBuilder.start();
        inheritIO(process.getInputStream(), System.out, magicStartString)
        inheritIO(process.getErrorStream(), System.err, null)
        rv = process.waitFor()
        if (rv == 0)
          sys.exit(0);
        System.err.println("Fork failed with exit code: " + rv);
      } catch {
        case e => System.err.println("Fork failed with error: " + e.getMessage())
      }
      if (foundIt)
        sys.exit(rv)
      else
        System.err.println("Continuing anyway, acumen may be slow...")
    }
  }

  val magicStartString = "Acumen Started."

  var foundIt = false;

  def inheritIO(src0:InputStream, dest:PrintStream, scanFor0:String) {
    var scanFor = scanFor0
    def src = new BufferedReader(new InputStreamReader(src0))
    new Thread(new Runnable() {
      def run() {
        var line:String = null
        while ({line = src.readLine; line != null}) {
          if (scanFor != null && line == scanFor) {
            foundIt = true
            scanFor = null
          }
          dest.println(line)
        }
      }
    }).start();
  }

  override def main(args: Array[String]) {
    if (Main.positionalArgs.size() > 1)
      Main.openFile = Main.checkFile(Main.positionalArgs(1))
    maybeFork(args)
    super.main(args)
  }

  def top = {
    try {
      App.init
    } catch {
      case e: Errors.AcumenError =>
        System.err.println(e.getMessage)
        System.exit(1)
    }
    val ret = App.ui.top
    println(magicStartString) // Do not remove, needed by forking code
    ret
  }
}

