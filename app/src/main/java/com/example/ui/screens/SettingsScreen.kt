package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AngleUnit
import com.example.engine.CalcVariable
import com.example.engine.DisplayMode
import com.example.engine.FractionPreference
import com.example.engine.NumberFormatter
import com.example.engine.ThemeMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.CalculatorUiState
import com.example.viewmodel.CalculatorViewModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  state: CalculatorUiState,
  viewModel: CalculatorViewModel,
  onBack: () -> Unit,
) {
  var showFactoryConfirm by remember { mutableStateOf(false) }

  if (showFactoryConfirm) {
    AlertDialog(
      onDismissRequest = { showFactoryConfirm = false },
      title = { Text("Factory Reset Calculator?") },
      text = {
        Text("This clears all saved memory variables, calculation history, SD/REG datasets, and restores default COMP/DEG/NORM1 settings.")
      },
      confirmButton = {
        TextButton(
          onClick = {
            viewModel.factoryResetAll()
            showFactoryConfirm = false
          }
        ) {
          Text("Reset All", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { showFactoryConfirm = false }) { Text("Cancel") }
      },
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier =
          Modifier.widthIn(max = 640.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Appearance
        SettingsSectionCard(title = "Appearance") {
          Text("Theme", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { mode ->
              FilterChip(
                selected = state.themeMode == mode,
                onClick = { viewModel.setThemeMode(mode) },
                label = { Text(mode.label) },
                modifier = Modifier.testTag("theme_${mode.name.lowercase()}"),
              )
            }
          }
        }

        // Feedback
        SettingsSectionCard(title = "Tactile Feedback") {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Key Sound", style = MaterialTheme.typography.titleMedium)
              Text("Subtle click sound on key press", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = state.soundEnabled,
              onCheckedChange = viewModel::setSoundEnabled,
              modifier = Modifier.testTag("switch_sound"),
            )
          }
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column {
              Text("Haptic Vibration", style = MaterialTheme.typography.titleMedium)
              Text("Tactile vibration feedback where supported", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
              checked = state.hapticEnabled,
              onCheckedChange = viewModel::setHapticEnabled,
              modifier = Modifier.testTag("switch_haptic"),
            )
          }
        }

        // Display & Calculation Setup
        SettingsSectionCard(title = "Display & Angle Setup") {
          Text("Angle Unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AngleUnit.entries.forEach { unit ->
              FilterChip(
                selected = state.angleUnit == unit,
                onClick = { viewModel.setAngleUnit(unit) },
                label = { Text("${unit.name} (${unit.label})") },
                modifier = Modifier.testTag("angle_${unit.name.lowercase()}"),
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text("Number Format", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = state.displayMode == DisplayMode.NORM1,
              onClick = { viewModel.setDisplayMode(DisplayMode.NORM1) },
              label = { Text("Norm 1") },
            )
            FilterChip(
              selected = state.displayMode == DisplayMode.NORM2,
              onClick = { viewModel.setDisplayMode(DisplayMode.NORM2) },
              label = { Text("Norm 2") },
            )
            FilterChip(
              selected = state.displayMode == DisplayMode.FIX,
              onClick = { viewModel.setDisplayMode(DisplayMode.FIX) },
              label = { Text("Fix (${state.fixDigits})") },
            )
            FilterChip(
              selected = state.displayMode == DisplayMode.SCI,
              onClick = { viewModel.setDisplayMode(DisplayMode.SCI) },
              label = { Text("Sci (${state.sciDigits})") },
            )
          }

          if (state.displayMode == DisplayMode.FIX) {
            Text("Decimal Places (Fix): ${state.fixDigits}", style = MaterialTheme.typography.bodyMedium)
            Slider(
              value = state.fixDigits.toFloat(),
              onValueChange = { viewModel.setDisplayMode(DisplayMode.FIX, fixDigits = it.roundToInt()) },
              valueRange = 0f..9f,
              steps = 8,
            )
          }
          if (state.displayMode == DisplayMode.SCI) {
            Text("Significant Digits (Sci): ${state.sciDigits}", style = MaterialTheme.typography.bodyMedium)
            Slider(
              value = state.sciDigits.toFloat(),
              onValueChange = { viewModel.setDisplayMode(DisplayMode.SCI, sciDigits = it.roundToInt()) },
              valueRange = 1f..10f,
              steps = 8,
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text("Fraction Format", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = state.fractionPreference == FractionPreference.MIXED,
              onClick = {
                if (state.fractionPreference != FractionPreference.MIXED) viewModel.toggleFractionPreference()
              },
              label = { Text("Mixed (a b/c)") },
            )
            FilterChip(
              selected = state.fractionPreference == FractionPreference.IMPROPER,
              onClick = {
                if (state.fractionPreference != FractionPreference.IMPROPER) viewModel.toggleFractionPreference()
              },
              label = { Text("Improper (d/c)") },
            )
          }
        }

        // Saved Memory Variables
        SettingsSectionCard(title = "Saved Memory Variables (Offline)") {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CalcVariable.entries.forEach { v ->
              val num = state.variables[v] ?: 0.0
              Text(
                text = "${v.symbol} = ${NumberFormatter.formatCompact(num)}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
              )
            }
            Text(
              text = "Ans = ${NumberFormatter.formatCompact(state.ans)}",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        // Reset Controls
        SettingsSectionCard(title = "Reset Calculator") {
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = viewModel::clearMemoryVariables, modifier = Modifier.testTag("reset_memory_btn")) {
              Text("Clear Memory (Mcl)")
            }
            OutlinedButton(onClick = viewModel::resetModeAndDisplay, modifier = Modifier.testTag("reset_mode_btn")) {
              Text("Reset Mode & Display")
            }
            OutlinedButton(onClick = viewModel::clearHistory, modifier = Modifier.testTag("reset_history_btn")) {
              Text("Clear History")
            }
            OutlinedButton(
              onClick = { showFactoryConfirm = true },
              colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.testTag("factory_reset_btn"),
            ) {
              Text("Factory Reset All")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      content()
    }
  }
}
