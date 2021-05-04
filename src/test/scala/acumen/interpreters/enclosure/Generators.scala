package acumen.interpreters.enclosure

import scala.math.max
import scala.math.min
import org.scalacheck.Arbitrary.arbitrary
import org.scalacheck.Gen._
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import acumen.interpreters.enclosure.affine.AffineScalarEnclosure._
import Types._
import Interval._
import acumen.interpreters.enclosure.affine.UnivariateAffineEnclosure
import acumen.interpreters.enclosure.affine.AffineScalarEnclosure
import acumen.interpreters.enclosure.affine.UnivariateAffineScalarEnclosure
import acumen.interpreters.enclosure.affine.AffineEnclosure

object Generators {

  import TestingContext._

  /* Double */

  def genSmallDouble: Gen[Double] = choose(-1000.0, 1000.0)
  implicit def arbitraryDouble: Arbitrary[Double] = Arbitrary(genSmallDouble)

  /** Generates Doubles larger than the parameter d. */
  def genLargerDouble(d: Double): Gen[Double] = {
    // TODO Verify that this works correctly 
    val rest = Double.MaxValue + (if (d < 0) d else -d)
    for { delta <- choose(0, if (rest < 0) -rest else rest) } yield d + delta
  }

  /* Interval */

  /** Generates a random interval. */
  def genInterval: Gen[Interval] = for {
    lo <- arbitrary[Double]
    hi <- arbitrary[Double]
  } yield Interval(min(lo, hi), max(lo, hi))
  implicit val arbitraryInterval = Arbitrary(genInterval)

  /** Generates a positive interval */
  def genPosInterval: Gen[Interval] = for {
    lo <- posNum[Double]
    hi <- posNum[Double]
  } yield Interval(min(lo, hi), max(lo, hi))

  /** Generates a non-zero interval */
  def genNonZeroInterval: Gen[Interval] = for {
    i <- genPosInterval
    b <- arbitrary[Boolean]
  } yield if (b) i else -i

  /** Generates a sub-interval of the interval. */
  def genSubInterval(i: Interval): Gen[Interval] = for {
    s <- choose(0.0, 1.0)
    val r = ((i.width.low / 2).low * s).low
  } yield (i.low + r) /\ (i.high - r)

  /** Generates a point in the interval. */
  def genThinSubInterval(i: Interval) = for {
    s <- choose(0.0, 1.0)
  } yield (i.low + i.width.low * s).high

  def genIntervalFilter: Gen[Interval] = for {
    lo <- arbitrary[Double]
    hi <- genLargerDouble(lo)
  } yield Interval(lo, hi)

  /* VarName */

  /** Generates a random VarName */
  def genVarName: Gen[VarName] = for {
    c <- alphaChar
  } yield c.toString
  implicit val arbitraryVarName = Arbitrary(genVarName)

  /* Box */

  /** Generates a plain box. */
  def genBox: Gen[Box] = for {
    dim <- posNum[Int]
    intervals <- listOfN(dim, arbitrary[Interval])
  } yield (listOfNames(dim) zip intervals).toMap
  implicit def arbitraryBox: Arbitrary[Box] = Arbitrary(genBox)

  /** Generates a dim-dimensional box. */
  def genDimBox(dim: Int): Gen[Box] = for {
    intervals <- listOfN(dim, arbitrary[Interval])
  } yield (listOfNames(dim) zip intervals).toMap

  /** Generates a sub-box of the box. */
  def genSubBox(box: Box): Gen[Box] = for {
    subIntervals <- Gen.sequence[List, Interval](box.values.map(genSubInterval(_)))
  } yield (box.keys zip subIntervals).toMap

  /** Generates a point in the box. */
  def genThinSubBox(box: Box): Gen[Box] = for {
    thinSubIntervals <- Gen.sequence[List, Interval](box.values.map(genThinSubInterval(_)))
  } yield (box.keys zip thinSubIntervals).toMap

  /** Generates a box with the given variable names. */
  def genNamesBox(names: Set[VarName]) = for {
    intervals <- listOfN(names.size, arbitrary[Interval])
  } yield names.zip(intervals).toMap

  /* AffineScalarEnclosure */

  /** Generates a plain enclosure. */
  def genAffineScalarEnclosure: Gen[AffineScalarEnclosure] = for {
    dim <- posNum[Int]
    names <- listOfN(dim, arbitrary[VarName])
    domains <- listOfN(dim, arbitrary[Interval])
    constant <- arbitrary[Interval]
    coeffs <- listOfN(dim, arbitrary[Interval])
    val domain = (names zip domains).toMap
    val coefficients = (names zip coeffs).toMap
  } yield AffineScalarEnclosure(domain, Box.normalize(domain), constant, coefficients)
  implicit val arbitraryAffineScalarEnclosure = Arbitrary(genAffineScalarEnclosure)

  /** Generates a dim-dimensional enclosure. */
  def genDimAffineScalarEnclosure(dim: Int): Gen[AffineScalarEnclosure] = for {
    names <- listOfN(dim, arbitrary[VarName])
    domains <- listOfN(dim, arbitrary[Interval])
    constant <- arbitrary[Interval]
    coeffs <- listOfN(dim, arbitrary[Interval])
    val domain = (names zip domains).toMap
    val coefficients = (names zip coeffs).toMap
  } yield AffineScalarEnclosure(domain, Box.normalize(domain), constant, coefficients)

  /** Generates an enclosure over the box. */
  def genBoxAffineScalarEnclosure(box: Box): Gen[AffineScalarEnclosure] = for {
    constant <- arbitrary[Interval]
    coeffs <- listOfN(box.size, arbitrary[Interval])
    val coefficients = (box.keys zip coeffs).toMap
  } yield AffineScalarEnclosure(box, Box.normalize(box), constant, coefficients)

  /** Generates a sub-enclosure of the enclosure. */
  def genSubAffineScalarEnclosure(f: AffineScalarEnclosure): Gen[AffineScalarEnclosure] = for {
    subconst <- genSubInterval(f.constant)
    subcoeffs <- Gen.sequence[List, Interval](f.coefficients.values.map(genSubInterval(_)))
  } yield AffineScalarEnclosure(f.domain, f.normalizedDomain, subconst, (f.domain.keys zip subcoeffs).toMap)

  /* AffineEnclosure */

  /** Generates a plain enclosure. */
  def genAffineEnclosure: Gen[AffineEnclosure] = for {
    dom <- arbitrary[Box]
    components <- listOfN(dom.size, genBoxAffineScalarEnclosure(dom))
  } yield AffineEnclosure(dom, dom.keys.zip(components).toMap)
  implicit def arbitraryAffineEnclosure: Arbitrary[AffineEnclosure] = Arbitrary(genAffineEnclosure)

  /** Generates a plain enclosure. */
  def genDimAffineEnclosure(dim: Int): Gen[AffineEnclosure] = for {
    dom <- genDimBox(dim)
    components <- listOfN(dom.size, genBoxAffineScalarEnclosure(dom))
  } yield AffineEnclosure(dom, dom.keys.zip(components).toMap)

  /* UnivariateAffineScalarEnclosure */

  /** Generates a plain univariate enclosure. */
  def genUnivariateAffineScalarEnclosure: Gen[UnivariateAffineScalarEnclosure] = for {
    domain <- arbitrary[Interval]
    u <- genBoxUnivariateAffineScalarEnclosure(domain)
  } yield u
  implicit val use: Arbitrary[UnivariateAffineScalarEnclosure] = Arbitrary(genUnivariateAffineScalarEnclosure)

  /** Generates a univariate scalar enclosure over the domain. */
  def genBoxUnivariateAffineScalarEnclosure(domain: Interval): Gen[UnivariateAffineScalarEnclosure] = for {
    List(constant, coefficient) <- listOfN(2, arbitrary[Interval])
  } yield UnivariateAffineScalarEnclosure(domain, 0 /\ domain.width.high, constant, coefficient)

  /** Generates a univariate scalar enclosure over the domain of "e" that is also contained in "e". */
  def genSubUnivariateAffineScalarEnclosure(e: UnivariateAffineScalarEnclosure): Gen[UnivariateAffineScalarEnclosure] = for {
    constant <- genSubInterval(e.constant)
    coefficient <- genSubInterval(e.coefficient)
  } yield UnivariateAffineScalarEnclosure(e.domain, e.normalizedDomain, constant, coefficient)

  /* UnivariateAffineEnclosure */

  /** Generates a univariate enclosure with given number of components (variables) over the given interval. */
  def genDimDomUnivariateAffineEnclosure(dim: Int, dom: Interval): Gen[UnivariateAffineEnclosure] = for {
    components <- listOfN(dim, genBoxUnivariateAffineScalarEnclosure(dom))
  } yield UnivariateAffineEnclosure(dom, (((0 to dim) map (_.toString)) zip components) toMap)

  /** Generates a univariate enclosure with given number of components (variables). */
  def genDimUnivariateAffineEnclosure(dim: Int): Gen[UnivariateAffineEnclosure] = for {
    dom <- arbitrary[Interval]
    e <- genDimDomUnivariateAffineEnclosure(dim, dom)
  } yield e

  /** Generates a univariate enclosure over the domain. */
  def genUnivariateAffineEnclosure: Gen[UnivariateAffineEnclosure] = for {
    n <- chooseNum(1, 6) // TODO Externalize this constant
    e <- genDimUnivariateAffineEnclosure(n)
  } yield e

  // TODO write generators corresponding to those for AffineScalarEnclosure

  /* Expression */

  /**
   * Generates a random expression.
   *
   * Implementation note: when sampling the generators with equal
   * probability expressions of very large size are generated. To
   * generate smaller expression trees we sample the leaf
   * generators more often.
   */
  def genExpression =
    frequency(
      (2, genConstant),
      (2, genVariable),
      (1, genNegate),
      (1, genPlus),
      (1, genMultiply),
      (1, genDivide))
  implicit val arbitraryExpression: Arbitrary[Expression] = Arbitrary(genExpression)

  /** Generates a random constant. */
  def genConstant = for {
    value <- arbitrary[Interval]
  } yield Constant(value)

  /** Generates a random constant. */
  def genVariable = for {
    name <- arbitrary[VarName]
  } yield Variable(name)

  /** Generates a random negated expression. */
  def genNegate = for {
    e <- Gen.lzy { arbitrary[Expression] }
  } yield Negate(e)

  /** Generates a random negated expression. */
  def genPlus = for {
    l <- Gen.lzy { arbitrary[Expression] }
    r <- arbitrary[Expression]
  } yield Plus(l, r)

  /** Generates a random negated expression. */
  def genMultiply = for {
    l <- Gen.lzy { arbitrary[Expression] }
    r <- arbitrary[Expression]
  } yield Multiply(l, r)

  // TODO un-specialize the generator once enclosure division is implemented.
  /** Generates a random negated expression. */
  def genDivide = for {
    l <- Gen.lzy { arbitrary[Expression] }
    v <- genNonZeroInterval
  } yield Divide(l, Constant(v))

  /**
   * Generates a random affine expression.
   * See implementation note for genExpression.
   */
  def genAffineExpression: Gen[Expression] =
    frequency(
      (2, genConstant),
      (2, genVariable),
      (1, genAffineNegate),
      (1, genAffinePlus),
      (1, genAffineMultiply))

  /** Generates a random negated affine expression. */
  def genAffineNegate: Gen[Expression] = for {
    e <- Gen.lzy { genAffineExpression }
  } yield Negate(e)

  /** Generates a random sum of affine expressions. */
  def genAffinePlus: Gen[Expression] = for {
    l <- Gen.lzy { genAffineExpression }
    r <- genAffineExpression
  } yield Plus(l, r)

  /** Generates a random scalar multiple of an affine expression. */
  def genAffineMultiply: Gen[Expression] = for {
    l <- Gen.lzy { genConstant }
    r <- genAffineExpression
  } yield Multiply(l, r)

  /** Generates a random scalar quoteient of an affine expression. */
  def genAffineDivide: Gen[Expression] = for {
    l <- Gen.lzy { genAffineExpression }
    r <- genConstant
  } yield Divide(l, r)

  /* AffineEnclosure */

  // TODO write generators corresponding to those for AffineScalarEnclosure

  /* --- Utilities --- */

  /** Returns an version of the input Interval i, padded below and above by loPad and hiPad respectively. */
  def pad(i: Interval, loPad: Double, hiPad: Double): Interval = {
    i + Interval(-loPad, hiPad)
  }

  /** Returns a list of strings of the form t_i, where 0 <= i <= dim. For dim <= 0 an empty list is generated. */
  def listOfNames(dim: Int): Seq[VarName] = for { i <- 0 to dim } yield "t_" + i

}
