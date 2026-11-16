package com.example.viewmodel

import com.example.engine.AngleUnit
import com.example.engine.CalcErrorType
import com.example.engine.CalcValue
import com.example.engine.CalcVariable
import com.example.engine.CalculatorMode
import com.example.engine.DisplayMode
import com.example.engine.FractionPreference
import com.example.engine.HistoryEntry
import com.example.engine.RegEntry
import com.example.engine.RegSummary
import com.example.engine.RegressionType
import com.example.engine.SdSummary
import com.example.engine.StatEntry
import com.example.engine.StatisticsEngine
import com.example.engine.ThemeMode

sealed interface ActiveCalcMenu {
  data object None : ActiveCalcMenu
  data object ModeSelect : ActiveCalcMenu
  data object RegTypeSelect : ActiveCalcMenu
  data object SetupSelect : ActiveCalcMenu
  data object FixDigitsSelect : ActiveCalcMenu
  data object SciDigitsSelect : ActiveCalcMenu
  data object NormSelect : ActiveCalcMenu
  data object ClearSelect : ActiveCalcMenu
  data object DrgSelect : ActiveCalcMenu
  data object StatSumSelect : ActiveCalcMenu
  data object StatVarSelect : ActiveCalcMenu
}

data class CalculatorUiState(
  // Expression & Display
  val expression: String = "",
  val cursorIndex: Int = 0,
  val resultDisplay: String = "0",
  val primaryLabel: String? = null,
  val secondaryResultDisplay: String? = null,
  val errorType: CalcErrorType? = null,
  val justEvaluated: Boolean = false,
  val lastEvalValue: CalcValue? = null,

  // Modifiers
  val isShift: Boolean = false,
  val isAlpha: Boolean = false,
  val isHyp: Boolean = false,
  val isSto: Boolean = false,
  val isRcl: Boolean = false,
  val isInsertMode: Boolean = true,

  // Result display toggles
  val showAsFraction: Boolean = false,
  val showAsDms: Boolean = false,
  val engStepShift: Int? = null,

  // Replay
  val historyReplayIndex: Int = -1,

  // Calculator Modes & Settings
  val mode: CalculatorMode = CalculatorMode.COMP,
  val regressionType: RegressionType = RegressionType.LIN,
  val angleUnit: AngleUnit = AngleUnit.DEG,
  val displayMode: DisplayMode = DisplayMode.NORM1,
  val fixDigits: Int = 2,
  val sciDigits: Int = 3,
  val fractionPreference: FractionPreference = FractionPreference.MIXED,

  // App Preferences
  val themeMode: ThemeMode = ThemeMode.DARK,
  val soundEnabled: Boolean = true,
  val hapticEnabled: Boolean = true,

  // Memory & Data
  val variables: Map<CalcVariable, Double> = emptyMap(),
  val ans: Double = 0.0,
  val history: List<HistoryEntry> = emptyList(),
  val sdEntries: List<StatEntry> = emptyList(),
  val regEntries: List<RegEntry> = emptyList(),

  // Interactive Menu / Sheets
  val activeMenu: ActiveCalcMenu = ActiveCalcMenu.None,
  val showStatDataSheet: Boolean = false,
  val statusBanner: String? = null,
) {
  val hasMemoryM: Boolean
    get() = (variables[CalcVariable.M] ?: 0.0) != 0.0

  val sdSummary: SdSummary
    get() = StatisticsEngine.computeSd(sdEntries)

  val regSummary: RegSummary
    get() = StatisticsEngine.computeReg(regEntries, regressionType)
}
