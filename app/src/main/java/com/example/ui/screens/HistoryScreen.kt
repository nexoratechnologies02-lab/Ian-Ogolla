package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.HistoryEntry
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.CalculatorUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
  state: CalculatorUiState,
  onBack: () -> Unit,
  onSelectEntry: (HistoryEntry) -> Unit,
  onDeleteEntry: (String) -> Unit,
  onClearAll: () -> Unit,
) {
  val clipboard = LocalClipboardManager.current
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  Scaffold(
    snackbarHost = { SnackbarHost(snackbarHostState) },
    topBar = {
      TopAppBar(
        title = { Text("Calculation History") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("history_back")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        actions = {
          if (state.history.isNotEmpty()) {
            IconButton(onClick = onClearAll, modifier = Modifier.testTag("history_clear_all")) {
              Icon(Icons.Default.DeleteSweep, contentDescription = "Clear all history")
            }
          }
        },
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (state.history.isEmpty()) {
        Column(
          modifier = Modifier.fillMaxSize().padding(32.dp),
          verticalArrangement = Arrangement.Center,
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp),
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text("No Calculations Yet", style = MaterialTheme.typography.titleLarge)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "All expressions and results are automatically saved locally offline for instant replay.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      } else {
        LazyColumn(
          modifier = Modifier.widthIn(max = 640.dp).fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(state.history, key = { it.id }) { entry ->
            Card(
              colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              shape = RoundedCornerShape(14.dp),
              modifier =
                Modifier.fillMaxWidth()
                  .clickable { onSelectEntry(entry) }
                  .testTag("history_item_${entry.id}"),
            ) {
              Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = { onSelectEntry(entry) }, label = { Text(entry.mode.badge, fontSize = 11.sp) })
                    AssistChip(onClick = { onSelectEntry(entry) }, label = { Text(entry.angleUnit.name, fontSize = 11.sp) })
                  }
                  Row {
                    IconButton(
                      onClick = {
                        clipboard.setText(AnnotatedString(entry.formattedResult))
                        scope.launch { snackbarHostState.showSnackbar("Copied ${entry.formattedResult}") }
                      }
                    ) {
                      Icon(Icons.Default.ContentCopy, contentDescription = "Copy result")
                    }
                    IconButton(onClick = { onDeleteEntry(entry.id) }) {
                      Icon(Icons.Default.Delete, contentDescription = "Delete entry")
                    }
                  }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = entry.expression,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 15.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = "= ${entry.formattedResult}",
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 20.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }
      }
    }
  }
}
