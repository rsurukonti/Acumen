package acumen
package interpreters
package enclosure2015

import enclosure2015.Common._
import interpreters.Common._
import util._
import util.Canonical._

/* DynSetEnclosure */

/** Public constructors for DynSetEnclosure.
 *  NOTE: These must ensure that the CStore of the DynSetEnclosure is  
 *        up to date with the underlying dynSet. */
object DynSetEnclosure {

  /** Update the CStore in enc w.r.t. v (also updating variables defined by eqs). */
  def apply(v: RealVector, enc: DynSetEnclosure, eqsInlined: Set[CollectedAction], evalExpr: (Expr,Env,EStore) => CValue)(implicit cValueIsReal: Real[CValue], parameters: Parameters): DynSetEnclosure = {
    val updatedSt = updateCStore(enc.st, v, eqsInlined, evalExpr, enc.indexToName, enc.nameToIndex)
    DynSetEnclosure(updatedSt, IntervalBox(v), enc.nameToIndex)
  }
  
  /** Initialize a DynSetEnclosure (including its underlying IntervalDynSet) from a CStore and a nameToIndex Map. */
  def apply(st: CStore, nameToIndex: Map[(CId,Name),Int])(implicit cValueIsReal: Real[CValue], parameters: Parameters): DynSetEnclosure = {
    val indexToName = nameToIndex.map(_.swap)
    def initialVector: RealVector = breeze.linalg.Vector.tabulate[CValue](indexToName.size) { 
      i => val (id, n) = indexToName(i)
           Canonical.getObjectField(id, n, st) match {
             case VLit(e: GConstantRealEnclosure) => VLit(GConstantRealEnclosure(e.range))
           }
    }
    val dynSet = parameters.dynSetType match {
      case `DynSetCuboid`      => Cuboid(initialVector)
      case `DynSetIntervalBox` => IntervalBox(initialVector)
      case _                   => throw new Errors.InvalidDynSet(parameters.dynSetType)
    }
    DynSetEnclosure(st, dynSet, nameToIndex)
  }
  
  /** Initialize a DynSetEnclosure (including its underlying IntervalDynSet) from a CStore. */
  def apply(st: CStore)(implicit cValueIsReal: Real[CValue], parameters: Parameters): DynSetEnclosure = 
    apply(st, buildNameToIndexMap(st)) // TODO when introducing indexing, this one needs to match on indices too
  
  /** Build a name-to-index map of variableNames, sorted lexicographically first by CId then by Name. */
  def buildNameToIndexMap(variableNames: Set[(CId,Name)]): Map[(CId,Name), Int] =
    variableNames.toList.sorted(Ordering.Tuple2[CId, Name]).zipWithIndex.toMap

  /** Build a name-to-index map of all real variables in st. */
  def buildNameToIndexMap(st: CStore): Map[(CId,Name), Int] = {
    val realVariables = st.toList.flatMap {
      case (id, co) => co.toList.flatMap {
        case (n, VLit(v: GConstantRealEnclosure)) => List((id, n))
        case (n, VLit(v: GTDif[_]))               => List((id, n))
        case (n, VLit(v: GFDif[_]))               => List((id, n))
        case (n, v)                               => Nil
      }
    }.toSet
    buildNameToIndexMap(realVariables)
  }
  
  /** Update variables in st defined by continuous assignments w.r.t. flowValues */
  def updateCStore
    ( st: CStore
    , flowValues: RealVector
    , eqsInlined: Set[CollectedAction]
    , evalExpr: (Expr, Env, EStore) => CValue
    , indexToName: Map[Int,(CId,Name)]
    , nameToIndex: Map[(CId,Name),Int] )
    ( implicit cValueIsReal: Real[CValue]
    ,          parameters: Parameters
    ): CStore = {
    def updateFlowVariables(unupdated: CStore): CStore =
      (0 until flowValues.size).foldLeft(unupdated) {
        case (tmpSt, i) =>
          val (id, n) = indexToName(i)
          Canonical.setObjectField(id, n, flowValues(i), tmpSt)
      }
    def updateEquationVariables(unupdated: CStore): CStore = {
      val flowStore = DynSetEnclosure(st, IntervalBox(flowValues), nameToIndex)
      eqsInlined.foldLeft(unupdated){ case (stTmp, ca) =>
        val rd = ca.lhs
        val cv = evalExpr(ca.rhs, ca.env, flowStore)
        Canonical.setObjectField(rd.id, rd.field, cv, stTmp)
      }
    }
    updateEquationVariables(updateFlowVariables(st))
  }

}

/** DynSetEnclosure wraps an IntervalDynSet and a CStore. The two maps
 *  nameToIndex and indexToName identify which CStore (id, n) pairs are 
 *  overridden by the data contained in the IntervalDynSet
 *  
 *  NOTE: Variables in st represented by dynSet are assumed to be up to
 *        date with dynSet!  
 */
case class DynSetEnclosure
  ( st                   : CStore
  , dynSet               : IntervalDynSet
  , nameToIndex          : Map[(CId, Name), Int] ) 
  ( implicit cValueIsReal: Real[CValue]
  ,          parameters: Parameters
  ) extends RichStore[DynSetEnclosure, CId] with Enclosure with EStore {
  
  assert( dynSet.size      == nameToIndex.size 
       && nameToIndex.size == indexToName.size,
       "Creation of DynSetEnclosure failed: dimensions mismatch.")

  assert( nameToIndex.size > 0, 
       "Creation of DynSetEnclosure failed: dimension is 0.")
       
  lazy val indexToName = nameToIndex.map(_.swap)
 
  val dim = nameToIndex.size
  
  /** Move the enclosure by the mapping m, returning range and image enclosures. */
  def move( eqsInlined : Set[CollectedAction]
          , flow       : C1Flow
          , evalExpr   : (Expr, Env, EStore) => CValue ) = {
      
    def updateCStore(flowValues: RealVector): CStore = 
      DynSetEnclosure.updateCStore(this.cStore, flowValues, eqsInlined, evalExpr, indexToName, nameToIndex)
    
    val (rangeDynSet, endDynSet) = dynSet move flow
        
    ( DynSetEnclosure( updateCStore(rangeDynSet)
                     , rangeDynSet                              
                     , nameToIndex )
    , DynSetEnclosure( updateCStore(endDynSet)
                     , endDynSet                              
                     , nameToIndex ) )
  }
  
  def mapping( c1map    : C1Mapping
             , evalExpr : (Expr, Env, EStore) => CValue ) = {

    def updateCStore(mapValues: RealVector): CStore = 
      DynSetEnclosure.updateCStore(this.cStore, mapValues, Set.empty[CollectedAction], evalExpr, indexToName, nameToIndex)
      
    val imageDynSet = dynSet mapping c1map
    
    DynSetEnclosure( updateCStore(imageDynSet)
                   , imageDynSet
                   , nameToIndex )
  } 

  override def contains(that: Enclosure): Boolean = that match {
    case dse: DynSetEnclosure =>
      val flowNames = indexToName.values.toSet
      val containsNonOdeVariables = contains(that, flowNames)
      containsNonOdeVariables && (this.dynSet.length == dse.dynSet.length) && (this.dynSet contains dse.dynSet)
    case oe =>
      throw internalError(s"Have not implemented check for containment of ${oe.getClass} in ${this.getClass}.")
  }
              
  /* Enclosure interface */
  
  def cStore = st
  
  def initialize(s: CStore): Enclosure = DynSetEnclosure(s)

  /** Apply m to all CValues in the CStore and Lohner set components */
  def map(m: CValue => CValue): DynSetEnclosure =
    DynSetEnclosure( st.mapValues(_ mapValues m)
                   , dynSet map m
                   , nameToIndex )
                               
  /** Apply m to all CValues in the CStore and Lohner set components with the 
   *  CId and Name of the value in context */
  override def mapName(m: (GId, Name, CValue) => CValue): DynSetEnclosure = 
    DynSetEnclosure( st.map{ case (cid,co) => (cid, co.map{ case (n,v) => (n, m(cid,n,v)) }) }
                   , dynSet.map((i: Int, v: CValue) => m(indexToName(i)._1, indexToName(i)._2, v))
                   , nameToIndex )

  override def setObject(id: CId, o: CObject): Enclosure = this updated (id, o)
  
  def updated(id: CId, o: CObject): DynSetEnclosure = 
    if (nameToIndex.keySet.filter( key => key._1 == id ).isEmpty) 
      DynSetEnclosure( st updated(id, o)
                     , dynSet 
                     , nameToIndex )
    else
      DynSetEnclosure( st updated(id, o) )


  /* EStore interface */

  override def setObjectField(id: CId, n: Name, v: CValue): DynSetEnclosure =
    nameToIndex.get(id, n) match {
      case Some(i) =>
        Logger.trace(s"Setting DynSet variable $id.${Pretty pprint n}.")
        DynSetEnclosure( super.setObjectField(id, n, v).cStore
                       , dynSet.set(i, v)
                       , nameToIndex )
      case None =>
        DynSetEnclosure( super.setObjectField(id, n, v).cStore
                       , dynSet
                       , nameToIndex )
    }
  
  override def getObjectField(id: CId, n: Name): CValue =
    nameToIndex.get(id, n) match {
      case Some(i) => dynSet(i)
      case None => super.getObjectField(id, n)
    }

  /* RichStore interface */
  
  override def apply(id: CId, n: Name): CValue = getObjectField(id, n)
  
  override def updated(id: CId, n: Name, v: CValue): DynSetEnclosure = 
    DynSetEnclosure( st.updated(id, st(id).updated(n, v))
                     , if (nameToIndex.isDefinedAt(id, n)) dynSet.set(nameToIndex(id, n), v) else
                                                           dynSet
                     , nameToIndex )
  
  override def +++(that: DynSetEnclosure): DynSetEnclosure = mapName( (id: GId, n: Name, v: CValue) => cValueIsReal.add(v, that(id.cid, n)) )
                       
  override def ***(that: Double): DynSetEnclosure = map( x => cValueIsReal.fromDouble(that) * x ) 
 }