package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CalculatorMode
import com.example.engine.NumberFormatter
import com.example.engine.RegressionType
import com.example.engine.StatisticsEngine
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.CalculatorUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StatDataSheet(
  state: CalculatorUiState,
  onDismiss: () -> Unit,
  onSelectRegType: (RegressionType) -> Unit,
  onAddSd: (Double, Int) -> Unit,
  onUpdateSd: (String, Double, Int) -> Unit,
  onDeleteSd: (String) -> Unit,
  onClearSd: () -> Unit,
  onAddReg: (Double, Double, Int) -> Unit,
  onUpdateReg: (String, Double, Double, Int) -> Unit,
  onDeleteReg: (String) -> Unit,
  onClearReg: () -> Unit,
) {
  val isReg = state.mode == CalculatorMode.REG
  var xInput by remember { mutableStateOf("") }
  var yInput by remember { mutableStateOf("") }
  var freqInput by remember { mutableStateOf("1") }
  var editingId by remember { mutableStateOf<String?>(null) }
  var predictInput by remember { mutableStateOf("") }
  var predictDirectionIsYFromX by remember { mutableStateOf(true) }

  ModalBottomSheet(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 24.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = if (isReg) "REG Statistics & Regression" else "SD Standard Deviation Data",
            style = MaterialTheme.typography.titleLarge,
          )
          Text(
            text =
              if (isReg) "Model: ${state.regressionType.title} (${state.regressionType.formula})"
              else "Enter values with optional frequency (or use DT key)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        IconButton(
          onClick = { if (isReg) onClearReg() else onClearSd() },
          modifier = Modifier.testTag("stat_clear_all"),
        ) {
          Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all statistical data")
        }
      }

      if (isReg) {
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          RegressionType.entries.forEach { regType ->
            FilterChip(
              selected = state.regressionType == regType,
              onClick = { onSelectRegType(regType) },
              label = { Text("${regType.code}: ${regType.title}", fontSize = 12.sp) },
              modifier = Modifier.testTag("reg_type_${regType.code.lowercase()}"),
            )
          }
        }
      }

      // Data entry row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = xInput,
          onValueChange = { xInput = it },
          label = { Text("x") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          modifier = Modifier.weight(1f).testTag("stat_input_x"),
        )
        if (isReg) {
          OutlinedTextField(
            value = yInput,
            onValueChange = { yInput = it },
            label = { Text("y") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).testTag("stat_input_y"),
          )
        }
        OutlinedTextField(
          value = freqInput,
          onValueChange = { freqInput = it },
          label = { Text("Freq") },
          singleLine = true,
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.width(72.dp).testTag("stat_input_freq"),
        )
        Button(
          onClick = {
            val xVal = xInput.toDoubleOrNull() ?: return@Button
            val fVal = (freqInput.toIntOrNull() ?: 1).coerceAtLeast(1)
            if (isReg) {
              val yVal = yInput.toDoubleOrNull() ?: return@Button
              val eid = editingId
              if (eid != null) onUpdateReg(eid, xVal, yVal, fVal)
              else onAddReg(xVal, yVal, fVal)
            } else {
              val eid = editingId
              if (eid != null) onUpdateSd(eid, xVal, fVal)
              else onAddSd(xVal, fVal)
            }
            xInput = ""
            yInput = ""
            freqInput = "1"
            editingId = null
          },
          modifier = Modifier.height(54.dp).testTag("stat_add_button"),
        ) {
          Icon(
            imageVector = if (editingId != null) Icons.Default.Edit else Icons.Default.Add,
            contentDescription = if (editingId != null) "Update entry" else "Add entry",
          )
        }
      }

      // Summary Results Card
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          if (!isReg) {
            val s = state.sdSummary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              StatMetric("n", s.n.toString())
              StatMetric("Σx", NumberFormatter.formatCompact(s.sumX))
              StatMetric("Σx²", NumberFormatter.formatCompact(s.sumX2))
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              StatMetric("x̄", if (s.n > 0) NumberFormatter.formatCompact(s.mean) else "—")
              StatMetric("xσn", if (s.n > 0) NumberFormatter.formatCompact(s.popStdDev) else "—")
              StatMetric("xσn-1", if (s.n > 1) NumberFormatter.formatCompact(s.sampleStdDev) else "—")
            }
          } else {
            val reg = state.regSummary
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              StatMetric("n", reg.n.toString())
              StatMetric("x̄", if (reg.n > 0) NumberFormatter.formatCompact(reg.meanX) else "—")
              StatMetric("ȳ", if (reg.n > 0) NumberFormatter.formatCompact(reg.meanY) else "—")
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              StatMetric("Σx", NumberFormatter.formatCompact(reg.sumX))
              StatMetric("Σy", NumberFormatter.formatCompact(reg.sumY))
              StatMetric("Σxy", NumberFormatter.formatCompact(reg.sumXY))
            }
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              StatMetric("A", if (reg.a.isFinite()) NumberFormatter.formatCompact(reg.a) else "—")
              StatMetric("B", if (reg.b.isFinite()) NumberFormatter.formatCompact(reg.b) else "—")
              if (reg.c != null) {
                StatMetric("C", if (reg.c.isFinite()) NumberFormatter.formatCompact(reg.c) else "—")
              } else {
                StatMetric("r", if (reg.r != null && reg.r.isFinite()) NumberFormatter.formatCompact(reg.r) else "—")
              }
            }
            if (reg.c != null && reg.r != null) {
              StatMetric("R (Multiple Corr)", if (reg.r.isFinite()) NumberFormatter.formatCompact(reg.r) else "—")
            }

            // Prediction quick calculator
            if (reg.a.isFinite() && reg.b.isFinite()) {
              HorizontalDivider()
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                OutlinedButton(
                  onClick = { predictDirectionIsYFromX = !predictDirectionIsYFromX },
                  modifier = Modifier.height(44.dp),
                ) {
                  Text(if (predictDirectionIsYFromX) "ŷ(x)" else "x̂(y)", fontFamily = JetBrainsMonoFontFamily)
                }
                OutlinedTextField(
                  value = predictInput,
                  onValueChange = { predictInput = it },
                  label = { Text(if (predictDirectionIsYFromX) "Given x" else "Given y") },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                  modifier = Modifier.weight(1f),
                )
                val predResult =
                  predictInput.toDoubleOrNull()?.let { v ->
                    runCatching {
                      if (predictDirectionIsYFromX) StatisticsEngine.predictY(reg, v)
                      else StatisticsEngine.predictX(reg, v)
                    }.getOrNull()
                  }
                Text(
                  text = predResult?.let { NumberFormatter.formatCompact(it) } ?: "—",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp,
                )
              }
            }
          }
        }
      }

      // Data list
      val hasData = if (isReg) state.regEntries.isNotEmpty() else state.sdEntries.isNotEmpty()
      if (!hasData) {
        Text(
          text = "No entries yet. Add values above or enter in calculator and press DT (M+).",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(vertical = 8.dp),
        )
      } else {
        LazyColumn(
          modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (!isReg) {
            itemsIndexed(state.sdEntries, key = { _, item -> item.id }) { idx, item ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "#${idx + 1}   x = ${NumberFormatter.formatCompact(item.x)}   (f=${item.freq})",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 13.sp,
                )
                Row {
                  IconButton(
                    onClick = {
                      editingId = item.id
                      xInput = item.x.toString()
                      freqInput = item.freq.toString()
                    }
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit entry")
                  }
                  IconButton(onClick = { onDeleteSd(item.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                  }
                }
              }
            }
          } else {
            itemsIndexed(state.regEntries, key = { _, item -> item.id }) { idx, item ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = "#${idx + 1}  (${NumberFormatter.formatCompact(item.x)}, ${NumberFormatter.formatCompact(item.y)})  f=${item.freq}",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 13.sp,
                )
                Row {
                  IconButton(
                    onClick = {
                      editingId = item.id
                      xInput = item.x.toString()
                      yInput = item.y.toString()
                      freqInput = item.freq.toString()
                    }
                  ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit entry")
                  }
                  IconButton(onClick = { onDeleteReg(item.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                  }
                }
              }
            }
          }
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text("Done") }
      }
    }
  }
}

@Composable
private fun StatMetric(label: String, value: String) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 13.sp,
      fontWeight = FontWeight.Bold,
    )
  }
}
