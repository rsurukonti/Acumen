package acumen.interpreters.enclosure.event.tree

import acumen.interpreters.enclosure.Interval.min
import acumen.interpreters.enclosure.Interval.toInterval
import acumen.interpreters.enclosure.Types.M
import acumen.interpreters.enclosure.Types.UncertainState
import acumen.interpreters.enclosure.Types.endTimeInterval
import acumen.interpreters.enclosure.EnclosureInterpreterCallbacks
import acumen.interpreters.enclosure.Interval
import acumen.interpreters.enclosure.Util
import acumen.interpreters.enclosure.event.pwl.PWLEventEncloser
import acumen.interpreters.enclosure.affine.UnivariateAffineEnclosure
import acumen.interpreters.enclosure.HybridSystem

/**
 * Old implementation of the event tree based hybrid solver.
 *
 * Contains an implementation of the "simple recursive" time
 * subdivision strategy. It may be useful to factor out this
 * strategy and use it to give an alternative to the localizing
 * one, as an example of what needs to be done to implement a
 * strategy, and as an example of one that is too simplistic
 * (it is sensitive to parameters of the simulation, such as
 * endTime and gives simulation output that is hard to analyze.)
 */
trait Solver extends TreeEventEncloser {

  case class SolverException(message: String) extends Exception
  case class Aborted(resultSoFar: Seq[UnivariateAffineEnclosure]) extends Exception

  // TODO add description
  def solver(
    H: HybridSystem, // system to simulate
    T: Interval, // time segment to simulate over
    Ss: Set[UncertainState], // initial modes and initial conditions
    delta: Double, // parameter of solveVt
    m: Int, // parameter of solveVt
    n: Int, // maximum number of Picard iterations in solveVt
    degree: Int, // number of pieces to split each initial condition interval
    K: Int, // maximum event tree size in solveVtE
    d: Double, // minimum time step size
    e: Double, // maximum time step size
    minComputationImprovement: Double, // minimum improvement of enclosure
    cb: EnclosureInterpreterCallbacks): Seq[UnivariateAffineEnclosure] = {
    cb.endTime = T.hiDouble
    solveHybrid(H, T, Ss, delta, m, n, degree, K, d, e, minComputationImprovement, cb)._2
  }

  // TODO add description
  def solveHybrid(
    H: HybridSystem, // system to simulate
    T: Interval, // time segment to simulate over
    Ss: Set[UncertainState], // initial modes and initial conditions
    delta: Double, // padding for initial condition in solveVt
    m: Int, // number of extra Picard iterations in solveVt
    n: Int, // maximum number of Picard iterations in solveVt
    degree: Int, // number of pieces to split each initial condition interval
    K: Int, // maximum event tree size in solveVtE, gives termination condition for tree enlargement
    d: Double, // minimum time step size
    e: Double, // maximum time step size
    minComputationImprovement: Double, // minimum improvement of enclosure
    cb: EnclosureInterpreterCallbacks // carrier for call-backs for communicating with the GUI
    ): (Set[UncertainState], Seq[UnivariateAffineEnclosure]) = {
    //    println("solveHybrid: on so" + T)
    val mustSplit = T.width greaterThan e
    val (lT, rT) = T.split
    val cannotSplit = !(min(lT.width, rT.width) greaterThan d)
    if (mustSplit) {
      if (cannotSplit) {
        sys.error("gave up for minimum step size " + d + " at " + T)
      }
      else {
        //        cb.log("splitting " + T)
        val (ssl, ysl) = solveHybrid(H, lT, Ss, delta, m, n, degree, K, d, e, minComputationImprovement, cb)
        val (ssr, ysr) = solveHybrid(H, rT, ssl, delta, m, n, degree, K, d, e, minComputationImprovement, cb)
        (ssr, ysl ++ ysr)
      }
    }
    else {
      val onT = Ss.map(solveVtE(H, T, _, delta, m, n, degree, K, cb.log))
      if (onT contains None)
        if (cannotSplit) {
          sys.error("gave up for minimum step size " + d + " at " + T)
        }
        else {
          //        cb.log("splitting " + T)
          val (ssl, ysl) = solveHybrid(H, lT, Ss, delta, m, n, degree, K, d, e, minComputationImprovement, cb)
          val (ssr, ysr) = solveHybrid(H, rT, ssl, delta, m, n, degree, K, d, e, minComputationImprovement, cb)
          (ssr, ysl ++ ysr)
        }
      else {
        val resultForT @ (endStatesOnT, enclosuresOnT) =
          onT.map(_.get).foldLeft((Set[UncertainState](), Seq[UnivariateAffineEnclosure]())) {
            case ((resss, resys), (ss, ys)) => (resss ++ ss, resys ++ ys)
          }
        val ssT = M(endStatesOnT)

        val onlT = Ss.map(solveVtE(H, lT, _, delta, m, n, degree, K, cb.log))
        if (onlT contains None) {
          cb.sendResult(resultForT._2)
          //          println("solveHybrid: @" + T + ": " + resultForT._1.head.initialCondition) //  PRINTME
          resultForT
        }
        else {
          val (endStatesOnlT, enclosuresOnlT) =
            onlT.map(_.get).foldLeft((Set[UncertainState](), Seq[UnivariateAffineEnclosure]())) {
              case ((resss, resys), (ss, ys)) => (resss ++ ss, resys ++ ys)
            }
          val sslT = M(endStatesOnlT)

          val onrT = sslT.map(solveVtE(H, rT, _, delta, m, n, degree, K, cb.log))
          if (onrT contains None) {
            cb.sendResult(resultForT._2)
            resultForT
          }
          else {
            val (endStatesOnrT, enclosuresOnrT) =
              onrT.map(_.get).foldLeft((Set[UncertainState](), Seq[UnivariateAffineEnclosure]())) {
                case ((resss, resys), (ss, ys)) => (resss ++ ss, resys ++ ys)
              }
            val ssrT = M(endStatesOnrT)

            lazy val nowhereBetter = (endTimeInterval(ssrT) zip endTimeInterval(ssT)).forall {
              case ((_, l), (_, r)) => l.width greaterThanOrEqualTo r.width
            }
            lazy val somewhereWorse = (endTimeInterval(ssrT) zip endTimeInterval(ssT)).exists {
              case ((_, l), (_, r)) => l.width greaterThan r.width
            }
            //          lazy val noImprovement = nowhereBetter || somewhereWorse
            lazy val improvement = {
              val onT = endTimeInterval(ssT).values.foldLeft(Interval.zero) { case (res, i) => i.width + res }
              val onrT = endTimeInterval(ssrT).values.foldLeft(Interval.zero) { case (res, i) => i.width + res }
              onT - onrT
            }

            // TODO make the improvement threshold a parameter 
            if (cannotSplit ||
              {
                //              log("improvement : " + improvement);
                improvement lessThanOrEqualTo minComputationImprovement
              }) {
              cb.sendResult(resultForT._2)
              //              println("solveHybrid: logged results for " + T) //  PRINTME
              //              println("solveHybrid: " + resultForT._1.head.initialCondition) //  PRINTME
              //              println("solveHybrid: " + resultForT._2) //  PRINTME
              resultForT
            }
            else {
              //            cb.log("splitting " + T)
              val (ssl, ysl) = solveHybrid(H, lT, Ss, delta, m, n, degree, K, d, e, minComputationImprovement, cb)
              val (ssr, ysr) = solveHybrid(H, rT, ssl, delta, m, n, degree, K, d, e, minComputationImprovement, cb)
              //                            println("solveHybrid: @" + T + ": " + ssr.head.initialCondition)  //  PRINTME
              (ssr, ysl ++ ysr)
            }
          }
        }
      }
    }
  }

}

object Solver {
  def defaultCallback = new EnclosureInterpreterCallbacks {
    override def log(msg: String) = println(msg)
  }
}
