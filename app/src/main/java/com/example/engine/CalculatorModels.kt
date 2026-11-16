package com.example.engine

enum class AngleUnit(val label: String) {
  DEG("D"),
  RAD("R"),
  GRA("G"),
}

enum class CalculatorMode(val displayName: String, val badge: String) {
  COMP("COMP", "COMP"),
  SD("SD", "SD"),
  REG("REG", "REG"),
}

enum class RegressionType(val code: String, val title: String, val formula: String) {
  LIN("Lin", "Linear", "y = A + Bx"),
  LOG("Log", "Logarithmic", "y = A + B·ln(x)"),
  EXP("Exp", "Exponential", "y = A·e^(Bx)"),
  PWR("Pwr", "Power", "y = A·x^B"),
  INV("Inv", "Inverse", "y = A + B/x"),
  QUAD("Quad", "Quadratic", "y = A + Bx + Cx²"),
}

enum class DisplayMode {
  NORM1,
  NORM2,
  FIX,
  SCI,
}

enum class FractionPreference {
  MIXED, // a b/c
  IMPROPER, // d/c
}

enum class ThemeMode(val label: String) {
  SYSTEM("System"),
  DARK("Dark"),
  LIGHT("Light"),
}

enum class CalcVariable(val symbol: String) {
  A("A"),
  B("B"),
  C("C"),
  D("D"),
  E("E"),
  F("F"),
  X("X"),
  Y("Y"),
  M("M"),
}

data class HistoryEntry(
  val id: String,
  val expression: String,
  val formattedResult: String,
  val numericResult: Double,
  val angleUnit: AngleUnit,
  val mode: CalculatorMode,
  val timestampMs: Long,
)

data class StatEntry(
  val id: String,
  val x: Double,
  val freq: Int = 1,
)

data class RegEntry(
  val id: String,
  val x: Double,
  val y: Double,
  val freq: Int = 1,
)

data class CalcValue(
  val number: Double,
  val fraction: Fraction? = null,
  val secondaryPair: Pair<String, Double>? = null, // e.g. Pol -> "θ" to theta, Rec -> "Y" to y
  val primaryLabel: String? = null, // e.g. "r=" or "X="
  val updatedVariables: Map<CalcVariable, Double> = emptyMap(),
)

sealed class CalcErrorType(val title: String) {
  data object SyntaxError : CalcErrorType("Syntax ERROR")
  data object MathError : CalcErrorType("Math ERROR")
  data object StackError : CalcErrorType("Stack ERROR")
}

class CalcException(val errorType: CalcErrorType, message: String = errorType.title) :
  RuntimeException(message)

sealed class EvalOutcome {
  data class Success(val value: CalcValue) : EvalOutcome()
  data class Failure(val errorType: CalcErrorType, val detail: String) : EvalOutcome()
}
