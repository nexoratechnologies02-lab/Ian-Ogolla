package com.example.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

data class EvalContext(
  val angleUnit: AngleUnit = AngleUnit.DEG,
  val displayMode: DisplayMode = DisplayMode.NORM1,
  val fixDigits: Int = 2,
  val sciDigits: Int = 3,
  val variables: Map<CalcVariable, Double> = emptyMap(),
  val ans: Double = 0.0,
  val sdSummary: SdSummary? = null,
  val regSummary: RegSummary? = null,
  val randomProvider: () -> Double = {
    // 3-digit Casio-style Ran# in [0.000, 0.999]
    floor(Math.random() * 1000.0) / 1000.0
  },
)

/**
 * Custom recursive-descent expression parser and evaluator inspired by Casio fx-82MS 2nd Edition.
 * Never uses JavaScript or external eval().
 */
class ExpressionParser {

  private enum class TokenType {
    NUMBER,
    FRACTION_SEP, // ⌟
    IDENT, // sin, cos, log, A, B, Ans, π, e, nPr, nCr, Ran#, etc.
    PLUS,
    MINUS,
    MUL,
    DIV,
    POW, // ^
    ROOT, // ˣ√
    N_PR, // 𝐏
    N_CR, // 𝐂
    LPAREN,
    RPAREN,
    COMMA,
    POST_SQ, // ²
    POST_CUBE, // ³
    POST_INV, // ⁻¹
    POST_FACT, // !
    POST_PCT, // %
    DEG_SYM, // °
    RAD_SYM, // ʳ
    GRA_SYM, // ᵍ
    EOF,
  }

  private data class Token(val type: TokenType, val text: String, val pos: Int)

  private data class ExprVal(
    val number: Double,
    val fraction: Fraction? = null,
  )

  fun evaluate(expression: String, context: EvalContext): EvalOutcome {
    val trimmed = expression.trim()
    if (trimmed.isEmpty()) {
      return EvalOutcome.Failure(CalcErrorType.SyntaxError, "Empty expression")
    }
    return try {
      val tokens = tokenize(trimmed)
      val containsFractionLiteral = tokens.any { it.type == TokenType.FRACTION_SEP }
      val updatedVars = mutableMapOf<CalcVariable, Double>()
      val parser = Parser(tokens, context, updatedVars)
      val result = parser.parseFull()
      if (!result.number.isFinite() || abs(result.number) >= 1e100) {
        return EvalOutcome.Failure(CalcErrorType.MathError, "Out of range")
      }
      val sanitized = NumberFormatter.sanitize(result.number)
      val finalFraction =
        result.fraction
          ?: if (containsFractionLiteral) Fraction.fromDouble(sanitized) else null
      EvalOutcome.Success(
        CalcValue(
          number = sanitized,
          fraction = finalFraction,
          secondaryPair = parser.lastSecondaryPair,
          primaryLabel = parser.lastPrimaryLabel,
          updatedVariables = updatedVars,
        )
      )
    } catch (e: CalcException) {
      EvalOutcome.Failure(e.errorType, e.message ?: e.errorType.title)
    } catch (e: Exception) {
      EvalOutcome.Failure(CalcErrorType.SyntaxError, e.message ?: "Syntax ERROR")
    }
  }

  private fun tokenize(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    val len = input.length

    while (i < len) {
      val ch = input[i]
      if (ch.isWhitespace()) {
        i++
        continue
      }

      // Check multi-char postfix / operators first
      if (input.startsWith("⁻¹", i)) {
        tokens.add(Token(TokenType.POST_INV, "⁻¹", i))
        i += 2
        continue
      }
      if (input.startsWith("ˣ√", i)) {
        tokens.add(Token(TokenType.ROOT, "ˣ√", i))
        i += 2
        continue
      }
      if (input.startsWith("×10^", i)) {
        // Handle explicit "×10^" inside literal if user typed it, otherwise handled as MUL + 10 + POW
        tokens.add(Token(TokenType.MUL, "×", i))
        i++
        continue
      }
      if (input.startsWith("𝐏", i)) {
        tokens.add(Token(TokenType.N_PR, "𝐏", i))
        i += 2
        continue
      }
      if (input.startsWith("𝐂", i)) {
        tokens.add(Token(TokenType.N_CR, "𝐂", i))
        i += 2
        continue
      }

      // Number literal (including optional scientific 'ᴇ' / 'E' exponent inside literal)
      if (ch.isDigit() || ch == '.') {
        val start = i
        var hasDot = false
        while (i < len && (input[i].isDigit() || (input[i] == '.' && !hasDot))) {
          if (input[i] == '.') hasDot = true
          i++
        }
        // Check Casio EXP symbol 'ᴇ' (U+1D07)
        if (i < len && input[i] == 'ᴇ') {
          i++
          if (i < len && (input[i] == '+' || input[i] == '-' || input[i] == '−')) {
            i++
          }
          val expStart = i
          while (i < len && input[i].isDigit()) {
            i++
          }
          if (i == expStart) {
            throw CalcException(CalcErrorType.SyntaxError, "Incomplete exponent")
          }
        }
        val numText = input.substring(start, i)
        if (numText == ".") throw CalcException(CalcErrorType.SyntaxError, "Invalid decimal")
        tokens.add(Token(TokenType.NUMBER, numText, start))
        continue
      }

      // Standalone ᴇ (treat as 1ᴇ...)
      if (ch == 'ᴇ') {
        val start = i
        i++
        if (i < len && (input[i] == '+' || input[i] == '-' || input[i] == '−')) {
          i++
        }
        val expStart = i
        while (i < len && input[i].isDigit()) i++
        if (i == expStart) throw CalcException(CalcErrorType.SyntaxError, "Incomplete exponent")
        tokens.add(Token(TokenType.NUMBER, "1" + input.substring(start, i), start))
        continue
      }

      when (ch) {
        '+' -> {
          tokens.add(Token(TokenType.PLUS, "+", i))
          i++
        }
        '-', '−' -> {
          tokens.add(Token(TokenType.MINUS, "−", i))
          i++
        }
        '*', '×' -> {
          tokens.add(Token(TokenType.MUL, "×", i))
          i++
        }
        '/', '÷' -> {
          tokens.add(Token(TokenType.DIV, "÷", i))
          i++
        }
        '^' -> {
          tokens.add(Token(TokenType.POW, "^", i))
          i++
        }
        '(' -> {
          tokens.add(Token(TokenType.LPAREN, "(", i))
          i++
        }
        ')' -> {
          tokens.add(Token(TokenType.RPAREN, ")", i))
          i++
        }
        ',' -> {
          tokens.add(Token(TokenType.COMMA, ",", i))
          i++
        }
        '⌟' -> {
          tokens.add(Token(TokenType.FRACTION_SEP, "⌟", i))
          i++
        }
        '²' -> {
          tokens.add(Token(TokenType.POST_SQ, "²", i))
          i++
        }
        '³' -> {
          tokens.add(Token(TokenType.POST_CUBE, "³", i))
          i++
        }
        '!' -> {
          tokens.add(Token(TokenType.POST_FACT, "!", i))
          i++
        }
        '%' -> {
          tokens.add(Token(TokenType.POST_PCT, "%", i))
          i++
        }
        '°' -> {
          tokens.add(Token(TokenType.DEG_SYM, "°", i))
          i++
        }
        'ʳ' -> {
          tokens.add(Token(TokenType.RAD_SYM, "ʳ", i))
          i++
        }
        'ᵍ' -> {
          tokens.add(Token(TokenType.GRA_SYM, "ᵍ", i))
          i++
        }
        'π' -> {
          tokens.add(Token(TokenType.IDENT, "π", i))
          i++
        }
        '√' -> {
          tokens.add(Token(TokenType.IDENT, "√", i))
          i++
        }
        '∛' -> {
          tokens.add(Token(TokenType.IDENT, "∛", i))
          i++
        }
        else -> {
          // Match known multi-character symbols / identifiers
          val matchedSymbol = matchKnownIdent(input, i)
          if (matchedSymbol != null) {
            val type =
              when (matchedSymbol) {
                "nPr" -> TokenType.N_PR
                "nCr" -> TokenType.N_CR
                else -> TokenType.IDENT
              }
            tokens.add(Token(type, matchedSymbol, i))
            i += matchedSymbol.length
          } else {
            throw CalcException(CalcErrorType.SyntaxError, "Unexpected character '$ch'")
          }
        }
      }
    }

    tokens.add(Token(TokenType.EOF, "", len))
    return tokens
  }

  private fun matchKnownIdent(input: String, pos: Int): String? {
    val candidates =
      listOf(
        "sinh⁻¹",
        "cosh⁻¹",
        "tanh⁻¹",
        "sin⁻¹",
        "cos⁻¹",
        "tan⁻¹",
        "sinh",
        "cosh",
        "tanh",
        "sin",
        "cos",
        "tan",
        "log",
        "ln",
        "Abs",
        "Pol",
        "Rec",
        "Ran#",
        "Rnd",
        "Ans",
        "nPr",
        "nCr",
        "xσn-1",
        "xσn",
        "yσn-1",
        "yσn",
        "Σx²",
        "Σy²",
        "Σxy",
        "Σx",
        "Σy",
        "x̄",
        "ȳ",
        "RegA",
        "RegB",
        "RegC",
        "x̂1",
        "x̂2",
        "x̂",
        "ŷ",
        "10^",
        "e^",
      )
    for (c in candidates) {
      if (input.startsWith(c, pos)) return c
    }
    val ch = input[pos]
    if (ch in listOf('A', 'B', 'C', 'D', 'E', 'F', 'X', 'Y', 'M', 'e', 'n', 'r')) {
      return ch.toString()
    }
    return null
  }

  private inner class Parser(
    private val tokens: List<Token>,
    private val context: EvalContext,
    private val updatedVars: MutableMap<CalcVariable, Double>,
  ) {
    private var idx = 0
    private var depth = 0
    var lastSecondaryPair: Pair<String, Double>? = null
    var lastPrimaryLabel: String? = null

    private fun peek(offset: Int = 0): Token =
      tokens[(idx + offset).coerceAtMost(tokens.lastIndex)]

    private fun advance(): Token {
      val t = peek()
      if (idx < tokens.lastIndex) idx++
      return t
    }

    fun parseFull(): ExprVal {
      val res = parseAddSub()
      if (peek().type != TokenType.EOF) {
        throw CalcException(CalcErrorType.SyntaxError, "Unexpected '${peek().text}'")
      }
      return res
    }

    private fun parseAddSub(): ExprVal {
      var left = parseMulDiv()
      while (peek().type == TokenType.PLUS || peek().type == TokenType.MINUS) {
        val op = advance().type
        if (peek().type == TokenType.PLUS) {
          throw CalcException(CalcErrorType.SyntaxError, "Unexpected '+'")
        }
        val right = parseMulDiv()
        left =
          if (op == TokenType.PLUS) addVals(left, right)
          else subVals(left, right)
      }
      return left
    }

    private fun parseMulDiv(): ExprVal {
      var left = parseUnary()
      while (true) {
        when (peek().type) {
          TokenType.MUL -> {
            advance()
            val right = parseUnary()
            left = mulVals(left, right)
          }
          TokenType.DIV -> {
            advance()
            val right = parseUnary()
            left = divVals(left, right)
          }
          else -> {
            if (startsImplicitFactor(peek())) {
              val right = parseUnary()
              left = mulVals(left, right)
            } else {
              break
            }
          }
        }
      }
      return left
    }

    private fun startsImplicitFactor(token: Token): Boolean =
      when (token.type) {
        TokenType.LPAREN,
        TokenType.IDENT,
        TokenType.NUMBER -> true
        else -> false
      }

    private fun parseUnary(): ExprVal {
      return when (peek().type) {
        TokenType.PLUS -> {
          advance()
          parseUnary()
        }
        TokenType.MINUS -> {
          advance()
          val v = parseUnary()
          ExprVal(-v.number, v.fraction?.unaryMinus())
        }
        else -> parseCombPerm()
      }
    }

    private fun parseCombPerm(): ExprVal {
      var left = parsePower()
      while (peek().type == TokenType.N_PR || peek().type == TokenType.N_CR) {
        val op = advance().type
        val right = parsePower()
        val nLong = toNonNegativeInt(left.number)
        val rLong = toNonNegativeInt(right.number)
        if (rLong > nLong) throw CalcException(CalcErrorType.MathError, "r > n")
        val value = if (op == TokenType.N_PR) nPr(nLong, rLong) else nCr(nLong, rLong)
        left = ExprVal(value, Fraction.fromDouble(value))
      }
      return left
    }

    private fun parsePower(): ExprVal {
      val base = parsePostfix()
      return when (peek().type) {
        TokenType.POW -> {
          advance()
          val exponent = parseUnary()
          powVals(base, exponent)
        }
        TokenType.ROOT -> {
          advance()
          val radicand = parseUnary()
          rootVal(base.number, radicand.number)
        }
        else -> base
      }
    }

    private fun parsePostfix(): ExprVal {
      var value = parsePrimaryWithFractionOrDms()
      while (true) {
        value =
          when (peek().type) {
            TokenType.POST_SQ -> {
              advance()
              val sq = value.number * value.number
              val frac = value.fraction?.let { it * it }
              ExprVal(sq, frac)
            }
            TokenType.POST_CUBE -> {
              advance()
              val cube = value.number * value.number * value.number
              val frac = value.fraction?.pow(3)
              ExprVal(cube, frac)
            }
            TokenType.POST_INV -> {
              advance()
              if (value.number == 0.0) throw CalcException(CalcErrorType.MathError, "Division by zero")
              val inv = 1.0 / value.number
              val frac = value.fraction?.pow(-1)
              ExprVal(inv, frac)
            }
            TokenType.POST_FACT -> {
              advance()
              val n = toNonNegativeInt(value.number)
              if (n > 69) throw CalcException(CalcErrorType.MathError, "Factorial overflow")
              val fact = factorial(n)
              ExprVal(fact, Fraction.fromDouble(fact))
            }
            TokenType.POST_PCT -> {
              advance()
              val pct = value.number / 100.0
              val frac = value.fraction?.div(Fraction.of(100))
              ExprVal(pct, frac)
            }
            TokenType.RAD_SYM -> {
              advance()
              ExprVal(convertAngle(value.number, AngleUnit.RAD, context.angleUnit), null)
            }
            TokenType.GRA_SYM -> {
              advance()
              ExprVal(convertAngle(value.number, AngleUnit.GRA, context.angleUnit), null)
            }
            else -> return value
          }
      }
    }

    private fun parsePrimaryWithFractionOrDms(): ExprVal {
      val first = parsePrimary()

      // Check for Casio fraction literal: a⌟b or a⌟b⌟c
      if (peek().type == TokenType.FRACTION_SEP) {
        val a = first.fraction?.takeIf { it.isInteger }?.numerator?.toLong()
          ?: toExactInteger(first.number)
        advance() // consume ⌟
        val second = parsePrimary()
        val b = second.fraction?.takeIf { it.isInteger }?.numerator?.toLong()
          ?: toExactInteger(second.number)
        if (peek().type == TokenType.FRACTION_SEP) {
          advance() // consume second ⌟
          val third = parsePrimary()
          val c = third.fraction?.takeIf { it.isInteger }?.numerator?.toLong()
            ?: toExactInteger(third.number)
          val mixed = Fraction.fromMixed(a, b, c)
          return ExprVal(mixed.toDouble(), mixed)
        } else {
          val simple = Fraction.of(a, b)
          return ExprVal(simple.toDouble(), simple)
        }
      }

      // Check for DMS or degree override: 30° or 12°30° or 12°30°45°
      if (peek().type == TokenType.DEG_SYM) {
        val degVal = first.number
        advance() // consume first °
        val next = peek()
        val isMultiSegmentDms = next.type == TokenType.NUMBER
        return if (isMultiSegmentDms) {
          val minVal = parsePrimary().number
          if (peek().type == TokenType.DEG_SYM) advance()
          val secVal =
            if (peek().type == TokenType.NUMBER || peek().type == TokenType.LPAREN) {
              val s = parsePrimary().number
              if (peek().type == TokenType.DEG_SYM) advance()
              s
            } else {
              0.0
            }
          val sign = if (degVal < 0) -1.0 else 1.0
          val totalDeg = sign * (abs(degVal) + minVal / 60.0 + secVal / 3600.0)
          ExprVal(convertAngle(totalDeg, AngleUnit.DEG, context.angleUnit), null)
        } else {
          ExprVal(convertAngle(degVal, AngleUnit.DEG, context.angleUnit), null)
        }
      }

      return first
    }

    private fun parsePrimary(): ExprVal {
      depth++
      if (depth > 80) throw CalcException(CalcErrorType.StackError, "Nesting too deep")
      try {
        val tok = peek()
        return when (tok.type) {
          TokenType.NUMBER -> {
            advance()
            parseNumberLiteral(tok.text)
          }
          TokenType.LPAREN -> {
            advance()
            val inner = parseAddSub()
            // Auto-close missing ')' at EOF just like Casio fx-82MS
            if (peek().type == TokenType.RPAREN) {
              advance()
            } else if (peek().type != TokenType.EOF) {
              throw CalcException(CalcErrorType.SyntaxError, "Missing ')'")
            }
            inner
          }
          TokenType.IDENT -> parseIdentifier()
          else -> throw CalcException(CalcErrorType.SyntaxError, "Unexpected '${tok.text}'")
        }
      } finally {
        depth--
      }
    }

    private fun parseIdentifier(): ExprVal {
      val tok = advance()
      return when (tok.text) {
        "π" -> ExprVal(PI, null)
        "e" -> ExprVal(kotlin.math.E, null)
        "Ans" -> ExprVal(context.ans, Fraction.fromDouble(context.ans))
        "Ran#" -> {
          val r = context.randomProvider()
          ExprVal(r, Fraction.fromDouble(r))
        }
        "A" -> resolveVar(CalcVariable.A)
        "B" -> resolveVar(CalcVariable.B)
        "C" -> resolveVar(CalcVariable.C)
        "RegA" -> {
          val v = context.regSummary?.a
          if (v == null || !v.isFinite()) throw CalcException(CalcErrorType.MathError, "No regression fit")
          ExprVal(v)
        }
        "RegB" -> {
          val v = context.regSummary?.b
          if (v == null || !v.isFinite()) throw CalcException(CalcErrorType.MathError, "No regression fit")
          ExprVal(v)
        }
        "RegC" -> {
          val v = context.regSummary?.c
          if (v == null || !v.isFinite()) throw CalcException(CalcErrorType.MathError, "No quadratic fit")
          ExprVal(v)
        }
        "D" -> resolveVar(CalcVariable.D)
        "E" -> resolveVar(CalcVariable.E)
        "F" -> resolveVar(CalcVariable.F)
        "X" -> resolveVar(CalcVariable.X)
        "Y" -> resolveVar(CalcVariable.Y)
        "M" -> resolveVar(CalcVariable.M)
        // Stat variables
        "n" -> {
          val nVal = context.regSummary?.n ?: context.sdSummary?.n ?: 0
          ExprVal(nVal.toDouble(), Fraction.of(nVal.toLong()))
        }
        "Σx" -> ExprVal(context.regSummary?.sumX ?: context.sdSummary?.sumX ?: 0.0)
        "Σx²" -> ExprVal(context.regSummary?.sumX2 ?: context.sdSummary?.sumX2 ?: 0.0)
        "x̄" -> {
          val sd = context.sdSummary
          val reg = context.regSummary
          val v =
            when {
              reg != null && reg.n > 0 -> reg.meanX
              sd != null && sd.n > 0 -> sd.mean
              else -> throw CalcException(CalcErrorType.MathError, "No stat data")
            }
          ExprVal(v)
        }
        "xσn" -> {
          val sd = context.sdSummary
          val reg = context.regSummary
          val v =
            when {
              reg != null && reg.n > 0 -> reg.xPopStdDev
              sd != null && sd.n > 0 -> sd.popStdDev
              else -> throw CalcException(CalcErrorType.MathError, "No stat data")
            }
          ExprVal(v)
        }
        "xσn-1" -> {
          val sd = context.sdSummary
          val reg = context.regSummary
          val v =
            when {
              reg != null && reg.n > 1 -> reg.xSampleStdDev
              sd != null && sd.n > 1 -> sd.sampleStdDev
              else -> throw CalcException(CalcErrorType.MathError, "Need n ≥ 2")
            }
          ExprVal(v)
        }
        "Σy" -> ExprVal(context.regSummary?.sumY ?: 0.0)
        "Σy²" -> ExprVal(context.regSummary?.sumY2 ?: 0.0)
        "Σxy" -> ExprVal(context.regSummary?.sumXY ?: 0.0)
        "ȳ" -> {
          val reg = context.regSummary
          if (reg == null || reg.n == 0) throw CalcException(CalcErrorType.MathError)
          ExprVal(reg.meanY)
        }
        "yσn" -> {
          val reg = context.regSummary
          if (reg == null || reg.n == 0) throw CalcException(CalcErrorType.MathError)
          ExprVal(reg.yPopStdDev)
        }
        "yσn-1" -> {
          val reg = context.regSummary
          if (reg == null || reg.n <= 1) throw CalcException(CalcErrorType.MathError)
          ExprVal(reg.ySampleStdDev)
        }
        "r" -> {
          val rVal = context.regSummary?.r
          if (rVal == null || !rVal.isFinite()) throw CalcException(CalcErrorType.MathError)
          ExprVal(rVal)
        }
        "x̂", "x̂1" -> {
          val arg = parseSingleArgOrScalar()
          val reg = context.regSummary ?: throw CalcException(CalcErrorType.MathError)
          ExprVal(StatisticsEngine.predictX(reg, arg.number, 1))
        }
        "x̂2" -> {
          val arg = parseSingleArgOrScalar()
          val reg = context.regSummary ?: throw CalcException(CalcErrorType.MathError)
          ExprVal(StatisticsEngine.predictX(reg, arg.number, 2))
        }
        "ŷ" -> {
          val arg = parseSingleArgOrScalar()
          val reg = context.regSummary ?: throw CalcException(CalcErrorType.MathError)
          ExprVal(StatisticsEngine.predictY(reg, arg.number))
        }
        "Pol" -> {
          val (x, y) = parseTwoArgs()
          val r = hypot(x, y)
          val thetaRad = atan2(y, x)
          val theta = fromRadians(thetaRad, context.angleUnit)
          updatedVars[CalcVariable.E] = r
          updatedVars[CalcVariable.F] = theta
          lastPrimaryLabel = "r="
          lastSecondaryPair = "θ" to theta
          ExprVal(r)
        }
        "Rec" -> {
          val (r, theta) = parseTwoArgs()
          if (r < 0.0) throw CalcException(CalcErrorType.MathError, "r < 0")
          val thetaRad = toRadians(theta, context.angleUnit)
          val x = NumberFormatter.sanitize(r * cos(thetaRad))
          val y = NumberFormatter.sanitize(r * sin(thetaRad))
          updatedVars[CalcVariable.E] = x
          updatedVars[CalcVariable.F] = y
          lastPrimaryLabel = "X="
          lastSecondaryPair = "Y" to y
          ExprVal(x)
        }
        "10^" -> {
          val arg = parseFunctionArg()
          ExprVal(10.0.pow(arg.number))
        }
        "e^" -> {
          val arg = parseFunctionArg()
          ExprVal(exp(arg.number))
        }
        else -> evalSingleArgFunction(tok.text)
      }
    }

    private fun resolveVar(v: CalcVariable): ExprVal {
      val value = updatedVars[v] ?: context.variables[v] ?: 0.0
      return ExprVal(value, Fraction.fromDouble(value))
    }

    private fun resolveVarOrRegCoeff(v: CalcVariable, regCoeff: Double?): ExprVal {
      // Standard variables A-F store user memory unless explicitly queried via S-VAR;
      // user memory A-F always returns stored variable value.
      val value = updatedVars[v] ?: context.variables[v] ?: 0.0
      return ExprVal(value, Fraction.fromDouble(value))
    }

    private fun parseFunctionArg(): ExprVal {
      return if (peek().type == TokenType.LPAREN) {
        advance()
        val inner = parseAddSub()
        if (peek().type == TokenType.RPAREN) {
          advance()
        } else if (peek().type != TokenType.EOF) {
          throw CalcException(CalcErrorType.SyntaxError, "Missing ')'")
        }
        inner
      } else {
        // Casio allows sin 30 without parens!
        parseUnary()
      }
    }

    private fun parseSingleArgOrScalar(): ExprVal = parseFunctionArg()

    private fun parseTwoArgs(): Pair<Double, Double> {
      if (peek().type != TokenType.LPAREN) {
        throw CalcException(CalcErrorType.SyntaxError, "Expected '('")
      }
      advance()
      val first = parseAddSub()
      if (peek().type != TokenType.COMMA) {
        throw CalcException(CalcErrorType.SyntaxError, "Expected ','")
      }
      advance()
      val second = parseAddSub()
      if (peek().type == TokenType.RPAREN) {
        advance()
      } else if (peek().type != TokenType.EOF) {
        throw CalcException(CalcErrorType.SyntaxError, "Expected ')'")
      }
      return first.number to second.number
    }

    private fun evalSingleArgFunction(name: String): ExprVal {
      val arg = parseFunctionArg()
      val x = arg.number
      val result =
        when (name) {
          "sin" -> sin(toRadians(x, context.angleUnit))
          "cos" -> cos(toRadians(x, context.angleUnit))
          "tan" -> {
            val rad = toRadians(x, context.angleUnit)
            val c = cos(rad)
            if (abs(c) < 1e-13) throw CalcException(CalcErrorType.MathError, "tan undefined")
            tan(rad)
          }
          "sin⁻¹" -> {
            if (x < -1.0 || x > 1.0) throw CalcException(CalcErrorType.MathError, "Domain error")
            fromRadians(asin(x), context.angleUnit)
          }
          "cos⁻¹" -> {
            if (x < -1.0 || x > 1.0) throw CalcException(CalcErrorType.MathError, "Domain error")
            fromRadians(acos(x), context.angleUnit)
          }
          "tan⁻¹" -> fromRadians(atan(x), context.angleUnit)
          "sinh" -> sinh(x)
          "cosh" -> cosh(x)
          "tanh" -> tanh(x)
          "sinh⁻¹" -> asinh(x)
          "cosh⁻¹" -> {
            if (x < 1.0) throw CalcException(CalcErrorType.MathError, "Domain error")
            acosh(x)
          }
          "tanh⁻¹" -> {
            if (abs(x) >= 1.0) throw CalcException(CalcErrorType.MathError, "Domain error")
            atanh(x)
          }
          "log" -> {
            if (x <= 0.0) throw CalcException(CalcErrorType.MathError, "Domain error")
            log10(x)
          }
          "ln" -> {
            if (x <= 0.0) throw CalcException(CalcErrorType.MathError, "Domain error")
            ln(x)
          }
          "√" -> {
            if (x < 0.0) throw CalcException(CalcErrorType.MathError, "Square root of negative")
            sqrt(x)
          }
          "∛" -> cbrt(x)
          "Abs" -> abs(x)
          "Rnd" -> roundToDisplay(x, context)
          else -> throw CalcException(CalcErrorType.SyntaxError, "Unknown function $name")
        }
      if (!result.isFinite()) throw CalcException(CalcErrorType.MathError)
      val clean = NumberFormatter.sanitize(result)
      return ExprVal(clean, null)
    }

    private fun parseNumberLiteral(text: String): ExprVal {
      val parts = text.split('ᴇ')
      return if (parts.size == 2) {
        val mantissa = parts[0].toDoubleOrNull()
          ?: throw CalcException(CalcErrorType.SyntaxError, "Bad number")
        val expNum = parts[1].replace('−', '-').toIntOrNull()
          ?: throw CalcException(CalcErrorType.SyntaxError, "Bad exponent")
        if (abs(expNum) > 99) throw CalcException(CalcErrorType.MathError, "Exponent overflow")
        val v = mantissa * 10.0.pow(expNum)
        ExprVal(v, null)
      } else {
        val v = text.toDoubleOrNull()
          ?: throw CalcException(CalcErrorType.SyntaxError, "Bad number")
        val frac =
          if (!text.contains('.')) {
            text.toLongOrNull()?.let { Fraction.of(it) }
          } else {
            Fraction.fromDouble(v)
          }
        ExprVal(v, frac)
      }
    }

    private fun addVals(a: ExprVal, b: ExprVal): ExprVal {
      val f = if (a.fraction != null && b.fraction != null) safeFracOp { a.fraction + b.fraction } else null
      return ExprVal(a.number + b.number, f)
    }

    private fun subVals(a: ExprVal, b: ExprVal): ExprVal {
      val f = if (a.fraction != null && b.fraction != null) safeFracOp { a.fraction - b.fraction } else null
      return ExprVal(a.number - b.number, f)
    }

    private fun mulVals(a: ExprVal, b: ExprVal): ExprVal {
      val f = if (a.fraction != null && b.fraction != null) safeFracOp { a.fraction * b.fraction } else null
      return ExprVal(a.number * b.number, f)
    }

    private fun divVals(a: ExprVal, b: ExprVal): ExprVal {
      if (b.number == 0.0) throw CalcException(CalcErrorType.MathError, "Division by zero")
      val f = if (a.fraction != null && b.fraction != null) safeFracOp { a.fraction / b.fraction } else null
      return ExprVal(a.number / b.number, f)
    }

    private fun powVals(a: ExprVal, b: ExprVal): ExprVal {
      if (a.number == 0.0 && b.number <= 0.0) {
        throw CalcException(CalcErrorType.MathError, "0^0 or 0^-n undefined")
      }
      val expInt = b.fraction?.takeIf { it.isInteger }?.numerator?.toInt()
      val frac =
        if (a.fraction != null && expInt != null && abs(expInt) <= 20) {
          safeFracOp { a.fraction.pow(expInt) }
        } else {
          null
        }
      val res =
        if (a.number < 0.0 && abs(b.number - round(b.number)) > 1e-12) {
          throw CalcException(CalcErrorType.MathError, "Negative base fractional power")
        } else {
          a.number.pow(b.number)
        }
      if (!res.isFinite()) throw CalcException(CalcErrorType.MathError)
      return ExprVal(res, frac)
    }

    private fun rootVal(n: Double, x: Double): ExprVal {
      if (n == 0.0) throw CalcException(CalcErrorType.MathError, "0th root undefined")
      val isOddInt = abs(n - round(n)) < 1e-12 && abs(round(n).toLong() % 2L) == 1L
      val res =
        if (x < 0.0) {
          if (!isOddInt) throw CalcException(CalcErrorType.MathError, "Even root of negative")
          -((-x).pow(1.0 / n))
        } else {
          x.pow(1.0 / n)
        }
      if (!res.isFinite()) throw CalcException(CalcErrorType.MathError)
      return ExprVal(res, null)
    }

    private inline fun safeFracOp(block: () -> Fraction): Fraction? {
      return try {
        val f = block()
        if (f.denominator.bitLength() <= 60 && f.numerator.bitLength() <= 60) f else null
      } catch (e: CalcException) {
        throw e
      } catch (_: Exception) {
        null
      }
    }

    private fun toRadians(angle: Double, unit: AngleUnit): Double =
      when (unit) {
        AngleUnit.DEG -> angle * PI / 180.0
        AngleUnit.RAD -> angle
        AngleUnit.GRA -> angle * PI / 200.0
      }

    private fun fromRadians(rad: Double, unit: AngleUnit): Double =
      when (unit) {
        AngleUnit.DEG -> rad * 180.0 / PI
        AngleUnit.RAD -> rad
        AngleUnit.GRA -> rad * 200.0 / PI
      }

    private fun convertAngle(value: Double, from: AngleUnit, to: AngleUnit): Double {
      if (from == to) return value
      return fromRadians(toRadians(value, from), to)
    }

    private fun toNonNegativeInt(v: Double): Int {
      val r = round(v)
      if (v < -1e-12 || abs(v - r) > 1e-10 || r > Int.MAX_VALUE) {
        throw CalcException(CalcErrorType.MathError, "Expected non-negative integer")
      }
      return r.toInt()
    }

    private fun toExactInteger(v: Double): Long {
      val r = round(v)
      if (abs(v - r) > 1e-10) {
        throw CalcException(CalcErrorType.SyntaxError, "Fraction parts must be integers")
      }
      return r.toLong()
    }

    private fun factorial(n: Int): Double {
      var acc = 1.0
      for (k in 2..n) acc *= k
      return acc
    }

    private fun nPr(n: Int, r: Int): Double {
      var acc = 1.0
      for (k in 0 until r) {
        acc *= (n - k)
        if (!acc.isFinite() || acc >= 1e100) throw CalcException(CalcErrorType.MathError)
      }
      return acc
    }

    private fun nCr(n: Int, r: Int): Double {
      val k = minOf(r, n - r)
      var acc = 1.0
      for (i in 1..k) {
        acc = acc * (n - k + i) / i
        if (!acc.isFinite() || acc >= 1e100) throw CalcException(CalcErrorType.MathError)
      }
      return round(acc)
    }

    private fun roundToDisplay(x: Double, ctx: EvalContext): Double {
      if (x == 0.0 || !x.isFinite()) return x
      val bd =
        when (ctx.displayMode) {
          DisplayMode.FIX -> BigDecimal.valueOf(x).setScale(ctx.fixDigits.coerceIn(0, 9), RoundingMode.HALF_UP)
          DisplayMode.SCI -> BigDecimal.valueOf(x).round(MathContext(ctx.sciDigits.coerceIn(1, 10), RoundingMode.HALF_UP))
          DisplayMode.NORM1,
          DisplayMode.NORM2 -> BigDecimal.valueOf(x).round(MathContext(10, RoundingMode.HALF_UP))
        }
      return bd.toDouble()
    }
  }
}
