package acumen.ui

import java.io._
import java.util.Date
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.net.URL


object Files {
 
  private val _home = System.getProperty("user.home") + File.separator + ".acumen"
  private val _saved = _home + File.separator + "autoSaved"
  private def _now = _saved + File.separator + timeTag + ".acm"

  val currentDir = {
    val md = new File("examples")
    if (md.exists()) md
    else new File(".")
  }

  val LibDir = {
    val md = new File("stdlib")
    if (md.exists()) md
    else new File(".")
  }
 
 /* Question:  How come the absolute pate of the new File is 
               the correct one automatically ? In my case 
               /Users/yingfuzeng/Desktop/work/newAcumen/acumen/_3D  */
  val _3DDir = {
	val md = new File("_3D")
    if (md.exists()) md
    else{ md.mkdirs; md } 
  }
  private val home = new File(_home)
  private val saved = new File(_saved)
  private def now = new File(_now)

  private def timeTag = {
    val dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")
    val date = new Date()
    dateFormat.format(date)
  }

  def acumenDir : File = {
    if (!home.exists) home.mkdirs
    home
  }

  def autoSavedDir : File = {
    if (!saved.exists) saved.mkdirs
    saved
  }

  def getFreshFile : File = {
    if (!saved.exists) saved.mkdirs
    now
  }

  def getFreshFile(prefix:File, ext:String) : File = {
		new File(prefix.getAbsolutePath + File.separator + timeTag + "." + ext)
  }
}


// vim: set ts=2 sw=2 et:
