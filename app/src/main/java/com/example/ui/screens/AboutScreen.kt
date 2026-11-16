package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("About Scientific Calculator Pro") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("about_back")) {
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
          shape = RoundedCornerShape(16.dp),
        ) {
          Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Scientific Calculator Pro", style = MaterialTheme.typography.headlineSmall)
            Text(
              text = "A faithful, high-precision scientific calculator inspired by the classic fx-82MS 2nd Edition (240+ functions) for students, teachers, and engineers.",
              style = MaterialTheme.typography.bodyLarge,
            )
          }
        }

        AboutFeatureRow(
          icon = Icons.Default.LockOpen,
          title = "100% Free & Ad-Free",
          subtitle = "No accounts, no subscriptions, no paywalls, and no advertisements interrupting your homework or exams.",
        )
        AboutFeatureRow(
          icon = Icons.Default.OfflinePin,
          title = "100% Offline Mathematical Engine",
          subtitle = "All calculations, statistical computations, history logs, and saved memory variables run locally on your device without any internet connection.",
        )
        AboutFeatureRow(
          icon = Icons.Default.School,
          title = "Built for Education",
          subtitle = "Designed so students and educators always have reliable access to exact fraction arithmetic, trigonometry, DMS conversions, SD statistics, and 6 regression models even when a physical calculator is left at school or home.",
        )
        AboutFeatureRow(
          icon = Icons.Default.Calculate,
          title = "Authentic fx-82MS Operation",
          subtitle = "Includes SHIFT and ALPHA modifiers, 4-way REPLAY cursor and history navigation, 9 independent memory variables (A–F, X, Y, M), Ans, DEG/RAD/GRA modes, and FIX/SCI/NORM/ENG formatting.",
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Independent educational software. Not affiliated with or endorsed by Casio Computer Co., Ltd.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun AboutFeatureRow(icon: ImageVector, title: String, subtitle: String) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      horizontalArrangement = Arrangement.spacedBy(14.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  }
}
