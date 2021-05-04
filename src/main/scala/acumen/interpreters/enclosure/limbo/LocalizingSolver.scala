package acumen.interpreters.enclosure.limbo

import acumen.interpreters.enclosure._
import acumen.interpreters.enclosure.Types._
import acumen.interpreters.enclosure.affine.UnivariateAffineEnclosure
import acumen.interpreters.enclosure.Expression.lift
import acumen.interpreters.enclosure.Interval.toInterval
import acumen.interpreters.enclosure.event.tree.Solver

trait LocalizingSolver extends SolveVt {

  /**
   * Returns a piece-wise enclosure of the solutions to the hybrid system.
   *
   * 1. Solve the ODE IVP for each mode m in 'uncertainModes' to get interval
   * functions f_m
   * 2. Use f_m to find a sub-segment time_m of 'time' that contains the first event in
   * 'time'
   * 3. Unify time_m for all m to get time'
   */
  def solver(
    system: HybridSystem, // system to simulate
    time: Interval, // time segment to simulate over
    uncertainStates: Set[UncertainState], // initial modes and initial conditions
    initialConditionPadding: Double, // parameter of solveVt
    picardImprovements: Int, // parameter of solveVt
    maxPicardIterations: Int, // maximum number of Picard iterations in solveVt
    maxEventTreeSize: Int, // maximum event tree size in solveVtE
    minTimeStep: Double, // minimum time step size
    maxTimeStep: Double, // maximum time step size
    splittingDegree: Int, // initial condition splitting degree
    outputFile: String, // path to write output 
    cb: EnclosureInterpreterCallbacks): Seq[UnivariateAffineEnclosure] = {
    // get an enclosure for each uncertain state
    val enclosures = uncertainStates.toSeq.flatMap(us => piecewisePicard(system.fields(us.mode), initialConditionPadding, picardImprovements, maxPicardIterations, minTimeStep, maxTimeStep, splittingDegree, outputFile, cb)(us.initialCondition, time))
    enclosures
  }

  /**
   * Solves the ODE-IVP over the time segment by bisection until the enclosure width
   * at the end time is no longer improved.
   */
  def piecewisePicard(
    field: Field,
    initialConditionPadding: Double,
    picardImprovements: Int,
    maxPicardIterations: Int,
    minTimeStep: Double,
    maxTimeStep: Double,
    splittingDegree: Int,
    outputFile: String,
    cb: EnclosureInterpreterCallbacks)(initialCondition: Box, segment: Interval): Seq[UnivariateAffineEnclosure] = {
    def piecewisePicardHelper = piecewisePicard(field, initialConditionPadding, picardImprovements, maxPicardIterations, minTimeStep, maxTimeStep, splittingDegree, outputFile, cb)_
    if (segment.width greaterThan maxTimeStep) {
      val (leftSegment, rightSegment) = segment.split
      val leftEnclosures = piecewisePicardHelper(initialCondition, leftSegment)
      val rightInitialCondition = leftEnclosures.last(leftSegment.high)
      val rightEnclosures = piecewisePicardHelper(rightInitialCondition, rightSegment)
      leftEnclosures ++ rightEnclosures
    }
    else {
      val enclosure = solveVt(field, segment, initialCondition, initialConditionPadding, picardImprovements, maxPicardIterations, splittingDegree)
      if (segment.width lessThan minTimeStep * 2) {
        println("minimum step size at " + segment)
        Seq(enclosure)
      }
      else {
        val (leftSegment, rightSegment) = segment.split
        val leftEnclosure = solveVt(field, leftSegment, initialCondition, initialConditionPadding, picardImprovements, maxPicardIterations, splittingDegree)
        var rightInitialCondition = leftEnclosure(leftSegment.high)
        val rightEnclosure = solveVt(field, rightSegment, rightInitialCondition, initialConditionPadding, picardImprovements, maxPicardIterations, splittingDegree)
        if (rightEnclosure(segment.high).l1Norm lessThan enclosure(segment.high).l1Norm) {
          val leftEnclosures = piecewisePicardHelper(initialCondition, leftSegment)
          val rightInitialCondition = leftEnclosures.last(leftSegment.high)
          val rightEnclosures = piecewisePicardHelper(rightInitialCondition, rightSegment)
          leftEnclosures ++ rightEnclosures
        }
        else {
          println("optimal enclosure at " + segment)
          Seq(enclosure)
        }
      }
    }
  }

}

object LocalizingSolverApp extends LocalizingSolver with App {
  implicit val rnd = Parameters.default.rnd
  val initalCondition = Box(
    "x" -> Interval.one,
    "x'" -> Interval.zero)
  val field = Field(Map(
    "x" -> Variable("x'"),
    "x'" -> -(0.5 * Variable("x'") + Variable("x"))))
  println(field)
  val time = Interval(0, 4)
  val minTimeStep = 0.01
  val maxTimeStep = 1
  val result = piecewisePicard(field, 0, 20, 200, minTimeStep, maxTimeStep, 1, "output", Solver.defaultCallback)(initalCondition, time)
  val plotter = new acumen.ui.plot.EnclosurePlotter
  plotter.plot("x'' = -x'/2 - x")(null)(result)
}
