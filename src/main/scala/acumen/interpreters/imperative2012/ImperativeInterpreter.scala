package acumen
package interpreters
package imperative2012

import scala.collection.immutable.HashMap

import acumen.Errors._
import acumen.Pretty._
import acumen.util.Conversions._
import acumen.util.Random
import acumen.interpreters.Common.{ 
  classDef, evalOp, initStoreInterpreter, deviceClass, magicClass, visibleParametersMap 
}
import acumen.util.Canonical.{
  childrenOf, 
  classf,
  cmain,
  cmagic,
  endTime,
  nextChild,
  parentOf,
  parent,
  seedOf,
  seed1,
  seed2,
  self,
  resultType,
  time,
  timeStep
}
import scala.annotation.tailrec

class ImperativeInterpreter extends CStoreInterpreter {
  import Common._

  type Store = Common.Store
  def repr (s:Store) : CStore = Common.repr(s)
  def fromCStore (cs:CStore, root:CId) : Store = Common.fromCStore(cs, root)
  val initStepType = Discrete
  val timeStep = 0.01
  val outputRows = "WhenChanged"
  override def visibleParameters = visibleParametersMap(initStoreInterpreter(initStep = initStepType, initTimeStep = timeStep, initOutputRows = outputRows, isImperative = true))
   /* Identity lift function */
  def identLift(p:Prog) = p 
  def lift = identLift
  def init(prog: Prog): (Prog, Store, Metadata) = {
    val magic = fromCStore(initStoreInterpreter(initStep = initStepType, initTimeStep = timeStep, initOutputRows = outputRows, isImperative = true), CId(0))
    /* WARNING: the following line works because there is no children access check
       if one of the instructions of the provate section tries to access magic,
       and there was a check, this would crash (which we don't want) */
    val (sd1, sd2) = Random.split(Random.mkGen(0))
    val mainObj = mkObj(cmain, prog, None, sd1, List(VObjId(Some(magic))), magic, 1)
    magic.seed = sd2
    changeParent(magic, mainObj)
    val cprog = CleanParameters.run(prog, CStoreInterpreterType)
    val sprog = Simplifier.run(cprog)
    val mprog = Prog(magicClass :: deviceClass :: sprog.defs)
    (mprog , mainObj, NoMetadata)
  }

  def localStep(p: Prog, st: Store): ResultType = {
    val magic = getSimulator(st)
    stepInit
    if (getTime(magic) > getEndTime(magic)) {
      null
    } else {
      val chtset = traverse(evalStep(p, magic), st)
      val rt = getResultType(magic) match {
        case Discrete | Continuous =>
          chtset match {
            case SomeChange(dead, rps) =>
              for ((o, p) <- rps)
                changeParent(o, p)
              for (o <- dead) {
                o.parent match {
                  case None => ()
                  case Some(op) =>
                    for (oc <- o.children) changeParent(oc, op)
                    op.children = op.children diff Seq(o)
                }
              }
              Discrete
            case NoChange() =>
              FixedPoint
          }
        case FixedPoint =>
          setTime(magic, getTime(magic) + getTimeStep(magic))
          Continuous
      }
      setResultType(magic, rt)
      rt
    }
  }

  def step(p: Prog, st: Store, md: Metadata): StepRes = {
    val res = localStep(p, st)
    if (res == null) Done(NoMetadata, Double.NaN)
    else Data(st, NoMetadata)
  }

  // always returns the last known step, the adder callback is used to
  // determine when teh simulation is done
  override def multiStep(p: Prog, st: Store, md: Metadata, adder: DataAdder): (Store, Metadata, Double) = {
    val magic = getSimulator(st)
    var shouldAddData = ShouldAddData.IfLast
    // ^^ set to IfLast on purpose to make things work
    @tailrec def step0() : Unit = {
      val res = localStep(p, st)
      stepInit
      if (res == null) {
        if (shouldAddData == ShouldAddData.IfLast)
          addData(st, adder)
        adder.noMoreData()
      } else {
        shouldAddData = adder.newStep(res)
        if (shouldAddData == ShouldAddData.Yes)
          addData(st, adder)
        if (adder.continue)
          step0()
      }
    }
    step0()
    (st, NoMetadata, Double.NaN)
  }

  def addData(st: Store, adder: DataAdder) : Unit = {
    // Note: Conversion to a CStore just to add the data is certainly
    // not the most efficient way to go about things, but for now it
    // will do. --kevina
    adder.addData(st.id, st.fields)
    st.children.foreach { child => addData(child, adder) }
  }

  def stepInit : Unit = {}
  def traverse(f: ObjId => Changeset, root: ObjId): Changeset =
    traverseSimple(f, root)

}

// The ImperativeInterpreter is stateless hence it is okay to have a
// single global instance
object ImperativeInterpreter extends ImperativeInterpreter
