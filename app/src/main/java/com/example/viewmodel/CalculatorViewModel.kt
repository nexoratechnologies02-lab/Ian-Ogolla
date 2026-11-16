package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CalculatorRepository
import com.example.engine.AngleUnit
import com.example.engine.CalcErrorType
import com.example.engine.CalcValue
import com.example.engine.CalcVariable
import com.example.engine.CalculatorMode
import com.example.engine.DisplayMode
import com.example.engine.EvalContext
import com.example.engine.EvalOutcome
import com.example.engine.ExpressionParser
import com.example.engine.Fraction
import com.example.engine.FractionPreference
import com.example.engine.HistoryEntry
import com.example.engine.NumberFormatter
import com.example.engine.RegEntry
import com.example.engine.RegressionType
import com.example.engine.StatEntry
import com.example.engine.StatisticsEngine
import com.example.engine.ThemeMode
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CalcKey {
  SHIFT,
  ALPHA,
  UP,
  DOWN,
  LEFT,
  RIGHT,
  MODE,
  ON_AC,
  DEL,

  // Scientific Row 1
  X_INV,
  NCR,
  POL,
  X_CUBE,
  ABS,
  PI_KEY,

  // Scientific Row 2
  FRACTION,
  SQRT,
  X_SQ,
  POW,
  LOG,
  LN,

  // Scientific Row 3
  NEG,
  DMS,
  HYP,
  SIN,
  COS,
  TAN,

  // Scientific Row 4
  RCL,
  ENG,
  LPAREN,
  RPAREN,
  COMMA,
  M_PLUS,

  // Numpad & Arithmetic
  D7,
  D8,
  D9,
  D4,
  D5,
  D6,
  MUL,
  DIV,
  D1,
  D2,
  D3,
  PLUS,
  MINUS,
  D0,
  DOT,
  EXP,
  ANS,
  EQUALS,
}

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = CalculatorRepository(application.applicationContext)
  private val parser = ExpressionParser()

  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

  private val atomicTokens =
    listOf(
      "sinh⁻¹(",
      "cosh⁻¹(",
      "tanh⁻¹(",
      "sin⁻¹(",
      "cos⁻¹(",
      "tan⁻¹(",
      "sinh(",
      "cosh(",
      "tanh(",
      "sin(",
      "cos(",
      "tan(",
      "log(",
      "ln(",
      "Abs(",
      "Pol(",
      "Rec(",
      "Rnd(",
      "10^(",
      "e^(",
      "√(",
      "∛(",
      "Ran#",
      "Ans",
      "xσn-1",
      "xσn",
      "yσn-1",
      "yσn",
      "Σx²",
      "Σy²",
      "Σxy",
      "Σx",
      "Σy",
      "x̂1",
      "x̂2",
      "x̂(",
      "ŷ(",
      "x̄",
      "ȳ",
      "RegA",
      "RegB",
      "RegC",
      "ˣ√",
      "⁻¹",
      "nPr",
      "nCr",
    )

  init {
    viewModelScope.launch {
      val persisted = repository.stateFlow.first()
      _uiState.update { state ->
        state.copy(
          themeMode = persisted.themeMode,
          soundEnabled = persisted.soundEnabled,
          hapticEnabled = persisted.hapticEnabled,
          angleUnit = persisted.angleUnit,
          mode = persisted.mode,
          regressionType = persisted.regressionType,
          displayMode = persisted.displayMode,
          fixDigits = persisted.fixDigits,
          sciDigits = persisted.sciDigits,
          fractionPreference = persisted.fractionPreference,
          variables = persisted.variables,
          ans = persisted.ans,
          history = persisted.history,
          sdEntries = persisted.sdEntries,
          regEntries = persisted.regEntries,
        )
      }
    }
  }

  fun onKeyPress(key: CalcKey) {
    val state = _uiState.value

    // Clear error state on AC or any editing key (except directional keys if desired)
    if (state.errorType != null && key != CalcKey.ON_AC && key != CalcKey.LEFT && key != CalcKey.RIGHT) {
      if (key == CalcKey.DEL) {
        _uiState.update { it.copy(errorType = null, resultDisplay = "0") }
        return
      }
    }

    // Handle active LCD overlay menu selection via physical number keys 0..9
    if (state.activeMenu != ActiveCalcMenu.None && key != CalcKey.ON_AC && key != CalcKey.MODE) {
      if (handleMenuDigitKey(key, state)) return
    }

    // Handle STO / RCL waiting for variable key
    if ((state.isSto || state.isRcl) && key != CalcKey.SHIFT && key != CalcKey.ALPHA && key != CalcKey.ON_AC) {
      val targetVar = keyToVariable(key)
      if (targetVar != null) {
        if (state.isSto) {
          performStoreVariable(targetVar)
        } else {
          performRecallVariable(targetVar)
        }
        return
      }
    }

    when (key) {
      CalcKey.SHIFT -> {
        _uiState.update {
          it.copy(
            isShift = !it.isShift,
            isAlpha = false,
            isSto = false,
            isRcl = false,
          )
        }
      }

      CalcKey.ALPHA -> {
        _uiState.update {
          it.copy(
            isAlpha = !it.isAlpha,
            isShift = false,
            isSto = false,
            isRcl = false,
          )
        }
      }

      CalcKey.HYP -> {
        if (state.isAlpha) {
          insertToken("C", clearAlpha = true)
        } else {
          _uiState.update { it.copy(isHyp = !it.isHyp, isShift = false) }
        }
      }

      CalcKey.ON_AC -> handleAc(state)
      CalcKey.DEL -> handleDel(state)
      CalcKey.LEFT -> moveCursorLeft()
      CalcKey.RIGHT -> moveCursorRight()
      CalcKey.UP -> replayOlder()
      CalcKey.DOWN -> replayNewer()

      CalcKey.MODE -> {
        if (state.isShift) {
          _uiState.update {
            it.copy(isShift = false, activeMenu = ActiveCalcMenu.ClearSelect)
          }
        } else {
          val nextMenu =
            when (state.activeMenu) {
              ActiveCalcMenu.ModeSelect -> ActiveCalcMenu.SetupSelect
              ActiveCalcMenu.SetupSelect -> ActiveCalcMenu.None
              else -> ActiveCalcMenu.ModeSelect
            }
          _uiState.update { it.copy(activeMenu = nextMenu, isShift = false, isAlpha = false) }
        }
      }

      CalcKey.RCL -> {
        if (state.isShift) {
          _uiState.update { it.copy(isSto = true, isRcl = false, isShift = false, isAlpha = false) }
        } else {
          _uiState.update { it.copy(isRcl = !it.isRcl, isSto = false, isShift = false, isAlpha = false) }
        }
      }

      CalcKey.EQUALS -> {
        if (state.isShift) {
          insertToken("%", clearShift = true)
        } else {
          evaluateExpression()
        }
      }

      CalcKey.FRACTION -> handleFractionKey(state)
      CalcKey.DMS -> handleDmsKey(state)
      CalcKey.ENG -> handleEngKey(state)
      CalcKey.M_PLUS -> handleMPlusKey(state)
      CalcKey.ANS -> handleAnsKey(state)

      // Variable / Alpha mappings & primary scientific keys
      CalcKey.X_INV -> insertToken(if (state.isShift) "!" else "⁻¹", clearShift = true)
      CalcKey.NCR -> insertToken(if (state.isShift) "nPr" else "nCr", clearShift = true)
      CalcKey.POL -> insertToken(if (state.isShift) "Rec(" else "Pol(", clearShift = true)
      CalcKey.X_CUBE -> insertToken(if (state.isShift) "∛(" else "³", clearShift = true)
      CalcKey.ABS -> insertToken(if (state.isShift) "Ran#" else "Abs(", clearShift = true)
      CalcKey.PI_KEY -> insertToken(if (state.isShift) "e" else "π", clearShift = true)

      CalcKey.SQRT -> insertToken(if (state.isShift) "∛(" else "√(", clearShift = true)
      CalcKey.X_SQ -> insertToken(if (state.isShift) "√(" else "²", clearShift = true)
      CalcKey.POW -> insertToken(if (state.isShift) "ˣ√" else "^", clearShift = true)
      CalcKey.LOG -> insertToken(if (state.isShift) "10^(" else "log(", clearShift = true)
      CalcKey.LN -> insertToken(if (state.isShift) "e^(" else "ln(", clearShift = true)

      CalcKey.NEG -> {
        if (state.isAlpha) insertToken("A", clearAlpha = true)
        else insertToken("−")
      }

      CalcKey.SIN -> {
        when {
          state.isAlpha -> insertToken("D", clearAlpha = true)
          state.isHyp && state.isShift -> insertToken("sinh⁻¹(", clearShift = true, clearHyp = true)
          state.isHyp -> insertToken("sinh(", clearHyp = true)
          state.isShift -> insertToken("sin⁻¹(", clearShift = true)
          else -> insertToken("sin(")
        }
      }

      CalcKey.COS -> {
        when {
          state.isAlpha -> insertToken("E", clearAlpha = true)
          state.isHyp && state.isShift -> insertToken("cosh⁻¹(", clearShift = true, clearHyp = true)
          state.isHyp -> insertToken("cosh(", clearHyp = true)
          state.isShift -> insertToken("cos⁻¹(", clearShift = true)
          else -> insertToken("cos(")
        }
      }

      CalcKey.TAN -> {
        when {
          state.isAlpha -> insertToken("F", clearAlpha = true)
          state.isHyp && state.isShift -> insertToken("tanh⁻¹(", clearShift = true, clearHyp = true)
          state.isHyp -> insertToken("tanh(", clearHyp = true)
          state.isShift -> insertToken("tan⁻¹(", clearShift = true)
          else -> insertToken("tan(")
        }
      }

      CalcKey.LPAREN -> insertToken(if (state.isShift) "Abs(" else "(", clearShift = true)
      CalcKey.RPAREN -> {
        if (state.isAlpha) insertToken("X", clearAlpha = true)
        else insertToken(")")
      }
      CalcKey.COMMA -> {
        when {
          state.isAlpha -> insertToken("Y", clearAlpha = true)
          state.isShift -> insertToken(";", clearShift = true)
          else -> insertToken(",")
        }
      }

      // Digits & Numpad
      CalcKey.D0 -> insertToken(if (state.isShift) "Rnd(" else "0", clearShift = true)
      CalcKey.D1 -> {
        if (state.isShift) {
          _uiState.update { it.copy(isShift = false, activeMenu = ActiveCalcMenu.StatSumSelect) }
        } else {
          insertToken("1")
        }
      }
      CalcKey.D2 -> {
        if (state.isShift) {
          _uiState.update { it.copy(isShift = false, activeMenu = ActiveCalcMenu.StatVarSelect) }
        } else {
          insertToken("2")
        }
      }
      CalcKey.D3 -> {
        if (state.isShift && state.mode != CalculatorMode.COMP) {
          _uiState.update { it.copy(isShift = false, showStatDataSheet = true) }
        } else {
          insertToken("3", clearShift = true)
        }
      }
      CalcKey.D4 -> insertToken("4", clearShift = true)
      CalcKey.D5 -> insertToken("5", clearShift = true)
      CalcKey.D6 -> insertToken("6", clearShift = true)
      CalcKey.D7 -> insertToken("7", clearShift = true)
      CalcKey.D8 -> insertToken("8", clearShift = true)
      CalcKey.D9 -> insertToken("9", clearShift = true)
      CalcKey.DOT -> insertToken(if (state.isShift) "Ran#" else ".", clearShift = true)
      CalcKey.EXP -> {
        when {
          state.isAlpha -> insertToken("e", clearAlpha = true)
          state.isShift -> insertToken("π", clearShift = true)
          else -> insertToken("ᴇ")
        }
      }
      CalcKey.PLUS -> insertToken("+")
      CalcKey.MINUS -> insertToken("−")
      CalcKey.MUL -> insertToken("×")
      CalcKey.DIV -> insertToken("÷")
    }
  }

  private fun handleMenuDigitKey(key: CalcKey, state: CalculatorUiState): Boolean {
    val digit =
      when (key) {
        CalcKey.D0 -> 0
        CalcKey.D1 -> 1
        CalcKey.D2 -> 2
        CalcKey.D3 -> 3
        CalcKey.D4 -> 4
        CalcKey.D5 -> 5
        CalcKey.D6 -> 6
        CalcKey.D7 -> 7
        CalcKey.D8 -> 8
        CalcKey.D9 -> 9
        else -> return false
      }
    return selectMenuOption(state.activeMenu, digit)
  }

  fun selectMenuOption(menu: ActiveCalcMenu, option: Int): Boolean {
    when (menu) {
      ActiveCalcMenu.ModeSelect -> {
        when (option) {
          1 -> setMode(CalculatorMode.COMP)
          2 -> setMode(CalculatorMode.SD)
          3 -> _uiState.update { it.copy(activeMenu = ActiveCalcMenu.RegTypeSelect) }
          else -> return false
        }
      }
      ActiveCalcMenu.RegTypeSelect -> {
        val regType =
          when (option) {
            1 -> RegressionType.LIN
            2 -> RegressionType.LOG
            3 -> RegressionType.EXP
            4 -> RegressionType.PWR
            5 -> RegressionType.INV
            6 -> RegressionType.QUAD
            else -> return false
          }
        setRegMode(regType)
      }
      ActiveCalcMenu.SetupSelect -> {
        when (option) {
          1 -> {
            setAngleUnit(AngleUnit.DEG)
            dismissMenu()
          }
          2 -> {
            setAngleUnit(AngleUnit.RAD)
            dismissMenu()
          }
          3 -> {
            setAngleUnit(AngleUnit.GRA)
            dismissMenu()
          }
          4 -> _uiState.update { it.copy(activeMenu = ActiveCalcMenu.FixDigitsSelect) }
          5 -> _uiState.update { it.copy(activeMenu = ActiveCalcMenu.SciDigitsSelect) }
          6 -> _uiState.update { it.copy(activeMenu = ActiveCalcMenu.NormSelect) }
          7 -> {
            toggleFractionPreference()
            dismissMenu()
          }
          else -> return false
        }
      }
      ActiveCalcMenu.FixDigitsSelect -> {
        if (option in 0..9) {
          setDisplayMode(DisplayMode.FIX, fixDigits = option)
          dismissMenu()
        } else return false
      }
      ActiveCalcMenu.SciDigitsSelect -> {
        val digits = if (option == 0) 10 else option
        if (digits in 1..10) {
          setDisplayMode(DisplayMode.SCI, sciDigits = digits)
          dismissMenu()
        } else return false
      }
      ActiveCalcMenu.NormSelect -> {
        when (option) {
          1 -> {
            setDisplayMode(DisplayMode.NORM1)
            dismissMenu()
          }
          2 -> {
            setDisplayMode(DisplayMode.NORM2)
            dismissMenu()
          }
          else -> return false
        }
      }
      ActiveCalcMenu.ClearSelect -> {
        when (option) {
          1 -> {
            clearMemoryVariables()
            dismissMenu()
          }
          2 -> {
            resetModeAndDisplay()
            dismissMenu()
          }
          3 -> {
            factoryResetAll()
            dismissMenu()
          }
          else -> return false
        }
      }
      ActiveCalcMenu.DrgSelect -> {
        when (option) {
          1 -> {
            insertToken("°")
            dismissMenu()
          }
          2 -> {
            insertToken("ʳ")
            dismissMenu()
          }
          3 -> {
            insertToken("ᵍ")
            dismissMenu()
          }
          else -> return false
        }
      }
      ActiveCalcMenu.StatSumSelect -> {
        val token =
          when (option) {
            1 -> "Σx²"
            2 -> "Σx"
            3 -> "n"
            4 -> if (_uiState.value.mode == CalculatorMode.REG) "Σy²" else return false
            5 -> if (_uiState.value.mode == CalculatorMode.REG) "Σy" else return false
            6 -> if (_uiState.value.mode == CalculatorMode.REG) "Σxy" else return false
            else -> return false
          }
        insertToken(token)
        dismissMenu()
      }
      ActiveCalcMenu.StatVarSelect -> {
        val isReg = _uiState.value.mode == CalculatorMode.REG
        val token =
          when (option) {
            1 -> "x̄"
            2 -> "xσn"
            3 -> "xσn-1"
            4 -> if (isReg) "ȳ" else return false
            5 -> if (isReg) "yσn" else return false
            6 -> if (isReg) "yσn-1" else return false
            7 -> if (isReg) "r" else return false
            8 -> if (isReg) "ŷ(" else return false
            9 -> if (isReg) "x̂(" else return false
            else -> return false
          }
        insertToken(token)
        dismissMenu()
      }
      ActiveCalcMenu.None -> return false
    }
    return true
  }

  fun insertStatTokenAndDismiss(token: String) {
    insertToken(token)
    dismissMenu()
  }

  fun dismissMenu() {
    _uiState.update { it.copy(activeMenu = ActiveCalcMenu.None) }
  }

  private fun keyToVariable(key: CalcKey): CalcVariable? =
    when (key) {
      CalcKey.NEG -> CalcVariable.A
      CalcKey.DMS -> CalcVariable.B
      CalcKey.HYP -> CalcVariable.C
      CalcKey.SIN -> CalcVariable.D
      CalcKey.COS -> CalcVariable.E
      CalcKey.TAN -> CalcVariable.F
      CalcKey.RPAREN -> CalcVariable.X
      CalcKey.COMMA -> CalcVariable.Y
      CalcKey.M_PLUS -> CalcVariable.M
      else -> null
    }

  private fun performStoreVariable(variable: CalcVariable) {
    val state = _uiState.value
    val valueToStore: Double =
      if (state.expression.isNotBlank() && !state.justEvaluated) {
        when (val outcome = evaluateRaw(state.expression, state)) {
          is EvalOutcome.Success -> outcome.value.number
          is EvalOutcome.Failure -> {
            _uiState.update {
              it.copy(
                isSto = false,
                errorType = outcome.errorType,
                resultDisplay = outcome.errorType.title,
              )
            }
            return
          }
        }
      } else {
        state.lastEvalValue?.number ?: state.ans
      }

    val updatedVars = state.variables.toMutableMap().apply { put(variable, valueToStore) }
    val formatted = formatNumber(valueToStore, state)
    _uiState.update {
      it.copy(
        variables = updatedVars,
        ans = valueToStore,
        isSto = false,
        isShift = false,
        isAlpha = false,
        justEvaluated = true,
        lastEvalValue = CalcValue(valueToStore, Fraction.fromDouble(valueToStore)),
        primaryLabel = "${variable.symbol}←",
        resultDisplay = formatted,
        secondaryResultDisplay = null,
        errorType = null,
      )
    }
    persistVars(updatedVars, valueToStore)
  }

  private fun performRecallVariable(variable: CalcVariable) {
    val state = _uiState.value
    val value = state.variables[variable] ?: 0.0
    val formatted = formatNumber(value, state)
    _uiState.update {
      it.copy(
        isRcl = false,
        isShift = false,
        isAlpha = false,
        justEvaluated = true,
        lastEvalValue = CalcValue(value, Fraction.fromDouble(value)),
        primaryLabel = "${variable.symbol} =",
        resultDisplay = formatted,
        secondaryResultDisplay = null,
        errorType = null,
      )
    }
  }

  private fun handleAc(state: CalculatorUiState) {
    if (state.activeMenu != ActiveCalcMenu.None) {
      _uiState.update { it.copy(activeMenu = ActiveCalcMenu.None, isShift = false, isAlpha = false) }
      return
    }
    _uiState.update {
      it.copy(
        expression = "",
        cursorIndex = 0,
        resultDisplay = "0",
        primaryLabel = null,
        secondaryResultDisplay = null,
        errorType = null,
        justEvaluated = false,
        lastEvalValue = null,
        isShift = false,
        isAlpha = false,
        isHyp = false,
        isSto = false,
        isRcl = false,
        showAsFraction = false,
        showAsDms = false,
        engStepShift = null,
        historyReplayIndex = -1,
      )
    }
  }

  private fun handleDel(state: CalculatorUiState) {
    if (state.isShift) {
      _uiState.update { it.copy(isInsertMode = !it.isInsertMode, isShift = false) }
      return
    }
    if (state.expression.isEmpty() || state.cursorIndex <= 0) return

    val expr = state.expression
    val cur = state.cursorIndex.coerceIn(0, expr.length)

    // Check if cursor sits immediately after an atomic multi-char token
    val matchedToken = atomicTokens.firstOrNull { tok -> cur >= tok.length && expr.substring(cur - tok.length, cur) == tok }
    val deleteLen = matchedToken?.length ?: 1
    val newExpr = expr.removeRange(cur - deleteLen, cur)
    val newCursor = cur - deleteLen

    _uiState.update {
      it.copy(
        expression = newExpr,
        cursorIndex = newCursor,
        justEvaluated = false,
        historyReplayIndex = -1,
        errorType = null,
      )
    }
  }

  private fun moveCursorLeft() {
    val state = _uiState.value
    if (state.expression.isEmpty() || state.cursorIndex <= 0) return
    val cur = state.cursorIndex.coerceIn(0, state.expression.length)
    val matched = atomicTokens.firstOrNull { tok -> cur >= tok.length && state.expression.substring(cur - tok.length, cur) == tok }
    val step = matched?.length ?: 1
    _uiState.update { it.copy(cursorIndex = (cur - step).coerceAtLeast(0), justEvaluated = false) }
  }

  private fun moveCursorRight() {
    val state = _uiState.value
    if (state.expression.isEmpty() || state.cursorIndex >= state.expression.length) return
    val cur = state.cursorIndex.coerceIn(0, state.expression.length)
    val matched = atomicTokens.firstOrNull { tok -> state.expression.startsWith(tok, cur) }
    val step = matched?.length ?: 1
    _uiState.update { it.copy(cursorIndex = (cur + step).coerceAtMost(state.expression.length), justEvaluated = false) }
  }

  private fun replayOlder() {
    val state = _uiState.value
    if (state.history.isEmpty()) return
    val nextIdx = (state.historyReplayIndex + 1).coerceAtMost(state.history.lastIndex)
    val entry = state.history[nextIdx]
    _uiState.update {
      it.copy(
        expression = entry.expression,
        cursorIndex = entry.expression.length,
        resultDisplay = entry.formattedResult,
        primaryLabel = null,
        secondaryResultDisplay = null,
        errorType = null,
        justEvaluated = true,
        lastEvalValue = CalcValue(entry.numericResult, Fraction.fromDouble(entry.numericResult)),
        historyReplayIndex = nextIdx,
        showAsFraction = entry.formattedResult.contains("⌟"),
        showAsDms = entry.formattedResult.contains("′"),
        engStepShift = null,
      )
    }
  }

  private fun replayNewer() {
    val state = _uiState.value
    if (state.historyReplayIndex <= 0) {
      if (state.historyReplayIndex == 0) {
        _uiState.update {
          it.copy(
            expression = "",
            cursorIndex = 0,
            resultDisplay = "0",
            primaryLabel = null,
            secondaryResultDisplay = null,
            justEvaluated = false,
            historyReplayIndex = -1,
          )
        }
      }
      return
    }
    val nextIdx = state.historyReplayIndex - 1
    val entry = state.history[nextIdx]
    _uiState.update {
      it.copy(
        expression = entry.expression,
        cursorIndex = entry.expression.length,
        resultDisplay = entry.formattedResult,
        primaryLabel = null,
        secondaryResultDisplay = null,
        errorType = null,
        justEvaluated = true,
        lastEvalValue = CalcValue(entry.numericResult, Fraction.fromDouble(entry.numericResult)),
        historyReplayIndex = nextIdx,
      )
    }
  }

  private fun handleFractionKey(state: CalculatorUiState) {
    if (state.isShift) {
      // Toggle Mixed <-> Improper fraction preference & reformat if currently showing result
      val nextPref =
        if (state.fractionPreference == FractionPreference.MIXED) FractionPreference.IMPROPER
        else FractionPreference.MIXED
      _uiState.update { s ->
        val valObj = s.lastEvalValue
        val frac = valObj?.fraction ?: valObj?.let { Fraction.fromDouble(it.number) }
        val newResult =
          if (s.justEvaluated && frac != null && frac.fitsDisplayLimit()) {
            frac.format(nextPref)
          } else {
            s.resultDisplay
          }
        s.copy(
          fractionPreference = nextPref,
          showAsFraction = s.justEvaluated && frac != null && frac.fitsDisplayLimit(),
          resultDisplay = newResult,
          isShift = false,
        )
      }
      persistSettings()
      return
    }

    if (state.justEvaluated && state.lastEvalValue != null) {
      val valObj = state.lastEvalValue
      val frac = valObj.fraction ?: Fraction.fromDouble(valObj.number)
      if (frac != null && frac.fitsDisplayLimit()) {
        val nextShow = !state.showAsFraction
        val formatted =
          NumberFormatter.format(
            value = valObj.number,
            displayMode = state.displayMode,
            fixDigits = state.fixDigits,
            sciDigits = state.sciDigits,
            fraction = frac,
            showFraction = nextShow,
            fractionPreference = state.fractionPreference,
          )
        _uiState.update {
          it.copy(
            showAsFraction = nextShow,
            showAsDms = false,
            engStepShift = null,
            resultDisplay = formatted,
          )
        }
      }
      return
    }

    insertToken("⌟")
  }

  private fun handleDmsKey(state: CalculatorUiState) {
    if (state.isAlpha) {
      insertToken("B", clearAlpha = true)
      return
    }
    if (state.justEvaluated && state.lastEvalValue != null) {
      val nextDms = !state.showAsDms
      val formatted =
        NumberFormatter.format(
          value = state.lastEvalValue.number,
          displayMode = state.displayMode,
          fixDigits = state.fixDigits,
          sciDigits = state.sciDigits,
          asDms = nextDms,
        )
      _uiState.update {
        it.copy(
          showAsDms = nextDms,
          showAsFraction = false,
          engStepShift = null,
          isShift = false,
          resultDisplay = formatted,
        )
      }
      return
    }
    insertToken("°", clearShift = true)
  }

  private fun handleEngKey(state: CalculatorUiState) {
    val valObj = state.lastEvalValue ?: return
    val currentShift = state.engStepShift
    val nextShift =
      if (state.isShift) {
        (currentShift ?: 0) + 1
      } else {
        if (currentShift == null) 0 else currentShift - 1
      }
    val formatted = NumberFormatter.formatEngineering(valObj.number, nextShift)
    _uiState.update {
      it.copy(
        engStepShift = nextShift,
        showAsFraction = false,
        showAsDms = false,
        isShift = false,
        resultDisplay = formatted,
      )
    }
  }

  private fun handleAnsKey(state: CalculatorUiState) {
    if (state.isShift) {
      _uiState.update { it.copy(isShift = false, activeMenu = ActiveCalcMenu.DrgSelect) }
    } else {
      insertToken("Ans")
    }
  }

  private fun handleMPlusKey(state: CalculatorUiState) {
    if (state.isAlpha) {
      insertToken("M", clearAlpha = true)
      return
    }

    // In SD or REG mode, M+ acts as DT (Data entry) and SHIFT+M+ acts as CL (Clear last/data)
    if (state.mode == CalculatorMode.SD && !state.isShift) {
      addSdDataFromInput(state)
      return
    }
    if (state.mode == CalculatorMode.REG && !state.isShift) {
      addRegDataFromInput(state)
      return
    }

    val delta: Double =
      if (state.expression.isNotBlank() && !state.justEvaluated) {
        when (val outcome = evaluateRaw(state.expression, state)) {
          is EvalOutcome.Success -> outcome.value.number
          is EvalOutcome.Failure -> {
            _uiState.update {
              it.copy(errorType = outcome.errorType, resultDisplay = outcome.errorType.title, isShift = false)
            }
            return
          }
        }
      } else {
        state.lastEvalValue?.number ?: state.ans
      }

    val signedDelta = if (state.isShift) -delta else delta
    val currentM = state.variables[CalcVariable.M] ?: 0.0
    val newM = NumberFormatter.sanitize(currentM + signedDelta)
    val updatedVars = state.variables.toMutableMap().apply { put(CalcVariable.M, newM) }
    val formattedDelta = formatNumber(delta, state)
    val label = if (state.isShift) "M−" else "M+"

    _uiState.update {
      it.copy(
        variables = updatedVars,
        ans = delta,
        isShift = false,
        justEvaluated = true,
        lastEvalValue = CalcValue(delta, Fraction.fromDouble(delta)),
        primaryLabel = label,
        resultDisplay = formattedDelta,
        secondaryResultDisplay = "M = ${NumberFormatter.formatCompact(newM)}",
        errorType = null,
      )
    }
    persistVars(updatedVars, delta)
  }

  private fun addSdDataFromInput(state: CalculatorUiState) {
    val raw = state.expression.trim()
    if (raw.isEmpty()) return
    val parts = raw.split(';')
    if (parts.size > 2) {
      _uiState.update { it.copy(errorType = CalcErrorType.SyntaxError, resultDisplay = "Syntax ERROR") }
      return
    }
    val xVal =
      when (val res = evaluateRaw(parts[0], state)) {
        is EvalOutcome.Success -> res.value.number
        is EvalOutcome.Failure -> {
          _uiState.update { it.copy(errorType = res.errorType, resultDisplay = res.errorType.title) }
          return
        }
      }
    val freq =
      if (parts.size == 2) {
        when (val res = evaluateRaw(parts[1], state)) {
          is EvalOutcome.Success -> res.value.number.toInt().coerceAtLeast(1)
          is EvalOutcome.Failure -> {
            _uiState.update { it.copy(errorType = res.errorType, resultDisplay = res.errorType.title) }
            return
          }
        }
      } else 1

    val newList = state.sdEntries + StatEntry(UUID.randomUUID().toString(), xVal, freq)
    val summary = StatisticsEngine.computeSd(newList)
    _uiState.update {
      it.copy(
        sdEntries = newList,
        expression = "",
        cursorIndex = 0,
        primaryLabel = "n =",
        resultDisplay = summary.n.toString(),
        secondaryResultDisplay = "x̄ = ${NumberFormatter.formatCompact(summary.mean)}",
        justEvaluated = true,
        errorType = null,
      )
    }
    viewModelScope.launch { repository.saveSdEntries(newList) }
  }

  private fun addRegDataFromInput(state: CalculatorUiState) {
    val raw = state.expression.trim()
    if (raw.isEmpty()) return
    val freqSplit = raw.split(';')
    val pairParts = freqSplit[0].split(',')
    if (pairParts.size != 2 || freqSplit.size > 2) {
      _uiState.update {
        it.copy(
          errorType = CalcErrorType.SyntaxError,
          resultDisplay = "Enter x,y then DT",
        )
      }
      return
    }
    val xVal =
      when (val res = evaluateRaw(pairParts[0], state)) {
        is EvalOutcome.Success -> res.value.number
        is EvalOutcome.Failure -> {
          _uiState.update { it.copy(errorType = res.errorType, resultDisplay = res.errorType.title) }
          return
        }
      }
    val yVal =
      when (val res = evaluateRaw(pairParts[1], state)) {
        is EvalOutcome.Success -> res.value.number
        is EvalOutcome.Failure -> {
          _uiState.update { it.copy(errorType = res.errorType, resultDisplay = res.errorType.title) }
          return
        }
      }
    val freq =
      if (freqSplit.size == 2) {
        when (val res = evaluateRaw(freqSplit[1], state)) {
          is EvalOutcome.Success -> res.value.number.toInt().coerceAtLeast(1)
          is EvalOutcome.Failure -> {
            _uiState.update { it.copy(errorType = res.errorType, resultDisplay = res.errorType.title) }
            return
          }
        }
      } else 1

    val newList = state.regEntries + RegEntry(UUID.randomUUID().toString(), xVal, yVal, freq)
    val summary = StatisticsEngine.computeReg(newList, state.regressionType)
    _uiState.update {
      it.copy(
        regEntries = newList,
        expression = "",
        cursorIndex = 0,
        primaryLabel = "n =",
        resultDisplay = summary.n.toString(),
        secondaryResultDisplay =
          if (summary.a.isFinite() && summary.b.isFinite())
            "A=${NumberFormatter.formatCompact(summary.a)}, B=${NumberFormatter.formatCompact(summary.b)}"
          else "${state.regressionType.code} (${summary.n} pts)",
        justEvaluated = true,
        errorType = null,
      )
    }
    viewModelScope.launch { repository.saveRegEntries(newList) }
  }

  private fun insertToken(
    token: String,
    clearShift: Boolean = false,
    clearAlpha: Boolean = false,
    clearHyp: Boolean = false,
  ) {
    val state = _uiState.value
    val isContinuationOp =
      token in listOf("+", "−", "×", "÷", "^", "ˣ√", "nPr", "nCr", "²", "³", "⁻¹", "!", "%")

    var baseExpr = state.expression
    var cur = state.cursorIndex.coerceIn(0, baseExpr.length)

    if (state.justEvaluated) {
      if (isContinuationOp) {
        baseExpr = "Ans"
        cur = baseExpr.length
      } else {
        baseExpr = ""
        cur = 0
      }
    }

    val newExpr =
      if (state.isInsertMode || cur >= baseExpr.length) {
        baseExpr.substring(0, cur) + token + baseExpr.substring(cur)
      } else {
        // Overwrite single char or atomic token at cursor
        val existingTok = atomicTokens.firstOrNull { baseExpr.startsWith(it, cur) }
        val replaceLen = existingTok?.length ?: 1
        baseExpr.substring(0, cur) + token + baseExpr.substring((cur + replaceLen).coerceAtMost(baseExpr.length))
      }

    val newCursor = cur + token.length

    _uiState.update {
      it.copy(
        expression = newExpr,
        cursorIndex = newCursor,
        justEvaluated = false,
        errorType = null,
        historyReplayIndex = -1,
        primaryLabel = null,
        secondaryResultDisplay = null,
        showAsFraction = false,
        showAsDms = false,
        engStepShift = null,
        isShift = if (clearShift) false else it.isShift,
        isAlpha = if (clearAlpha) false else it.isAlpha,
        isHyp = if (clearHyp) false else it.isHyp,
      )
    }
  }

  fun evaluateExpression() {
    val state = _uiState.value
    val expr = state.expression.trim()
    if (expr.isEmpty()) return

    // In SD mode, if user enters "x;freq" and hits =, or in COMP/SD/REG evaluates expression
    when (val outcome = evaluateRaw(expr, state)) {
      is EvalOutcome.Failure -> {
        _uiState.update {
          it.copy(
            errorType = outcome.errorType,
            resultDisplay = outcome.errorType.title,
            primaryLabel = null,
            secondaryResultDisplay = outcome.detail.takeIf { d -> d != outcome.errorType.title },
            isShift = false,
            isAlpha = false,
            isHyp = false,
          )
        }
      }
      is EvalOutcome.Success -> {
        val calcVal = outcome.value
        val showFrac = expr.contains("⌟") && calcVal.fraction != null && calcVal.fraction.fitsDisplayLimit()
        val formatted =
          NumberFormatter.format(
            value = calcVal.number,
            displayMode = state.displayMode,
            fixDigits = state.fixDigits,
            sciDigits = state.sciDigits,
            fraction = calcVal.fraction,
            showFraction = showFrac,
            fractionPreference = state.fractionPreference,
          )

        val secondaryText =
          calcVal.secondaryPair?.let { (label, v) ->
            "$label = ${formatNumber(v, state)}"
          }

        val updatedVars =
          if (calcVal.updatedVariables.isNotEmpty()) {
            state.variables + calcVal.updatedVariables
          } else {
            state.variables
          }

        val entry =
          HistoryEntry(
            id = UUID.randomUUID().toString(),
            expression = expr,
            formattedResult = formatted,
            numericResult = calcVal.number,
            angleUnit = state.angleUnit,
            mode = state.mode,
            timestampMs = System.currentTimeMillis(),
          )
        val newHistory = (listOf(entry) + state.history).take(100)

        _uiState.update {
          it.copy(
            resultDisplay = formatted,
            primaryLabel = calcVal.primaryLabel,
            secondaryResultDisplay = secondaryText,
            errorType = null,
            justEvaluated = true,
            lastEvalValue = calcVal,
            showAsFraction = showFrac,
            showAsDms = false,
            engStepShift = null,
            ans = calcVal.number,
            variables = updatedVars,
            history = newHistory,
            historyReplayIndex = -1,
            isShift = false,
            isAlpha = false,
            isHyp = false,
          )
        }
        viewModelScope.launch {
          repository.saveVariablesAndAns(updatedVars, calcVal.number)
          repository.saveHistory(newHistory)
        }
      }
    }
  }

  private fun evaluateRaw(expression: String, state: CalculatorUiState): EvalOutcome {
    val ctx =
      EvalContext(
        angleUnit = state.angleUnit,
        displayMode = state.displayMode,
        fixDigits = state.fixDigits,
        sciDigits = state.sciDigits,
        variables = state.variables,
        ans = state.ans,
        sdSummary = if (state.mode == CalculatorMode.SD) state.sdSummary else null,
        regSummary = if (state.mode == CalculatorMode.REG) state.regSummary else null,
      )
    return parser.evaluate(expression, ctx)
  }

  private fun formatNumber(value: Double, state: CalculatorUiState): String =
    NumberFormatter.format(
      value = value,
      displayMode = state.displayMode,
      fixDigits = state.fixDigits,
      sciDigits = state.sciDigits,
    )

  // Public actions for Settings, History, Guide, and Stat Data Sheets
  fun loadHistoryEntry(entry: HistoryEntry) {
    _uiState.update {
      it.copy(
        expression = entry.expression,
        cursorIndex = entry.expression.length,
        resultDisplay = entry.formattedResult,
        primaryLabel = null,
        secondaryResultDisplay = null,
        errorType = null,
        justEvaluated = false,
        lastEvalValue = CalcValue(entry.numericResult, Fraction.fromDouble(entry.numericResult)),
        historyReplayIndex = -1,
      )
    }
  }

  fun loadExampleExpression(expression: String, autoEval: Boolean = false) {
    _uiState.update {
      it.copy(
        expression = expression,
        cursorIndex = expression.length,
        resultDisplay = "0",
        primaryLabel = null,
        secondaryResultDisplay = null,
        errorType = null,
        justEvaluated = false,
      )
    }
    if (autoEval) evaluateExpression()
  }

  fun deleteHistoryEntry(id: String) {
    val updated = _uiState.value.history.filterNot { it.id == id }
    _uiState.update { it.copy(history = updated, historyReplayIndex = -1) }
    viewModelScope.launch { repository.saveHistory(updated) }
  }

  fun clearHistory() {
    _uiState.update { it.copy(history = emptyList(), historyReplayIndex = -1) }
    viewModelScope.launch { repository.saveHistory(emptyList()) }
  }

  fun addSdEntry(x: Double, freq: Int = 1) {
    val updated = _uiState.value.sdEntries + StatEntry(UUID.randomUUID().toString(), x, freq.coerceAtLeast(1))
    _uiState.update { it.copy(sdEntries = updated) }
    viewModelScope.launch { repository.saveSdEntries(updated) }
  }

  fun updateSdEntry(id: String, x: Double, freq: Int) {
    val updated = _uiState.value.sdEntries.map { if (it.id == id) it.copy(x = x, freq = freq.coerceAtLeast(1)) else it }
    _uiState.update { it.copy(sdEntries = updated) }
    viewModelScope.launch { repository.saveSdEntries(updated) }
  }

  fun deleteSdEntry(id: String) {
    val updated = _uiState.value.sdEntries.filterNot { it.id == id }
    _uiState.update { it.copy(sdEntries = updated) }
    viewModelScope.launch { repository.saveSdEntries(updated) }
  }

  fun clearSdEntries() {
    _uiState.update { it.copy(sdEntries = emptyList()) }
    viewModelScope.launch { repository.saveSdEntries(emptyList()) }
  }

  fun addRegEntry(x: Double, y: Double, freq: Int = 1) {
    val updated = _uiState.value.regEntries + RegEntry(UUID.randomUUID().toString(), x, y, freq.coerceAtLeast(1))
    _uiState.update { it.copy(regEntries = updated) }
    viewModelScope.launch { repository.saveRegEntries(updated) }
  }

  fun updateRegEntry(id: String, x: Double, y: Double, freq: Int) {
    val updated =
      _uiState.value.regEntries.map {
        if (it.id == id) it.copy(x = x, y = y, freq = freq.coerceAtLeast(1)) else it
      }
    _uiState.update { it.copy(regEntries = updated) }
    viewModelScope.launch { repository.saveRegEntries(updated) }
  }

  fun deleteRegEntry(id: String) {
    val updated = _uiState.value.regEntries.filterNot { it.id == id }
    _uiState.update { it.copy(regEntries = updated) }
    viewModelScope.launch { repository.saveRegEntries(updated) }
  }

  fun clearRegEntries() {
    _uiState.update { it.copy(regEntries = emptyList()) }
    viewModelScope.launch { repository.saveRegEntries(emptyList()) }
  }

  fun setShowStatDataSheet(show: Boolean) {
    _uiState.update { it.copy(showStatDataSheet = show) }
  }

  fun setMode(mode: CalculatorMode) {
    _uiState.update {
      it.copy(
        mode = mode,
        activeMenu = ActiveCalcMenu.None,
        expression = "",
        cursorIndex = 0,
        resultDisplay = "0",
        primaryLabel = null,
        secondaryResultDisplay = null,
        justEvaluated = false,
      )
    }
    persistSettings()
  }

  fun setRegMode(type: RegressionType) {
    _uiState.update {
      it.copy(
        mode = CalculatorMode.REG,
        regressionType = type,
        activeMenu = ActiveCalcMenu.None,
        expression = "",
        cursorIndex = 0,
        resultDisplay = "0",
        primaryLabel = null,
        secondaryResultDisplay = "${type.code}: ${type.formula}",
        justEvaluated = false,
      )
    }
    persistSettings()
  }

  fun setAngleUnit(unit: AngleUnit) {
    _uiState.update { it.copy(angleUnit = unit) }
    reformatCurrentResult()
    persistSettings()
  }

  fun setDisplayMode(mode: DisplayMode, fixDigits: Int? = null, sciDigits: Int? = null) {
    _uiState.update {
      it.copy(
        displayMode = mode,
        fixDigits = fixDigits ?: it.fixDigits,
        sciDigits = sciDigits ?: it.sciDigits,
        engStepShift = null,
      )
    }
    reformatCurrentResult()
    persistSettings()
  }

  fun toggleFractionPreference() {
    val next =
      if (_uiState.value.fractionPreference == FractionPreference.MIXED) FractionPreference.IMPROPER
      else FractionPreference.MIXED
    _uiState.update { it.copy(fractionPreference = next) }
    reformatCurrentResult()
    persistSettings()
  }

  fun setThemeMode(themeMode: ThemeMode) {
    _uiState.update { it.copy(themeMode = themeMode) }
    persistSettings()
  }

  fun setSoundEnabled(enabled: Boolean) {
    _uiState.update { it.copy(soundEnabled = enabled) }
    persistSettings()
  }

  fun setHapticEnabled(enabled: Boolean) {
    _uiState.update { it.copy(hapticEnabled = enabled) }
    persistSettings()
  }

  fun clearMemoryVariables() {
    _uiState.update {
      it.copy(
        variables = emptyMap(),
        ans = 0.0,
        primaryLabel = null,
        resultDisplay = "Mem Cleared",
        secondaryResultDisplay = null,
        justEvaluated = false,
      )
    }
    persistVars(emptyMap(), 0.0)
  }

  fun resetModeAndDisplay() {
    _uiState.update {
      it.copy(
        mode = CalculatorMode.COMP,
        angleUnit = AngleUnit.DEG,
        displayMode = DisplayMode.NORM1,
        fixDigits = 2,
        sciDigits = 3,
        fractionPreference = FractionPreference.MIXED,
        expression = "",
        cursorIndex = 0,
        resultDisplay = "0",
        primaryLabel = null,
        secondaryResultDisplay = "Mode Reset",
        justEvaluated = false,
      )
    }
    persistSettings()
  }

  fun factoryResetAll() {
    _uiState.update { s ->
      CalculatorUiState(
        themeMode = s.themeMode,
        soundEnabled = s.soundEnabled,
        hapticEnabled = s.hapticEnabled,
        secondaryResultDisplay = "Reset All Done",
      )
    }
    viewModelScope.launch { repository.factoryResetAll() }
  }

  private fun reformatCurrentResult() {
    val state = _uiState.value
    val valObj = state.lastEvalValue ?: return
    if (!state.justEvaluated) return
    val formatted =
      NumberFormatter.format(
        value = valObj.number,
        displayMode = state.displayMode,
        fixDigits = state.fixDigits,
        sciDigits = state.sciDigits,
        asDms = state.showAsDms,
        fraction = valObj.fraction,
        showFraction = state.showAsFraction,
        fractionPreference = state.fractionPreference,
      )
    _uiState.update { it.copy(resultDisplay = formatted) }
  }

  private fun persistSettings() {
    val s = _uiState.value
    viewModelScope.launch {
      repository.saveSettings(
        themeMode = s.themeMode,
        soundEnabled = s.soundEnabled,
        hapticEnabled = s.hapticEnabled,
        angleUnit = s.angleUnit,
        mode = s.mode,
        regressionType = s.regressionType,
        displayMode = s.displayMode,
        fixDigits = s.fixDigits,
        sciDigits = s.sciDigits,
        fractionPreference = s.fractionPreference,
      )
    }
  }

  private fun persistVars(vars: Map<CalcVariable, Double>, ans: Double) {
    viewModelScope.launch { repository.saveVariablesAndAns(vars, ans) }
  }

  /** Handles physical keyboard KeyEvents */
  fun onKeyboardChar(char: Char): Boolean {
    return when (char) {
      '0' -> { onKeyPress(CalcKey.D0); true }
      '1' -> { onKeyPress(CalcKey.D1); true }
      '2' -> { onKeyPress(CalcKey.D2); true }
      '3' -> { onKeyPress(CalcKey.D3); true }
      '4' -> { onKeyPress(CalcKey.D4); true }
      '5' -> { onKeyPress(CalcKey.D5); true }
      '6' -> { onKeyPress(CalcKey.D6); true }
      '7' -> { onKeyPress(CalcKey.D7); true }
      '8' -> { onKeyPress(CalcKey.D8); true }
      '9' -> { onKeyPress(CalcKey.D9); true }
      '.' -> { onKeyPress(CalcKey.DOT); true }
      '+' -> { onKeyPress(CalcKey.PLUS); true }
      '-' -> { onKeyPress(CalcKey.MINUS); true }
      '*' -> { onKeyPress(CalcKey.MUL); true }
      '/' -> { onKeyPress(CalcKey.DIV); true }
      '(' -> { onKeyPress(CalcKey.LPAREN); true }
      ')' -> { onKeyPress(CalcKey.RPAREN); true }
      '^' -> { onKeyPress(CalcKey.POW); true }
      '%' -> { insertToken("%"); true }
      '!' -> { insertToken("!"); true }
      ',' -> { onKeyPress(CalcKey.COMMA); true }
      ';' -> { insertToken(";"); true }
      '=' -> { onKeyPress(CalcKey.EQUALS); true }
      else -> false
    }
  }
}
