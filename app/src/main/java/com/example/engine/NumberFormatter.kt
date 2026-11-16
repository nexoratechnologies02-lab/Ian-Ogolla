package com.example.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

object NumberFormatter {

  /** Snaps near-zero floating point dust (e.g. sin(180 deg) = 1.22e-16) to exact 0.0 */
  fun sanitize(value: Double): Double {
    if (!value.isFinite()) return value
    if (abs(value) < 1e-13) return 0.0
    val rounded = kotlin.math.round(value)
    if (abs(value - rounded) < 1e-12 * maxOf(1.0, abs(rounded))) {
      return rounded
    }
    return value
  }

  fun format(
    value: Double,
    displayMode: DisplayMode = DisplayMode.NORM1,
    fixDigits: Int = 2,
    sciDigits: Int = 3,
    engOffset: Int? = null,
    asDms: Boolean = false,
    fraction: Fraction? = null,
    showFraction: Boolean = false,
    fractionPreference: FractionPreference = FractionPreference.MIXED,
  ): String {
    if (value.isNaN() || value.isInfinite()) {
      return "Math ERROR"
    }
    val clean = sanitize(value)
    if (abs(clean) >= 1e100) {
      return "Math ERROR"
    }

    if (showFraction && fraction != null && fraction.fitsDisplayLimit()) {
      return fraction.format(fractionPreference)
    }

    if (asDms) {
      return formatDms(clean)
    }

    if (engOffset != null) {
      return formatEngineering(clean, engOffset)
    }

    return when (displayMode) {
      DisplayMode.FIX -> formatFix(clean, fixDigits.coerceIn(0, 9))
      DisplayMode.SCI -> formatSci(clean, sciDigits.coerceIn(1, 10))
      DisplayMode.NORM1 -> formatNorm(clean, lowThreshold = 1e-2)
      DisplayMode.NORM2 -> formatNorm(clean, lowThreshold = 1e-9)
    }
  }

  private fun formatNorm(value: Double, lowThreshold: Double): String {
    if (value == 0.0) return "0"
    val absVal = abs(value)
    if (absVal >= 1e10 || absVal < lowThreshold) {
      return formatSci(value, 10, stripTrailingZeros = true)
    }
    // Round to 10 significant digits like Casio fx-82MS internal display precision
    val bd =
      BigDecimal.valueOf(value)
        .round(MathContext(10, RoundingMode.HALF_UP))
        .stripTrailingZeros()
    val plain = bd.toPlainString()
    return if (plain == "-0") "0" else plain
  }

  private fun formatFix(value: Double, digits: Int): String {
    if (abs(value) >= 1e10) return formatSci(value, 10, stripTrailingZeros = false)
    val bd = BigDecimal.valueOf(value).setScale(digits, RoundingMode.HALF_UP)
    val str = bd.toPlainString()
    return if (str.startsWith("-0") && bd.compareTo(BigDecimal.ZERO) == 0) {
      str.removePrefix("-")
    } else {
      str
    }
  }

  private fun formatSci(
    value: Double,
    sigDigits: Int,
    stripTrailingZeros: Boolean = false,
  ): String {
    if (value == 0.0) {
      return if (stripTrailingZeros || sigDigits <= 1) "0×10⁰⁰"
      else "0.${"0".repeat(sigDigits - 1)}×10⁰⁰"
    }
    val sign = if (value < 0) "-" else ""
    val absVal = abs(value)
    var exp = floor(log10(absVal)).toInt()
    var mantissa = absVal / 10.0.pow(exp)
    val bd =
      BigDecimal.valueOf(mantissa)
        .round(MathContext(sigDigits, RoundingMode.HALF_UP))
    if (bd >= BigDecimal.TEN) {
      mantissa /= 10.0
      exp += 1
    }
    val finalBd =
      if (stripTrailingZeros) {
        BigDecimal.valueOf(mantissa)
          .round(MathContext(sigDigits, RoundingMode.HALF_UP))
          .stripTrailingZeros()
      } else {
        BigDecimal.valueOf(mantissa)
          .setScale((sigDigits - 1).coerceAtLeast(0), RoundingMode.HALF_UP)
      }
    val expStr = formatExponentSuperscript(exp)
    return "$sign${finalBd.toPlainString()}×10$expStr"
  }

  fun formatEngineering(value: Double, stepShift: Int = 0): String {
    if (value == 0.0) return "0×10⁰⁰"
    val sign = if (value < 0) "-" else ""
    val absVal = abs(value)
    val rawExp = floor(log10(absVal)).toInt()
    var engExp = floor(rawExp / 3.0).toInt() * 3
    engExp += stepShift * 3
    val mantissa = absVal / 10.0.pow(engExp)
    val bd =
      BigDecimal.valueOf(mantissa)
        .round(MathContext(10, RoundingMode.HALF_UP))
        .stripTrailingZeros()
    return "$sign${bd.toPlainString()}×10${formatExponentSuperscript(engExp)}"
  }

  fun formatDms(decimalDegrees: Double): String {
    val sign = if (decimalDegrees < 0) "-" else ""
    val absDeg = abs(decimalDegrees)
    var degrees = floor(absDeg).toLong()
    val remMinutes = (absDeg - degrees) * 60.0
    var minutes = floor(remMinutes).toLong()
    var seconds = (remMinutes - minutes) * 60.0
    // Handle rounding up at 59.995 seconds
    seconds = BigDecimal.valueOf(seconds).setScale(2, RoundingMode.HALF_UP).toDouble()
    if (seconds >= 60.0) {
      seconds -= 60.0
      minutes += 1
    }
    if (minutes >= 60L) {
      minutes -= 60L
      degrees += 1L
    }
    val secFormatted =
      BigDecimal.valueOf(seconds).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    return "$sign${degrees}°${minutes}′${secFormatted}″"
  }

  fun formatCompact(value: Double): String = format(value, DisplayMode.NORM1)

  private fun formatExponentSuperscript(exp: Int): String {
    val sign = if (exp < 0) "⁻" else ""
    val digits = String.format(Locale.US, "%02d", abs(exp))
    val supDigits =
      digits.map { ch ->
        when (ch) {
          '0' -> '⁰'
          '1' -> '¹'
          '2' -> '²'
          '3' -> '³'
          '4' -> '⁴'
          '5' -> '⁵'
          '6' -> '⁶'
          '7' -> '⁷'
          '8' -> '⁸'
          '9' -> '⁹'
          else -> ch
        }
      }.joinToString("")
    return "$sign$supDigits"
  }
}
