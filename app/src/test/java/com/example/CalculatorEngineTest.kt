package com.example

import com.example.engine.AngleUnit
import com.example.engine.CalcErrorType
import com.example.engine.CalcVariable
import com.example.engine.DisplayMode
import com.example.engine.EvalContext
import com.example.engine.EvalOutcome
import com.example.engine.ExpressionParser
import com.example.engine.Fraction
import com.example.engine.FractionPreference
import com.example.engine.NumberFormatter
import com.example.engine.RegEntry
import com.example.engine.RegressionType
import com.example.engine.StatEntry
import com.example.engine.StatisticsEngine
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorEngineTest {

  private val parser = ExpressionParser()

  private fun evalValue(expr: String, ctx: EvalContext = EvalContext()): Double {
    val res = parser.evaluate(expr, ctx)
    assertTrue("Expected Success for '$expr' but got $res", res is EvalOutcome.Success)
    return (res as EvalOutcome.Success).value.number
  }

  private fun evalError(expr: String, ctx: EvalContext = EvalContext()): CalcErrorType {
    val res = parser.evaluate(expr, ctx)
    assertTrue("Expected Failure for '$expr' but got $res", res is EvalOutcome.Failure)
    return (res as EvalOutcome.Failure).errorType
  }

  @Test
  fun `prompt accuracy suite`() {
    assertEquals(60.0, evalValue("25+35"), 1e-12)
    assertEquals(180.0, evalValue("12×15"), 1e-12)
    assertEquals(12.0, evalValue("√(144)"), 1e-12)
    assertEquals(25.0, evalValue("5²"), 1e-12)
    assertEquals(8.0, evalValue("2³"), 1e-12)
    assertEquals(0.5, evalValue("sin(30)", EvalContext(angleUnit = AngleUnit.DEG)), 1e-12)
    assertEquals(0.5, evalValue("cos(60)", EvalContext(angleUnit = AngleUnit.DEG)), 1e-12)
    assertEquals(1.0, evalValue("tan(45)", EvalContext(angleUnit = AngleUnit.DEG)), 1e-12)
    assertEquals(2.0, evalValue("log(100)"), 1e-12)
    assertEquals(1.0, evalValue("ln(e)"), 1e-12)
    assertEquals(120.0, evalValue("5!"), 1e-12)
    assertEquals(45.0, evalValue("10nCr2"), 1e-12)
    assertEquals(90.0, evalValue("10nPr2"), 1e-12)

    val fracOutcome = parser.evaluate("1⌟2+1⌟4", EvalContext()) as EvalOutcome.Success
    assertEquals(0.75, fracOutcome.value.number, 1e-12)
    assertEquals("3⌟4", fracOutcome.value.fraction?.format(FractionPreference.MIXED))
  }

  @Test
  fun `fraction arithmetic and conversions`() {
    val mixedOutcome = parser.evaluate("1⌟1⌟2+2⌟1⌟3", EvalContext()) as EvalOutcome.Success
    val frac = mixedOutcome.value.fraction
    assertNotNull(frac)
    assertEquals("3⌟5⌟6", frac!!.format(FractionPreference.MIXED))
    assertEquals("23⌟6", frac.format(FractionPreference.IMPROPER))

    val decToFrac = Fraction.fromDouble(0.125)
    assertNotNull(decToFrac)
    assertEquals("1⌟8", decToFrac!!.format(FractionPreference.MIXED))
  }

  @Test
  fun `all three angle modes and DRG overrides and DMS`() {
    assertEquals(1.0, evalValue("sin(90)", EvalContext(angleUnit = AngleUnit.DEG)), 1e-12)
    assertEquals(1.0, evalValue("sin(π÷2)", EvalContext(angleUnit = AngleUnit.RAD)), 1e-12)
    assertEquals(1.0, evalValue("sin(100)", EvalContext(angleUnit = AngleUnit.GRA)), 1e-12)

    // Explicit DRG overrides in RAD mode
    assertEquals(0.5, evalValue("sin(30°)", EvalContext(angleUnit = AngleUnit.RAD)), 1e-12)
    assertEquals(1.0, evalValue("sin(100ᵍ)", EvalContext(angleUnit = AngleUnit.RAD)), 1e-12)
    assertEquals(1.0, evalValue("sin((π÷2)ʳ)", EvalContext(angleUnit = AngleUnit.DEG)), 1e-12)

    // DMS entry & formatting
    assertEquals(12.5125, evalValue("12°30°45°", EvalContext(angleUnit = AngleUnit.DEG)), 1e-12)
    assertEquals("12°30′45″", NumberFormatter.formatDms(12.5125))
  }

  @Test
  fun `hyperbolic powers roots scientific notation and Pol Rec`() {
    assertEquals(0.0, evalValue("sinh(0)"), 1e-12)
    assertEquals(1.0, evalValue("cosh(0)"), 1e-12)
    assertEquals(2.0, evalValue("sinh⁻¹(sinh(2))"), 1e-10)
    assertEquals(2.0, evalValue("4ˣ√16"), 1e-12)
    assertEquals(3.0, evalValue("∛(27)"), 1e-12)
    assertEquals(0.25, evalValue("4⁻¹"), 1e-12)
    assertEquals(1500.0, evalValue("1.5ᴇ3"), 1e-12)
    assertEquals(1000.0, evalValue("10^(3)"), 1e-12)
    assertEquals(exp(2.0), evalValue("e^(2)"), 1e-12)
    assertEquals(-9.0, evalValue("−3²"), 1e-12)
    assertEquals(9.0, evalValue("(−3)²"), 1e-12)

    val pol = parser.evaluate("Pol(3,4)", EvalContext(angleUnit = AngleUnit.DEG)) as EvalOutcome.Success
    assertEquals(5.0, pol.value.number, 1e-12)
    assertEquals(5.0, pol.value.updatedVariables[CalcVariable.E]!!, 1e-12)
    assertEquals(53.130102354, pol.value.updatedVariables[CalcVariable.F]!!, 1e-6)
  }

  @Test
  fun `variables Ans and error handling`() {
    val ctx = EvalContext(variables = mapOf(CalcVariable.A to 4.0, CalcVariable.X to 5.0), ans = 10.0)
    assertEquals(30.0, evalValue("2A+3X+Ans−3", ctx), 1e-12)

    assertEquals(CalcErrorType.MathError, evalError("5÷0"))
    assertEquals(CalcErrorType.MathError, evalError("√(−4)"))
    assertEquals(CalcErrorType.MathError, evalError("log(0)"))
    assertEquals(CalcErrorType.MathError, evalError("sin⁻¹(2)"))
    assertEquals(CalcErrorType.MathError, evalError("70!"))
    assertEquals(CalcErrorType.SyntaxError, evalError("5++2"))
  }

  @Test
  fun `display modes FIX SCI NORM ENG`() {
    assertEquals("3.14", NumberFormatter.format(PI, DisplayMode.FIX, fixDigits = 2))
    assertEquals("3.14×10⁰⁰", NumberFormatter.format(PI, DisplayMode.SCI, sciDigits = 3))
    assertEquals("1.5×10⁰³", NumberFormatter.formatEngineering(1500.0, 0))
    assertEquals("1500×10⁰⁰", NumberFormatter.formatEngineering(1500.0, -1))
  }

  @Test
  fun `SD mode statistics calculations`() {
    val entries = listOf(StatEntry("1", 10.0), StatEntry("2", 20.0), StatEntry("3", 30.0, freq = 2))
    val sd = StatisticsEngine.computeSd(entries)
    assertEquals(4, sd.n)
    assertEquals(90.0, sd.sumX, 1e-12)
    assertEquals(2300.0, sd.sumX2, 1e-12)
    assertEquals(22.5, sd.mean, 1e-12)
    assertEquals(8.2915619758885, sd.popStdDev, 1e-9)
    assertEquals(9.57427107756338, sd.sampleStdDev, 1e-9)
  }

  @Test
  fun `REG mode all six regression models`() {
    // Linear: y = 1 + 2x
    val linPts = listOf(RegEntry("1", 1.0, 3.0), RegEntry("2", 2.0, 5.0), RegEntry("3", 3.0, 7.0))
    val lin = StatisticsEngine.computeReg(linPts, RegressionType.LIN)
    assertEquals(1.0, lin.a, 1e-9)
    assertEquals(2.0, lin.b, 1e-9)
    assertEquals(1.0, lin.r!!, 1e-9)
    assertEquals(11.0, StatisticsEngine.predictY(lin, 5.0), 1e-9)
    assertEquals(5.0, StatisticsEngine.predictX(lin, 11.0), 1e-9)

    // Logarithmic: y = 2 + 3*ln(x)
    val logPts = listOf(RegEntry("1", 1.0, 2.0), RegEntry("2", exp(1.0), 5.0), RegEntry("3", exp(2.0), 8.0))
    val logReg = StatisticsEngine.computeReg(logPts, RegressionType.LOG)
    assertEquals(2.0, logReg.a, 1e-9)
    assertEquals(3.0, logReg.b, 1e-9)

    // Exponential: y = 2 * e^(0.5*x)
    val expPts = listOf(RegEntry("1", 0.0, 2.0), RegEntry("2", 2.0, 2.0 * exp(1.0)), RegEntry("3", 4.0, 2.0 * exp(2.0)))
    val expReg = StatisticsEngine.computeReg(expPts, RegressionType.EXP)
    assertEquals(2.0, expReg.a, 1e-9)
    assertEquals(0.5, expReg.b, 1e-9)

    // Power: y = 3 * x^2
    val pwrPts = listOf(RegEntry("1", 1.0, 3.0), RegEntry("2", 2.0, 12.0), RegEntry("3", 4.0, 48.0))
    val pwrReg = StatisticsEngine.computeReg(pwrPts, RegressionType.PWR)
    assertEquals(3.0, pwrReg.a, 1e-9)
    assertEquals(2.0, pwrReg.b, 1e-9)

    // Inverse: y = 4 + 6/x
    val invPts = listOf(RegEntry("1", 1.0, 10.0), RegEntry("2", 2.0, 7.0), RegEntry("3", 3.0, 6.0))
    val invReg = StatisticsEngine.computeReg(invPts, RegressionType.INV)
    assertEquals(4.0, invReg.a, 1e-9)
    assertEquals(6.0, invReg.b, 1e-9)

    // Quadratic: y = 1 + 2x + 3x^2
    val quadPts = listOf(
      RegEntry("1", 0.0, 1.0),
      RegEntry("2", 1.0, 6.0),
      RegEntry("3", 2.0, 17.0),
      RegEntry("4", 3.0, 34.0),
    )
    val quadReg = StatisticsEngine.computeReg(quadPts, RegressionType.QUAD)
    assertEquals(1.0, quadReg.a, 1e-9)
    assertEquals(2.0, quadReg.b, 1e-9)
    assertEquals(3.0, quadReg.c!!, 1e-9)
    assertEquals(57.0, StatisticsEngine.predictY(quadReg, 4.0), 1e-9)
    assertEquals(ln(exp(1.0)), 1.0, 1e-12)
  }
}
