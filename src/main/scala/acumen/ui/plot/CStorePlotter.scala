package acumen
package ui
package plot

import java.awt.{BasicStroke, Color}
import scala.collection.JavaConversions._
import scala.collection.mutable.{Buffer,Map,HashMap,HashSet,ArrayBuffer}
import org.jfree.chart._
import org.jfree.chart.plot._
import org.jfree.chart.renderer.xy.XYDifferenceRenderer
import org.jfree.chart.renderer.xy.XYDotRenderer
import org.jfree.data.xy._
import org.jfree.ui.ApplicationFrame
import swing.Swing

import acumen.interpreters.enclosure._
import acumen.ui.interpreter.{PlotParms, Plottable,PlotDiscrete,PlotDoubles,PlotEnclosure,PlotModel}
import acumen.util.Canonical._
import acumen.util.Conversions._
import Errors._

case object TooManySubplots extends Exception

/**
 *  The plotter enabled by the --newplot argument.
 *  (uses jfreechart) 
 */
class CStorePlotter extends JFreePlotter {

  def renderer(color: Color) = {
    val ren = new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer(true,false)
    ren.setPaint(color)
    ren
  }
  
  def discreteRenderer(color: Color) = {
    val ren = new org.jfree.chart.renderer.xy.XYDotRenderer()
    ren.setDotWidth(2)
    ren.setDotHeight(2)
    ren.setPaint(color)
    ren
  }

  def addToPlot(d: Object) = try {
    //combinedPlot.setNotify(false)
    println("Adding data to plot")
    val model = d.asInstanceOf[PlotModel]
    try {
      for (toPlot <- model.getPlottables(PlotParms())) {
        addDataHelper(model, toPlot)
      }
      //combinedPlot.setNotify(true)
      for (ds <- dataSets.values) {
        ds.fireSeriesChanged()
      }
    } finally {
      lastFrame = model.getRowCount
    }
    //for (p <- subPlotsList) {
    //  p.notifyListeners(new org.jfree.chart.event.PlotChangeEvent(p))
    //}
    //combinedPlot.notifyListeners(new org.jfree.chart.event.PlotChangeEvent(combinedPlot))
  } catch {
    case TooManySubplots =>
  }

  val dataSets = new HashMap[Int,XYSeries]

  private def newSubPlot(legendLabel: String, idx: Int, isDiscrete: Boolean) = { // FIXME: rename
    if (!App.ui.jPlotI.forsedDisable && subPlotsList.size > 24) {
      println("Too Many Subplots, Disabling!")
      App.ui.jPlotI.enabled = false
      App.ui.jPlotI.tooSlow = true
      App.ui.jPlotI.forsedDisable = true
      App.ui.newPlotView.fixPlotState
      //App.ui.console.log("Too Many Subplots!  Disable New Plot Tab...")
      throw TooManySubplots
    }
    val p = initXYPlot(legendLabel)
    val s = new XYSeries(legendLabel,false,true)
    val sc = new XYSeriesCollection(s)
    p.setDataset(sc)
    if (isDiscrete) {
      /* Changing to discrete renderer forces 
       * discrete plot to be scattered points.
       * It slows down the plotting and causes
       * Live Plotting Disabled from
       * JFreePlotTab.scala.
       */
      //p.setRenderer(discreteRenderer(Color.red))
      p.setRenderer(renderer(Color.red))
    } else {
      p.setRenderer(renderer(Color.red))
    }
    combinedPlot.add(p, 1)
    combinedPlot.getDomainAxis setUpperMargin 0
    subPlotsList += p
    s
  }
  
  /** 
   *  The plotter receives the data in chunks,
   *  lastFrame keeps track at which frame the 
   *  most recent chunk has been finished.
   */
  var lastFrame = 0


  private def addDataHelper(model: PlotModel, toPlot: Plottable) = {
    val times = model.getTimes
    /* Keep the possibility of using a different XYPlot & Renderer 
     * instance for discrete plots by passing a Boolean flag. */
    val series = dataSets.getOrElseUpdate(toPlot.column, 
                                          newSubPlot(model.getPlotTitle(toPlot.column), toPlot.column, 
                                              toPlot match {
                                                case tP: PlotDiscrete => true
                                                case _ => false } 
                                          ))

    val offset = toPlot.startFrame
    series.setNotify(false)

    toPlot match {

      case tP: PlotDiscrete =>
         /* In the case of a DiscretePlot, build a DiscretePath
          * not just from the most recent chunk, 
          * but from the whole data set. */
         val lines = new DiscretePathBuilder
         for (i <- 0 until tP.values.size) {
          tP.values(i) match {
            case VLit(GStr(str)) => 
              lines.add(times(offset + i),Set(str))
            case VLit(e:GDiscreteEnclosure[String]) => 
              throw NewPlotEnclosureError()
            case VLit(GInt(n)) => 
              lines.add(times(offset + i),Set(n.toString))
          }
         // Sort the path lexicographically
         val orderedSeries = lines.sortValues(Some((a,b) => a < b))
         // Clear the old plot as the new order might override the previous one 
         series.clear()
         // Add the new set of points from the path
         orderedSeries.foreach{point => series.add(point(0).x, point(0).y, true) }
        }

      case tP: PlotDoubles =>
        /* In the case of a DoublesPlot, we only 
         * process the data that comes from and after
         * the lastFrame.  */
        for (i <- scala.math.max(lastFrame - offset,0) until tP.values.size)
          series.add(times(offset + i),tP.values(i),true)

      case tP: PlotEnclosure =>
        throw NewPlotEnclosureError()
    }
    
    series.setNotify(true)
  }

  override def resetPlot = {
    super.resetPlot
    dataSets.clear
    lastFrame = 0
  }

}

