package acumen
package interpreters
package reference2013

import Errors._
import util.Filters._
import util.Names._
import util.Canonical._
import scala.math._
import java.io.InputStreamReader
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.FunSuite
import java.io.File

class InterpreterTest extends FunSuite with ShouldMatchers {

  override def suiteName = "Reference 2013 Unit Tests"

  def semantics = SemanticsImpl.Ref2012

  /* tests that match theoretical values against the interpreter's values */
  type VarHistory = Stream[Tuple2[Double,Double]] 
  
  def oneVar(id:CId, x:String, h: Interpreter.History) : VarHistory = 
    h map (st => 
      (getTime(st), getObjectField(id, Name(x, 0), st)) match {
        case (t, VLit(GDouble(d))) => (t,d)
        case _  => throw VariableNotDeclared(Name(x,0))
      }
    )

  def matches(s:VarHistory, f:Double => Double) : Boolean = 
    if (s.isEmpty) true
    else {
      val (t,x) = s.head
      if (abs(f(t) - x) > 1e-2) false
      else matches(s.tail, f)
    }

  def parse(p:String, st:String) = 
    (semantics.applyRequiredPasses(Parser.run(Parser.prog, p)), 
     Parser.run(Parser.store, st))

  test("Gravity1d") {
    val progTxt =
      """
      model Simulator(time, timeStep, endTime, stepType) = initially
      model Ball(x, x', x'') = 
      initially 
      always
        self.x'' = -9.8
      
      model Main(mode, simulation) = initially
      """


    val storeTxt = 
      """
      #0 { className = Main,
           parent = none, 
           nextChild = 0 }

      #0.1 { className = Simulator,
             parent = #0, 
             time = 0.0, 
             timeStep = 0.001, 
             endTime = 2.0, 
             resultType = @Discrete,
             nextChild = 0 }

      #0.2 { className = Ball,
             parent = #0,
             x = 0.0,
             x' = 0.0,
             x'' = 0.0,
             nextChild = 0 }
      """

    val (prog, store) = parse(progTxt, storeTxt)

    val h = semantics.interpreter.lazyLoop(prog, store)
    val xs = oneVar(CId(2), "x", onlyAfterContinuous(h))

    def expected(t:Double) = - (9.8 / 2.0) * t * t 
    assert(matches(xs, expected))
  }

  test("Gravity2d") {
    val progTxt =
      """
      model Simulator(time, timeStep, endTime, stepType) = initially 
      model Ball(x, x', x'', y,y',y'') = 
        initially
        always
        self.y'' = -9.8,
        self.x'' = 0
      
      model Main(mode, simulation) = initially
      """

    val storeTxt = 
      """
      #0 { className = Main,
           parent = none,
           nextChild = 0 }

      #0.2 { className = Simulator,
             parent = #0, 
             time = 0.0, 
             timeStep = 0.001, 
             endTime = 2.0, 
             resultType = @Discrete,
             nextChild = 0 }

      #0.3 { className = Ball,
             parent = #0,
             x   = 0.0, 
             x'  = 1.0, 
             x'' = 0.0,
             y   = 0.0,
             y'  = 2.0,
             y'' = 0.0,
             nextChild = 0 }
      """

    val (prog, store) = parse(progTxt, storeTxt)

    val h = onlyAfterContinuous(semantics.interpreter.lazyLoop(prog, store))
    val xs = oneVar(CId(3), "x", h)
    val ys = oneVar(CId(3), "y", h)

    def expectedy(t:Double) = - (9.8 / 2.0) * t * t + 2.0 * t
    def expectedx(t:Double) = t
    assert(matches(xs, expectedx))
    assert(matches(ys, expectedy))
  }

}
