package com.example.engine

import java.math.BigInteger
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

/**
 * Exact rational arithmetic backed by BigInteger to prevent precision loss and overflow during
 * intermediate fraction operations, matching Casio fx-82MS fraction behavior.
 */
data class Fraction private constructor(val numerator: BigInteger, val denominator: BigInteger) {

  val isInteger: Boolean
    get() = denominator == BigInteger.ONE

  fun toDouble(): Double {
    val num = numerator.toDouble()
    val den = denominator.toDouble()
    return num / den
  }

  operator fun unaryMinus(): Fraction = Fraction(numerator.negate(), denominator)

  operator fun plus(other: Fraction): Fraction =
    of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)

  operator fun minus(other: Fraction): Fraction =
    of(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)

  operator fun times(other: Fraction): Fraction =
    of(numerator * other.numerator, denominator * other.denominator)

  operator fun div(other: Fraction): Fraction {
    if (other.numerator == BigInteger.ZERO) {
      throw CalcException(CalcErrorType.MathError, "Division by zero")
    }
    return of(numerator * other.denominator, denominator * other.numerator)
  }

  fun pow(exponent: Int): Fraction {
    if (exponent == 0) return ONE
    if (abs(exponent) > 64) {
      throw CalcException(CalcErrorType.MathError, "Exponent too large for exact fraction")
    }
    return if (exponent > 0) {
      of(numerator.pow(exponent), denominator.pow(exponent))
    } else {
      if (numerator == BigInteger.ZERO) {
        throw CalcException(CalcErrorType.MathError, "Division by zero")
      }
      val posExp = -exponent
      of(denominator.pow(posExp), numerator.pow(posExp))
    }
  }

  fun format(preference: FractionPreference): String {
    if (denominator == BigInteger.ONE) return numerator.toString()
    val sign = if (numerator < BigInteger.ZERO) "-" else ""
    val absNum = numerator.abs()
    return if (preference == FractionPreference.MIXED && absNum > denominator) {
      val whole = absNum / denominator
      val rem = absNum % denominator
      "$sign$whole⌟$rem⌟$denominator"
    } else {
      "$sign$absNum⌟$denominator"
    }
  }

  fun fitsDisplayLimit(): Boolean {
    // Casio fx-82MS supports fractions when total digits of integer + num + den <= 10
    val absNum = numerator.abs()
    val whole = absNum / denominator
    val rem = absNum % denominator
    val totalChars =
      if (whole > BigInteger.ZERO && rem > BigInteger.ZERO) {
        whole.toString().length + rem.toString().length + denominator.toString().length
      } else {
        absNum.toString().length + denominator.toString().length
      }
    return totalChars <= 10
  }

  companion object {
    val ZERO = Fraction(BigInteger.ZERO, BigInteger.ONE)
    val ONE = Fraction(BigInteger.ONE, BigInteger.ONE)

    fun of(numerator: Long, denominator: Long = 1L): Fraction =
      of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

    fun of(numerator: BigInteger, denominator: BigInteger): Fraction {
      if (denominator == BigInteger.ZERO) {
        throw CalcException(CalcErrorType.MathError, "Division by zero")
      }
      if (numerator == BigInteger.ZERO) return ZERO
      var num = numerator
      var den = denominator
      if (den < BigInteger.ZERO) {
        num = num.negate()
        den = den.negate()
      }
      val gcd = num.abs().gcd(den)
      return Fraction(num / gcd, den / gcd)
    }

    fun fromMixed(whole: Long, num: Long, den: Long): Fraction {
      if (den == 0L) throw CalcException(CalcErrorType.MathError, "Division by zero")
      val sign = if (whole < 0 || num < 0 || den < 0) -1L else 1L
      val absWhole = BigInteger.valueOf(abs(whole))
      val absNum = BigInteger.valueOf(abs(num))
      val absDen = BigInteger.valueOf(abs(den))
      val totalNum = absWhole * absDen + absNum
      return of(if (sign < 0) totalNum.negate() else totalNum, absDen)
    }

    /**
     * Converts a decimal Double to a clean Fraction using continued fractions, bounded by
     * maxDenominator so irrational numbers stay decimals unless within tight rational tolerance.
     */
    fun fromDouble(value: Double, maxDenominator: Long = 10_000_000L): Fraction? {
      if (!value.isFinite()) return null
      if (abs(value) > 1e9) return null
      if (abs(value) < 1e-12) return ZERO

      val sign = if (value < 0) -1L else 1L
      val x = abs(value)
      var h1 = 1L
      var h2 = 0L
      var k1 = 0L
      var k2 = 1L
      var b = x
      val tolerance = 1e-10 * maxOf(1.0, x)

      for (iter in 0 until 32) {
        val a = floor(b)
        val aLong = a.toLong()
        val nextH = aLong * h1 + h2
        val nextK = aLong * k1 + k2
        if (nextK > maxDenominator || nextH > 1_000_000_000L) break
        h2 = h1
        h1 = nextH
        k2 = k1
        k1 = nextK
        val approx = h1.toDouble() / k1.toDouble()
        if (abs(x - approx) <= tolerance) {
          val candidate = of(sign * h1, k1)
          return if (candidate.fitsDisplayLimit()) candidate else null
        }
        val fracPart = b - a
        if (fracPart < 1e-12) break
        b = 1.0 / fracPart
        if (!b.isFinite() || b > 1e12) break
      }
      val approx = h1.toDouble() / k1.toDouble()
      if (k1 in 1..maxDenominator && abs(x - approx) <= 1e-9 * maxOf(1.0, x)) {
        val candidate = of(sign * h1, k1)
        return if (candidate.fitsDisplayLimit()) candidate else null
      }
      // Also check exact integer
      val rounded = x.roundToLong()
      if (abs(x - rounded.toDouble()) < 1e-12) {
        return of(sign * rounded, 1L)
      }
      return null
    }
  }
}
