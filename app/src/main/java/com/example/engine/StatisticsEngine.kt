package com.example.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

data class SdSummary(
  val n: Int,
  val sumX: Double,
  val sumX2: Double,
  val mean: Double,
  val popStdDev: Double,
  val sampleStdDev: Double,
)

data class RegSummary(
  val type: RegressionType,
  val n: Int,
  val sumX: Double,
  val sumX2: Double,
  val sumY: Double,
  val sumY2: Double,
  val sumXY: Double,
  val meanX: Double,
  val meanY: Double,
  val xPopStdDev: Double,
  val xSampleStdDev: Double,
  val yPopStdDev: Double,
  val ySampleStdDev: Double,
  val a: Double,
  val b: Double,
  val c: Double?, // Only for Quadratic
  val r: Double?, // Correlation coefficient (null for Quad, or R for Quad)
)

object StatisticsEngine {

  fun computeSd(entries: List<StatEntry>): SdSummary {
    if (entries.isEmpty()) {
      return SdSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
    var n = 0
    var sumX = 0.0
    var sumX2 = 0.0
    for (entry in entries) {
      val f = entry.freq.coerceAtLeast(1)
      n += f
      sumX += entry.x * f
      sumX2 += entry.x * entry.x * f
    }
    if (n == 0) return SdSummary(0, 0.0, 0.0, 0.0, 0.0, 0.0)
    val mean = sumX / n
    var sqDiffSum = 0.0
    for (entry in entries) {
      val f = entry.freq.coerceAtLeast(1)
      val d = entry.x - mean
      sqDiffSum += d * d * f
    }
    val popStdDev = sqrt((sqDiffSum / n).coerceAtLeast(0.0))
    val sampleStdDev = if (n > 1) sqrt((sqDiffSum / (n - 1)).coerceAtLeast(0.0)) else 0.0
    return SdSummary(
      n = n,
      sumX = NumberFormatter.sanitize(sumX),
      sumX2 = NumberFormatter.sanitize(sumX2),
      mean = NumberFormatter.sanitize(mean),
      popStdDev = NumberFormatter.sanitize(popStdDev),
      sampleStdDev = NumberFormatter.sanitize(sampleStdDev),
    )
  }

  fun computeReg(entries: List<RegEntry>, type: RegressionType): RegSummary {
    if (entries.isEmpty()) {
      return RegSummary(
        type = type,
        n = 0,
        sumX = 0.0,
        sumX2 = 0.0,
        sumY = 0.0,
        sumY2 = 0.0,
        sumXY = 0.0,
        meanX = 0.0,
        meanY = 0.0,
        xPopStdDev = 0.0,
        xSampleStdDev = 0.0,
        yPopStdDev = 0.0,
        ySampleStdDev = 0.0,
        a = Double.NaN,
        b = Double.NaN,
        c = if (type == RegressionType.QUAD) Double.NaN else null,
        r = Double.NaN,
      )
    }

    var n = 0
    var sumX = 0.0
    var sumX2 = 0.0
    var sumY = 0.0
    var sumY2 = 0.0
    var sumXY = 0.0

    for (entry in entries) {
      val f = entry.freq.coerceAtLeast(1)
      n += f
      sumX += entry.x * f
      sumX2 += entry.x * entry.x * f
      sumY += entry.y * f
      sumY2 += entry.y * entry.y * f
      sumXY += entry.x * entry.y * f
    }

    val meanX = sumX / n
    val meanY = sumY / n
    var ssX = 0.0
    var ssY = 0.0
    for (entry in entries) {
      val f = entry.freq.coerceAtLeast(1)
      val dx = entry.x - meanX
      val dy = entry.y - meanY
      ssX += dx * dx * f
      ssY += dy * dy * f
    }
    val xPop = sqrt((ssX / n).coerceAtLeast(0.0))
    val xSample = if (n > 1) sqrt((ssX / (n - 1)).coerceAtLeast(0.0)) else 0.0
    val yPop = sqrt((ssY / n).coerceAtLeast(0.0))
    val ySample = if (n > 1) sqrt((ssY / (n - 1)).coerceAtLeast(0.0)) else 0.0

    var a = Double.NaN
    var b = Double.NaN
    var c: Double? = null
    var r: Double? = Double.NaN

    if (n >= 2) {
      if (type == RegressionType.QUAD) {
        if (n >= 3) {
          val quadResult = solveQuadraticRegression(entries)
          if (quadResult != null) {
            a = quadResult.a
            b = quadResult.b
            c = quadResult.c
            r = quadResult.r
          }
        }
      } else {
        val transformed = transformPairs(entries, type)
        if (transformed != null) {
          val (aRaw, bRaw, rRaw) = linearFit(transformed)
          when (type) {
            RegressionType.LIN,
            RegressionType.LOG,
            RegressionType.INV -> {
              a = aRaw
              b = bRaw
              r = rRaw
            }
            RegressionType.EXP,
            RegressionType.PWR -> {
              a = exp(aRaw)
              b = bRaw
              r = rRaw
            }
            RegressionType.QUAD -> Unit
          }
        }
      }
    }

    return RegSummary(
      type = type,
      n = n,
      sumX = NumberFormatter.sanitize(sumX),
      sumX2 = NumberFormatter.sanitize(sumX2),
      sumY = NumberFormatter.sanitize(sumY),
      sumY2 = NumberFormatter.sanitize(sumY2),
      sumXY = NumberFormatter.sanitize(sumXY),
      meanX = NumberFormatter.sanitize(meanX),
      meanY = NumberFormatter.sanitize(meanY),
      xPopStdDev = NumberFormatter.sanitize(xPop),
      xSampleStdDev = NumberFormatter.sanitize(xSample),
      yPopStdDev = NumberFormatter.sanitize(yPop),
      ySampleStdDev = NumberFormatter.sanitize(ySample),
      a = if (a.isFinite()) NumberFormatter.sanitize(a) else Double.NaN,
      b = if (b.isFinite()) NumberFormatter.sanitize(b) else Double.NaN,
      c = c?.let { if (it.isFinite()) NumberFormatter.sanitize(it) else Double.NaN },
      r = r?.let { if (it.isFinite()) NumberFormatter.sanitize(it) else Double.NaN },
    )
  }

  fun predictY(summary: RegSummary, x: Double): Double {
    if (!summary.a.isFinite() || !summary.b.isFinite()) {
      throw CalcException(CalcErrorType.MathError, "Insufficient regression data")
    }
    return when (summary.type) {
      RegressionType.LIN -> summary.a + summary.b * x
      RegressionType.LOG -> {
        if (x <= 0.0) throw CalcException(CalcErrorType.MathError, "Domain error for ln(x)")
        summary.a + summary.b * ln(x)
      }
      RegressionType.EXP -> summary.a * exp(summary.b * x)
      RegressionType.PWR -> {
        if (x <= 0.0) throw CalcException(CalcErrorType.MathError, "Domain error for x^B")
        summary.a * x.pow(summary.b)
      }
      RegressionType.INV -> {
        if (x == 0.0) throw CalcException(CalcErrorType.MathError, "Division by zero")
        summary.a + summary.b / x
      }
      RegressionType.QUAD -> {
        val cVal = summary.c ?: throw CalcException(CalcErrorType.MathError)
        if (!cVal.isFinite()) throw CalcException(CalcErrorType.MathError)
        summary.a + summary.b * x + cVal * x * x
      }
    }
  }

  fun predictX(summary: RegSummary, y: Double, rootIndex: Int = 1): Double {
    if (!summary.a.isFinite() || !summary.b.isFinite()) {
      throw CalcException(CalcErrorType.MathError, "Insufficient regression data")
    }
    return when (summary.type) {
      RegressionType.LIN -> {
        if (summary.b == 0.0) throw CalcException(CalcErrorType.MathError)
        (y - summary.a) / summary.b
      }
      RegressionType.LOG -> {
        if (summary.b == 0.0) throw CalcException(CalcErrorType.MathError)
        exp((y - summary.a) / summary.b)
      }
      RegressionType.EXP -> {
        if (summary.a == 0.0 || summary.b == 0.0 || y / summary.a <= 0.0) {
          throw CalcException(CalcErrorType.MathError)
        }
        ln(y / summary.a) / summary.b
      }
      RegressionType.PWR -> {
        if (summary.a == 0.0 || summary.b == 0.0 || y / summary.a <= 0.0) {
          throw CalcException(CalcErrorType.MathError)
        }
        (y / summary.a).pow(1.0 / summary.b)
      }
      RegressionType.INV -> {
        val denom = y - summary.a
        if (denom == 0.0) throw CalcException(CalcErrorType.MathError)
        summary.b / denom
      }
      RegressionType.QUAD -> {
        val cVal = summary.c ?: throw CalcException(CalcErrorType.MathError)
        if (!cVal.isFinite()) throw CalcException(CalcErrorType.MathError)
        if (abs(cVal) < 1e-14) {
          if (summary.b == 0.0) throw CalcException(CalcErrorType.MathError)
          return (y - summary.a) / summary.b
        }
        val disc = summary.b * summary.b - 4.0 * cVal * (summary.a - y)
        if (disc < 0.0) throw CalcException(CalcErrorType.MathError, "No real root")
        val sqrtD = sqrt(disc)
        if (rootIndex == 2) (-summary.b - sqrtD) / (2.0 * cVal)
        else (-summary.b + sqrtD) / (2.0 * cVal)
      }
    }
  }

  private fun transformPairs(entries: List<RegEntry>, type: RegressionType): List<Triple<Double, Double, Int>>? {
    val result = ArrayList<Triple<Double, Double, Int>>(entries.size)
    for (entry in entries) {
      val tx =
        when (type) {
          RegressionType.LIN,
          RegressionType.EXP -> entry.x
          RegressionType.LOG,
          RegressionType.PWR -> {
            if (entry.x <= 0.0) return null
            ln(entry.x)
          }
          RegressionType.INV -> {
            if (entry.x == 0.0) return null
            1.0 / entry.x
          }
          RegressionType.QUAD -> entry.x
        }
      val ty =
        when (type) {
          RegressionType.LIN,
          RegressionType.LOG,
          RegressionType.INV -> entry.y
          RegressionType.EXP,
          RegressionType.PWR -> {
            if (entry.y <= 0.0) return null
            ln(entry.y)
          }
          RegressionType.QUAD -> entry.y
        }
      result.add(Triple(tx, ty, entry.freq.coerceAtLeast(1)))
    }
    return result
  }

  private fun linearFit(pairs: List<Triple<Double, Double, Int>>): Triple<Double, Double, Double> {
    var n = 0.0
    var sx = 0.0
    var sy = 0.0
    var sxx = 0.0
    var syy = 0.0
    var sxy = 0.0
    for ((x, y, f) in pairs) {
      n += f
      sx += x * f
      sy += y * f
      sxx += x * x * f
      syy += y * y * f
      sxy += x * y * f
    }
    val denomX = n * sxx - sx * sx
    val denomY = n * syy - sy * sy
    if (abs(denomX) < 1e-15) {
      return Triple(Double.NaN, Double.NaN, Double.NaN)
    }
    val b = (n * sxy - sx * sy) / denomX
    val a = (sy - b * sx) / n
    val r =
      if (denomX > 0.0 && denomY > 0.0) {
        ((n * sxy - sx * sy) / sqrt(denomX * denomY)).coerceIn(-1.0, 1.0)
      } else {
        Double.NaN
      }
    return Triple(a, b, r)
  }

  private data class QuadSolution(val a: Double, val b: Double, val c: Double, val r: Double)

  private fun solveQuadraticRegression(entries: List<RegEntry>): QuadSolution? {
    var n = 0.0
    var sx = 0.0
    var sx2 = 0.0
    var sx3 = 0.0
    var sx4 = 0.0
    var sy = 0.0
    var sxy = 0.0
    var sx2y = 0.0
    var sy2 = 0.0

    for (entry in entries) {
      val f = entry.freq.coerceAtLeast(1).toDouble()
      val x = entry.x
      val y = entry.y
      val x2 = x * x
      n += f
      sx += x * f
      sx2 += x2 * f
      sx3 += x2 * x * f
      sx4 += x2 * x2 * f
      sy += y * f
      sxy += x * y * f
      sx2y += x2 * y * f
      sy2 += y * y * f
    }

    val mat =
      arrayOf(
        doubleArrayOf(n, sx, sx2, sy),
        doubleArrayOf(sx, sx2, sx3, sxy),
        doubleArrayOf(sx2, sx3, sx4, sx2y),
      )

    // Gaussian elimination with partial pivoting
    for (col in 0 until 3) {
      var pivotRow = col
      var maxVal = abs(mat[col][col])
      for (row in col + 1 until 3) {
        if (abs(mat[row][col]) > maxVal) {
          maxVal = abs(mat[row][col])
          pivotRow = row
        }
      }
      if (maxVal < 1e-14) return null
      if (pivotRow != col) {
        val tmp = mat[col]
        mat[col] = mat[pivotRow]
        mat[pivotRow] = tmp
      }
      for (row in col + 1 until 3) {
        val factor = mat[row][col] / mat[col][col]
        for (j in col until 4) {
          mat[row][j] -= factor * mat[col][j]
        }
      }
    }

    val c = mat[2][3] / mat[2][2]
    val b = (mat[1][3] - mat[1][2] * c) / mat[1][1]
    val a = (mat[0][3] - mat[0][1] * b - mat[0][2] * c) / mat[0][0]
    if (!a.isFinite() || !b.isFinite() || !c.isFinite()) return null

    // Coefficient of multiple correlation R = sqrt(1 - SS_res / SS_tot)
    val meanY = sy / n
    var ssTot = 0.0
    var ssRes = 0.0
    for (entry in entries) {
      val f = entry.freq.coerceAtLeast(1)
      val yHat = a + b * entry.x + c * entry.x * entry.x
      val res = entry.y - yHat
      val tot = entry.y - meanY
      ssRes += res * res * f
      ssTot += tot * tot * f
    }
    val r = if (ssTot > 1e-15) sqrt((1.0 - ssRes / ssTot).coerceIn(0.0, 1.0)) else 1.0
    return QuadSolution(a, b, c, r)
  }
}
