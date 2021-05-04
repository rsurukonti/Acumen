package acumen

import interpreters.enclosure.Interval
import util.ASTUtil
import acumen.interpreters.Common.RichStore
import acumen._

import reflect.runtime.universe.{
  typeOf, TypeTag
}

/**
 * Automatic Differentiation.
 *
 * Based on description from book "Validated Numerics" by
 * Warwick Tucker, pages 121 and 122
 * 
 * and on the note
 * "Automatic Differentiation Rules for Univariate Taylor Series" by
 * Ferenc A. Bartha
 */
object TAD extends App {

  /** Integral instance for TDif[V], where V itself has an Integral instance */
  abstract class TDifAsIntegral[V: Integral] extends DifAsIntegral[V,Int,TDif[V]] with Integral[TDif[V]] {
    /* Integral instance */
    def abs(x: TDif[V]): TDif[V] = throw Errors.NotImplemented("TDif.abs")
    def add(l: TDif[V], r: TDif[V]): TDif[V] =
      TDif(Stream.from(0).map(k => l(k) + r(k)), combinedLength(l, r))
    def sub(l: TDif[V], r: TDif[V]): TDif[V] =
      TDif(Stream.from(0).map(k => l(k) - r(k)), combinedLength(l, r))
    def neg(x: TDif[V]): TDif[V] =
      TDif(Stream.from(0).map(k => - x(k)), x.length)
    def mul(l: TDif[V], r: TDif[V]): TDif[V] =
      TDif(Stream.from(0).map(k => (0 to k).foldLeft(zeroOfV) {
        case (sum, i) => sum + (l(i) * r(k - i))
      }), combinedLength(l, r))
    def fromInt(x: Int): TDif[V] = TDif.constant(evVIsIntegral fromInt x)
    lazy val zero: TDif[V] = TDif.constant(zeroOfV)
    lazy val one: TDif[V] = TDif.constant(oneOfV)
    def isConstant(x: TDif[V]) = (1 until x.length).forall(x(_) == zeroOfV)
    override def isZero(x: TDif[V]): Boolean = isConstant(x) && x(0).isZero
    /** Return index of first non-zero coefficient. When none exists, returns -1. */
    def firstNonZero(x: TDif[V]): Int = x.indexWhere(xk => !(evVIsIntegral isZero xk))
    def ==(a: TDif[V], b: TDif[V]): Boolean = (0 to a.length).forall(i => a(i) == b(i))
    def !=(a: TDif[V], b: TDif[V]): Boolean = (0 to a.length).exists(i => a(i) != b(i))
  }
  
  /** Real instance for TDif[V], where V itself has a Real instance */
  abstract class TDifAsReal[V: Real] extends TDifAsIntegral[V] with Real[TDif[V]] {
    def fromDouble(x: Double): TDif[V] = TDif.constant(evVIsReal fromDouble x)
    /* Constants */
    val evVIsReal = implicitly[Real[V]]
    /* Real instance */
    def div(l: TDif[V], r: TDif[V]): TDif[V] = {
      val k0 = firstNonZero(r)
      val l0 = firstNonZero(l)
      require(k0 > -1, "Division by zero is not allowed.")
      require(k0 <= l0 || l0 == -1, s"First non-vanishing coefficient of $r must not be higher order than the first non-vanishing coefficient of $l.")
      TDif ({
        lazy val coeff: Stream[V] = (l(k0) / r(k0)) #:: Stream.from(1).map(k =>
          (l(k + k0) - (0 to k - 1).foldLeft(zeroOfV) {
            case (sum, i) => sum + (coeff(i) * r(k - i + k0))
          }) / r(k0))
        coeff
        }, combinedLength(l, r)) // as 0 <= k0 <= l0, this is a positive integer
    }

    /* Power functions */
    /** General interface for power */
    def pow(l: TDif[V], r: TDif[V]): TDif[V] =
      // r is constant
      if (isConstant(r))
        if (r(0) == oneOfV)    l        else
        if (r(0) == zeroOfV) { require(!isZero(l), s"pow is not applicable to ($l,$r) as 0^0 is not defined.")
                               one }
        else                   powOnReal(l, r)
      // r is not constant   
      else 
        if (l == zero)         { require(!(r(0) == zeroOfV), s"pow is not applicable to ($l,$r) as 0^0 is not defined.")
                               zero }
        else                   exp(mul(r, log(l)))

    /** Power with integer exponent */
    def pow(l: TDif[V], n: Int) : TDif[V] = {
        if (n == 0) one                       else 
        if (n == 1) l                         else
        if (n == 2) square(l)                 else
        if (n <  0) div(one, pow(l, -n)) else
        powBySquare(l, n)
    }
      
    /** Square root */
    def sqrt(x: TDif[V]): TDif[V] =
      if (isZero(x)) zero else {
        val twoOfV = evVIsIntegral fromInt 2
        val k0  = firstNonZero(x) // n >= k0 because x != zero
        require((k0 % 2 == 0), s"First non-vanishing coefficient must be an even power in $x in order to expand the sqrt function.")
        val x0 = x(k0)
        val k0d2 = k0 / 2
        TDif ({ // We might call sqrt directly not only through power, so we check for zero
          lazy val coeff: Stream[V] = Stream.from(0).map(k =>
            if (k < k0d2) zeroOfV       // the first k0/2 coefficients are zero
            else if (k == k0d2) x0.sqrt // the first non-zero coefficient of the result
            else {                      // possibly  non-zero coefficients k0/2 + 1 .. n - 1
              val kPk0d2 = k + k0d2
              val kEnd = (kPk0d2 + (kPk0d2 % 2) - 2) / 2
              (x(kPk0d2) - twoOfV * ((k0d2 + 1 to kEnd).foldLeft(zeroOfV) { 
                case (sum, i) => sum + coeff(i) * coeff(kPk0d2 - i)  
              }) + (if (kPk0d2 % 2 == 0) - coeff(kPk0d2 / 2).square else zeroOfV )) / (twoOfV * coeff(k0d2)) 
            })
          coeff        
        }, x.length) // FIXME How many?
      }
    
    /** Square */
    def square(x: TDif[V]): TDif[V] = TDif ({
      lazy val coeff: Stream[V] = x(0).square #:: Stream.from(1).map{ k =>
        val kEnd = (k + k%2 - 2)/2
        // 2 x(i)x(j), i != j
        evVIsIntegral.fromInt(2) * ((0 to kEnd).foldLeft(zeroOfV) {
          case (sum, i) => sum + x(i) * x(k - i)
        // x(k/2)^2 if k was even
        }) + (if (k % 2 == 0) x(k/2).square else zeroOfV) // FIXME should adding zeroOfV optimized? 
      }
      coeff
    }, x.length) // FIXME How many?
    
    /** Integer power by squaring */
    private def powBySquare(l: TDif[V], n: Int): TDif[V] =
      mul(square(pow(l, n/2)), (if (n % 2 == 1) l else one)) // FIXME should multiplying with one be optimized? 
    
    /** Power with real exponent
     *  l != zero, r != zero, r != one */
    private def powOnReal(l: TDif[V], r: TDif[V]) : TDif[V] =
      if (r(0).isValidInt) pow(l, r(0).toInt) else {
        val k0  = firstNonZero(l) // n >= k0 because l != zero
        val lk0 = l(k0)
        val a = r(0) // FIXME This is a constant! Should not be lifted in the first place.
        val k0V = a * evVIsIntegral.fromInt(k0)
        require((k0 == 0 || k0V.isValidInt), s"pow is not applicable to ($l,$r). Expanding around 0 needs the product of the exponent and of the index of the first non-zero coefficient to be an integer.")
        val ak0 = k0V.toInt
        TDif ({
          lazy val coeff: Stream[V] = Stream.from(0).map { k =>
            if (k <  ak0) zeroOfV else   // the first a * k0 coefficients are zero
            if (k == ak0) lk0 ^ a else { // the first non-zero coefficient of the result
              val kmak0L = evVIsIntegral.fromInt(k) - evVIsIntegral.fromInt(ak0)
              val ap1 = a + oneOfV
              ((1 to k).foldLeft(zeroOfV) { 
                case (sum, i) =>  
                  sum + (ap1 * evVIsIntegral.fromInt(i) / kmak0L - oneOfV) * l(i + k0) * coeff(k - i)  
              }) / lk0
            }
          }
          coeff
        }, l.length) // FIXME How many?
      }
    
    /** Exponential function */
    def exp(x: TDif[V]): TDif[V] = TDif ({
      lazy val coeff: Stream[V] = x(0).exp #:: Stream.from(1).map(k =>
        ((1 to k).foldLeft(zeroOfV) {
          case (sum, i) => sum + evVIsIntegral.fromInt(i) * x(i) * coeff(k-i)
        }) / evVIsIntegral.fromInt(k))
      coeff
    }, x.length) // FIXME How many?
    
    /** Natural logarithm */
    def log(x: TDif[V]): TDif[V] = TDif ({
        require(firstNonZero(x) == 0, "Not possible to expand log around 0.")
        val x0 = x(0)
        lazy val coeff: Stream[V] = x0.log #:: Stream.from(1).map(k => 
          (x(k) - ((1 to k - 1).foldLeft(zeroOfV) {
            case (sum, i) => sum + evVIsIntegral.fromInt(i) * coeff(i) * x(k-i)
          }) / evVIsIntegral.fromInt(k)) / x0)
        coeff
      }, x.length) // FIXME How many?
    
    /* Trigonometric functions */
    def sin(x: TDif[V]): TDif[V] = TDif(sinAndCos(x).map(_._1), x.length) // FIXME How many?
    def cos(x: TDif[V]): TDif[V] = TDif(sinAndCos(x).map(_._2), x.length) // FIXME How many?
    private def sinAndCos(x: TDif[V]): Stream[(V,V)] = {
      lazy val coeff: Stream[(V,V)] =
        (x(0).sin, x(0).cos) #:: Stream.from(1).map{ k =>
          val (sck, cck) = (1 to k).foldLeft(zeroOfV, zeroOfV) {
            case ((sckSum, cckSum), i) => 
              val ixi = evVIsIntegral.fromInt(i) * x(i)
              val cki = coeff(k - i)
              ( sckSum + ixi * cki._2
              , cckSum + ixi * cki._1 )
          }
          val kL = evVIsIntegral.fromInt(k)
          (sck / kL, -cck / kL)
        }
      coeff
    }
    
    def tan(x: TDif[V]): TDif[V] = TDif ({
      val cos2 = square(cos(x))
      lazy val coeff: Stream[V] = x(0).tan #:: Stream.from(1).map(k =>
        (x(k) - ((1 to k-1).foldLeft(zeroOfV) {
          case (sum, i) => 
            sum + evVIsIntegral.fromInt(i) * coeff(i) * cos2(k - i)
        }) / evVIsIntegral.fromInt(k)) / (x(0).cos).square)
      coeff
    }, x.length) // FIXME How many?
    
    def acos(x: TDif[V]): TDif[V] = TDif ({
      val c = sqrt(sub(one, square(x)))
      lazy val coeff: Stream[V] = x(0).acos #:: Stream.from(1).map(k => 
        - (x(k) + ((1 to k-1).foldLeft(zeroOfV) {
          case (sum, i) => 
            sum + evVIsIntegral.fromInt(i) * coeff(i) * c(k - i)
        }) / evVIsIntegral.fromInt(k)) / (oneOfV - x(0).square).sqrt)
      coeff
    }, x.length) // FIXME How many?
    
    def asin(x: TDif[V]): TDif[V] = TDif ({
      val c = sqrt(sub(one, square(x)))
      lazy val coeff: Stream[V] = x(0).asin #:: Stream.from(1).map(k =>
        (x(k) - ((1 to k-1).foldLeft(zeroOfV) {
          case (sum, i) => 
            sum + evVIsIntegral.fromInt(i) * coeff(i) * c(k - i)
        }) / evVIsIntegral.fromInt(k)) / (oneOfV - x(0).square).sqrt)
      coeff
    }, x.length) // FIXME How many?
    
    def atan(x: TDif[V]): TDif[V] = TDif ({
        val c = add(one, square(x))
        lazy val coeff: Stream[V] = x(0).atan #:: Stream.from(1).map(k =>
          (x(k) - ((1 to k-1).foldLeft(zeroOfV) {
            case (sum, i) => 
              sum + evVIsIntegral.fromInt(i) * coeff(i) * c(k - i)
          }) / evVIsIntegral.fromInt(k)) / (oneOfV + x(0).square))
        coeff
      }, x.length) // FIXME How many?
  }
  implicit object intTDifIsIntegral extends TDifAsIntegral[Int] {
    def groundValue(v: TDif[Int]) = GIntTDif(v)
  }
  implicit object doubleTDifIsReal extends TDifAsReal[Double] {
    def groundValue(v: TDif[Double]) = GDoubleTDif(v)
  }
  implicit object intervalTDifIsReal extends TDifAsReal[Interval] {
    def groundValue(v: TDif[Interval]) = GIntervalTDif(v)
  }
  
  /** Representation of a number and its time derivatives. */
  case class TDif[V: Integral](coeff: Seq[V], length: Int) extends Dif[V,Int]{
    def apply(i: Int): V = if (i < length) coeff(i) else implicitly[Integral[V]].zero
    def head: V = coeff(0)
    def map[W: Integral](m: V => W): TDif[W] = TDif(coeff map m, length)
    def indexWhere(m: V => Boolean): Int = coeff.take(length).indexWhere(c => m(c))
    override def hashCode() = (coeff take length).hashCode
    override def equals(o: Any) = o match {
      case tdif: TDif[V] => 
        this.length == tdif.length && 
          ((this.coeff zip tdif.coeff) take length).forall{ case (l,r) => l equals r }
      case _ => false
    } 
  }
  object TDif {
    /** Lift a constant value of type A to a TDif. */
    def constant[V: Integral](a: V) = TDif(a #:: Stream.continually(implicitly[Integral[V]].zero), 1)
  }
  
  /** Lift all numeric values in a store into TDifs */
  def lift[S,Id](st: RichStore[S,Id]): S = st map liftValue 
  
  /** Lift all numeric values in an Expr into TDifs */
  def lift(e: Expr): Expr = new acumen.util.ASTMap {
    override def mapExpr(e: Expr): Expr = (e match {
      case Lit(gv) => Lit(liftGroundValue(gv))
      case _ => super.mapExpr(e)
    }).setPos(e.pos)
  }.mapExpr(e)
  
  private def liftGroundValue(gv: GroundValue): GroundValue = gv match {
    case GDouble(d) => GDoubleTDif(TDif constant d)
    case GInt(i) => GDoubleTDif(TDif constant i)
    case GInterval(i) => GIntervalTDif(TDif constant i)
    case g: GConstantRealEnclosure => 
      GCValueTDif(TDif.constant(VLit(g): CValue)(interpreters.enclosure2015.intervalBase.cValueIsReal))
    case g: GIntervalFDif => 
      GCValueTDif(TDif.constant(VLit(g): CValue)(interpreters.enclosure2015.fDifBase.cValueIsReal))
    case _ => gv
  }
  
  /** Lift all the values inside a Value[Id] into TDifs */
  private def liftValue[Id](v: Value[Id]): Value[Id] = v match {
    case VLit(gv) => VLit(liftGroundValue(gv))
    case VVector(lv: List[Value[Id]]) => VVector(lv map liftValue)
    case VList(lv: List[Value[Id]]) => VList(lv map liftValue)
    case _ => v
  }
  
  /** Lower all TDif values in a RichStore into the corresponding numeric value */
  def lower[S,Id: TypeTag](st: RichStore[S,Id]): S = st map lowerValue
  
  /** Lower all TDif values in an Expr into the corresponding numeric value */
  def lower(e: Expr): Expr = new acumen.util.ASTMap {
    override def mapExpr(e: Expr): Expr = (e match {
      case Lit(gv) => Lit(lowerGroundValue(gv))
      case _ => super.mapExpr(e)
    }).setPos(e.pos)
  }.mapExpr(e)
  
  private def lowerGroundValue(gd: GroundValue): GroundValue = gd match {
    case GIntTDif(TDif(v,_)) => GInt(v(0))
    case GIntervalTDif(TDif(v,_)) => GConstantRealEnclosure(v(0))
    case GDoubleTDif(TDif(v,_)) => val v0 = v(0)
      if (doubleIsReal isValidInt v0) GInt(doubleIsReal toInt v0) else GDouble(v0)
    case _ => gd
  }
  
  /** Lower all the values inside a Value[Id] from TDifs */
  private def lowerValue[Id: TypeTag](v: Value[Id]): Value[Id] = v match {
    case VLit(gv) => gv match {
      // Special case where a GroundValue wraps a CValue 
      case GCValueTDif(TDif(v, _)) if typeOf[Id] <:< typeOf[CId] =>
        v(0).asInstanceOf[Value[Id]]
      case _ => VLit(lowerGroundValue(gv))
    }
    case VVector(lv: List[Value[Id]]) => VVector(lv map lowerValue)
    case VList(lv: List[Value[Id]]) => VList(lv map lowerValue)
    case _ => v
  }
  

}
