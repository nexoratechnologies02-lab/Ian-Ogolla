package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.engine.AngleUnit
import com.example.engine.CalcVariable
import com.example.engine.CalculatorMode
import com.example.engine.DisplayMode
import com.example.engine.FractionPreference
import com.example.engine.HistoryEntry
import com.example.engine.RegEntry
import com.example.engine.RegressionType
import com.example.engine.StatEntry
import com.example.engine.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.calculatorDataStore: DataStore<Preferences> by
  preferencesDataStore(name = "scientific_calculator_pro_prefs")

data class PersistedCalculatorState(
  val themeMode: ThemeMode = ThemeMode.DARK,
  val soundEnabled: Boolean = true,
  val hapticEnabled: Boolean = true,
  val angleUnit: AngleUnit = AngleUnit.DEG,
  val mode: CalculatorMode = CalculatorMode.COMP,
  val regressionType: RegressionType = RegressionType.LIN,
  val displayMode: DisplayMode = DisplayMode.NORM1,
  val fixDigits: Int = 2,
  val sciDigits: Int = 3,
  val fractionPreference: FractionPreference = FractionPreference.MIXED,
  val variables: Map<CalcVariable, Double> = emptyMap(),
  val ans: Double = 0.0,
  val history: List<HistoryEntry> = emptyList(),
  val sdEntries: List<StatEntry> = emptyList(),
  val regEntries: List<RegEntry> = emptyList(),
)

class CalculatorRepository(private val context: Context) {

  private object Keys {
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
    val ANGLE_UNIT = stringPreferencesKey("angle_unit")
    val CALC_MODE = stringPreferencesKey("calc_mode")
    val REG_TYPE = stringPreferencesKey("reg_type")
    val DISPLAY_MODE = stringPreferencesKey("display_mode")
    val FIX_DIGITS = intPreferencesKey("fix_digits")
    val SCI_DIGITS = intPreferencesKey("sci_digits")
    val FRACTION_PREF = stringPreferencesKey("fraction_pref")
    val VARIABLES_JSON = stringPreferencesKey("variables_json")
    val ANS_VALUE = stringPreferencesKey("ans_value")
    val HISTORY_JSON = stringPreferencesKey("history_json")
    val SD_ENTRIES_JSON = stringPreferencesKey("sd_entries_json")
    val REG_ENTRIES_JSON = stringPreferencesKey("reg_entries_json")
  }

  val stateFlow: Flow<PersistedCalculatorState> =
    context.calculatorDataStore.data.map { prefs ->
      PersistedCalculatorState(
        themeMode = enumOrDefault(prefs[Keys.THEME_MODE], ThemeMode.DARK),
        soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
        hapticEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
        angleUnit = enumOrDefault(prefs[Keys.ANGLE_UNIT], AngleUnit.DEG),
        mode = enumOrDefault(prefs[Keys.CALC_MODE], CalculatorMode.COMP),
        regressionType = enumOrDefault(prefs[Keys.REG_TYPE], RegressionType.LIN),
        displayMode = enumOrDefault(prefs[Keys.DISPLAY_MODE], DisplayMode.NORM1),
        fixDigits = (prefs[Keys.FIX_DIGITS] ?: 2).coerceIn(0, 9),
        sciDigits = (prefs[Keys.SCI_DIGITS] ?: 3).coerceIn(1, 10),
        fractionPreference = enumOrDefault(prefs[Keys.FRACTION_PREF], FractionPreference.MIXED),
        variables = decodeVariables(prefs[Keys.VARIABLES_JSON]),
        ans = prefs[Keys.ANS_VALUE]?.toDoubleOrNull() ?: 0.0,
        history = decodeHistory(prefs[Keys.HISTORY_JSON]),
        sdEntries = decodeSdEntries(prefs[Keys.SD_ENTRIES_JSON]),
        regEntries = decodeRegEntries(prefs[Keys.REG_ENTRIES_JSON]),
      )
    }

  suspend fun saveSettings(
    themeMode: ThemeMode,
    soundEnabled: Boolean,
    hapticEnabled: Boolean,
    angleUnit: AngleUnit,
    mode: CalculatorMode,
    regressionType: RegressionType,
    displayMode: DisplayMode,
    fixDigits: Int,
    sciDigits: Int,
    fractionPreference: FractionPreference,
  ) {
    context.calculatorDataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = themeMode.name
      prefs[Keys.SOUND_ENABLED] = soundEnabled
      prefs[Keys.HAPTIC_ENABLED] = hapticEnabled
      prefs[Keys.ANGLE_UNIT] = angleUnit.name
      prefs[Keys.CALC_MODE] = mode.name
      prefs[Keys.REG_TYPE] = regressionType.name
      prefs[Keys.DISPLAY_MODE] = displayMode.name
      prefs[Keys.FIX_DIGITS] = fixDigits.coerceIn(0, 9)
      prefs[Keys.SCI_DIGITS] = sciDigits.coerceIn(1, 10)
      prefs[Keys.FRACTION_PREF] = fractionPreference.name
    }
  }

  suspend fun saveVariablesAndAns(variables: Map<CalcVariable, Double>, ans: Double) {
    context.calculatorDataStore.edit { prefs ->
      prefs[Keys.VARIABLES_JSON] = encodeVariables(variables)
      prefs[Keys.ANS_VALUE] = ans.toString()
    }
  }

  suspend fun saveHistory(history: List<HistoryEntry>) {
    context.calculatorDataStore.edit { prefs ->
      prefs[Keys.HISTORY_JSON] = encodeHistory(history.take(100))
    }
  }

  suspend fun saveSdEntries(entries: List<StatEntry>) {
    context.calculatorDataStore.edit { prefs ->
      prefs[Keys.SD_ENTRIES_JSON] = encodeSdEntries(entries)
    }
  }

  suspend fun saveRegEntries(entries: List<RegEntry>) {
    context.calculatorDataStore.edit { prefs ->
      prefs[Keys.REG_ENTRIES_JSON] = encodeRegEntries(entries)
    }
  }

  suspend fun factoryResetAll() {
    context.calculatorDataStore.edit { prefs ->
      val keepTheme = prefs[Keys.THEME_MODE]
      val keepSound = prefs[Keys.SOUND_ENABLED]
      val keepHaptic = prefs[Keys.HAPTIC_ENABLED]
      prefs.clear()
      if (keepTheme != null) prefs[Keys.THEME_MODE] = keepTheme
      if (keepSound != null) prefs[Keys.SOUND_ENABLED] = keepSound
      if (keepHaptic != null) prefs[Keys.HAPTIC_ENABLED] = keepHaptic
    }
  }

  private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, fallback: T): T {
    if (raw == null) return fallback
    return runCatching { enumValueOf<T>(raw) }.getOrDefault(fallback)
  }

  private fun encodeVariables(vars: Map<CalcVariable, Double>): String {
    val obj = JSONObject()
    for ((k, v) in vars) {
      if (v.isFinite()) obj.put(k.name, v)
    }
    return obj.toString()
  }

  private fun decodeVariables(json: String?): Map<CalcVariable, Double> {
    if (json.isNullOrBlank()) return emptyMap()
    return runCatching {
      val obj = JSONObject(json)
      val map = mutableMapOf<CalcVariable, Double>()
      for (v in CalcVariable.entries) {
        if (obj.has(v.name)) {
          val num = obj.optDouble(v.name, 0.0)
          if (num.isFinite()) map[v] = num
        }
      }
      map.toMap()
    }.getOrDefault(emptyMap())
  }

  private fun encodeHistory(list: List<HistoryEntry>): String {
    val arr = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("expr", item.expression)
      obj.put("res", item.formattedResult)
      obj.put("num", item.numericResult)
      obj.put("ang", item.angleUnit.name)
      obj.put("mode", item.mode.name)
      obj.put("ts", item.timestampMs)
      arr.put(obj)
    }
    return arr.toString()
  }

  private fun decodeHistory(json: String?): List<HistoryEntry> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { idx ->
        val o = arr.getJSONObject(idx)
        HistoryEntry(
          id = o.optString("id"),
          expression = o.optString("expr"),
          formattedResult = o.optString("res"),
          numericResult = o.optDouble("num", 0.0),
          angleUnit = enumOrDefault(o.optString("ang"), AngleUnit.DEG),
          mode = enumOrDefault(o.optString("mode"), CalculatorMode.COMP),
          timestampMs = o.optLong("ts", 0L),
        )
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeSdEntries(list: List<StatEntry>): String {
    val arr = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("x", item.x)
      obj.put("freq", item.freq)
      arr.put(obj)
    }
    return arr.toString()
  }

  private fun decodeSdEntries(json: String?): List<StatEntry> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { idx ->
        val o = arr.getJSONObject(idx)
        StatEntry(
          id = o.optString("id"),
          x = o.optDouble("x", 0.0),
          freq = o.optInt("freq", 1).coerceAtLeast(1),
        )
      }
    }.getOrDefault(emptyList())
  }

  private fun encodeRegEntries(list: List<RegEntry>): String {
    val arr = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("id", item.id)
      obj.put("x", item.x)
      obj.put("y", item.y)
      obj.put("freq", item.freq)
      arr.put(obj)
    }
    return arr.toString()
  }

  private fun decodeRegEntries(json: String?): List<RegEntry> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
      val arr = JSONArray(json)
      List(arr.length()) { idx ->
        val o = arr.getJSONObject(idx)
        RegEntry(
          id = o.optString("id"),
          x = o.optDouble("x", 0.0),
          y = o.optDouble("y", 0.0),
          freq = o.optInt("freq", 1).coerceAtLeast(1),
        )
      }
    }.getOrDefault(emptyList())
  }
}
