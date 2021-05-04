package acumen.interpreters.enclosure.limbo

import acumen.interpreters.enclosure.EnclosureInterpreterCallbacks
import acumen.interpreters.enclosure.HybridSystem
import acumen.interpreters.enclosure.Interval
import acumen.interpreters.enclosure.Interval.toInterval
import acumen.interpreters.enclosure.Types.UncertainState
import acumen.interpreters.enclosure.Util
import acumen.interpreters.enclosure.affine.UnivariateAffineEnclosure

trait HybridSolver extends AtomicStep {

  def solver(
    H: HybridSystem, // system to simulate
    T: Interval, // time segment to simulate over
    Ss: Set[UncertainState], // initial modes and initial conditions
    delta: Double, // parameter of solveVt
    m: Int, // parameter of solveVt
    n: Int, // maximum number of Picard iterations in solveVt
    degree: Int,
    K: Int, // maximum event tree size in solveVtE
    d: Double, // minimum time step size
    e: Double, // maximum time step size
    minComputationImprovement: Double, // minimum improvement of enclosure
    output: String, // path to write output 
    cb: EnclosureInterpreterCallbacks): Seq[UnivariateAffineEnclosure] = {
    Util.newFile(output)
    cb.endTime = T.hiDouble
    solveHybrid(H, delta, m, n, degree, K, output, cb)(d, minComputationImprovement)(Ss, T).get._1
  }

  def solveHybrid(
    H: HybridSystem,
    delta: Double,
    m: Int,
    n: Int,
    degree: Int,
    K: Int,
    output: String,
    cb: EnclosureInterpreterCallbacks)(
      minTimeStep: Double,
      minComputationImprovement: Double)(
        us: Set[UncertainState],
        t: Interval): MaybeResult = {
    val maybeResultT = atomicStep(H, delta, m, n, degree, K, output, cb.log)(us, t)
    if (t.width lessThanOrEqualTo minTimeStep) maybeResultT
    else maybeResultT match {
      case None => subdivideAndRecur(H, delta, m, n, degree, K, output, cb)(minTimeStep, minComputationImprovement)(us, t)
      case Some(resultT) =>
        val maybeResultTR = subdivideOneLevelOnly(H, delta, m, n, degree, K, output, cb.log)(us, t)
        if (bestOf(minComputationImprovement)(resultT, maybeResultTR) == resultT) {
          cb.sendResult(resultT._1)
          Some(resultT)
        } else subdivideAndRecur(H, delta, m, n, degree, K, output, cb)(minTimeStep, minComputationImprovement)(us, t)
    }
  }

  def subdivideAndRecur(
    H: HybridSystem,
    delta: Double,
    m: Int,
    n: Int,
    degree: Int,
    K: Int,
    output: String,
    cb: EnclosureInterpreterCallbacks)(
      minTimeStep: Double,
      minComputationImprovement: Double)(
        us: Set[UncertainState],
        t: Interval): MaybeResult =
    solveHybrid(H, delta, m, n, degree, K, output, cb)(minTimeStep, minComputationImprovement)(us, t.left) match {
      case None => None
      case Some((esl, usl)) =>
        solveHybrid(H, delta, m, n, degree, K, output, cb)(minTimeStep, minComputationImprovement)(usl, t.right) match {
          case None => None
          case Some((esr, usr)) =>
            Some((esl ++ esr, usr))
        }
    }

  def subdivideOneLevelOnly(
    H: HybridSystem,
    delta: Double,
    m: Int,
    n: Int,
    degree:Int, 
    K: Int,
    output: String,
    log: String => Unit)(
      us: Set[UncertainState],
      t: Interval): MaybeResult =
    atomicStep(H, delta, m, n, degree, K, output, log)(us, t.left) match {
      case None => None
      case Some((es, usl)) => atomicStep(H, delta, m, n, degree, K, output, log)(usl, t.right)
    }

}


