package com.example.ui.screens

import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CalculatorKeypad
import com.example.ui.components.LcdDisplay
import com.example.ui.components.StatDataSheet
import com.example.ui.theme.HardwareChassisBottom
import com.example.ui.theme.HardwareChassisMid
import com.example.ui.theme.HardwareChassisTop
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.viewmodel.CalcKey
import com.example.viewmodel.CalculatorUiState
import com.example.viewmodel.CalculatorViewModel

@Composable
fun CalculatorScreen(
  state: CalculatorUiState,
  viewModel: CalculatorViewModel,
) {
  val context = LocalContext.current
  val haptic = LocalHapticFeedback.current
  val audioManager = remember(context) {
    context.getSystemService(AudioManager::class.java)
  }
  val focusRequester = remember { FocusRequester() }

  LaunchedEffect(Unit) {
    runCatching { focusRequester.requestFocus() }
  }

  val handleKey: (CalcKey) -> Unit = { calcKey ->
    if (state.hapticEnabled) {
      val type =
        if (calcKey == CalcKey.EQUALS || calcKey == CalcKey.ON_AC) HapticFeedbackType.LongPress
        else HapticFeedbackType.TextHandleMove
      haptic.performHapticFeedback(type)
    }
    if (state.soundEnabled) {
      runCatching { audioManager?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.35f) }
    }
    viewModel.onKeyPress(calcKey)
  }

  if (state.showStatDataSheet) {
    StatDataSheet(
      state = state,
      onDismiss = { viewModel.setShowStatDataSheet(false) },
      onSelectRegType = { viewModel.setRegMode(it) },
      onAddSd = { x, f -> viewModel.addSdEntry(x, f) },
      onUpdateSd = { id, x, f -> viewModel.updateSdEntry(id, x, f) },
      onDeleteSd = { id -> viewModel.deleteSdEntry(id) },
      onClearSd = { viewModel.clearSdEntries() },
      onAddReg = { x, y, f -> viewModel.addRegEntry(x, y, f) },
      onUpdateReg = { id, x, y, f -> viewModel.updateRegEntry(id, x, y, f) },
      onDeleteReg = { id -> viewModel.deleteRegEntry(id) },
      onClearReg = { viewModel.clearRegEntries() },
    )
  }

  // Full-bleed standalone calculator face (no phone navigation chrome, no nested device frame)
  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(HardwareChassisTop, HardwareChassisMid, HardwareChassisBottom)
          )
        )
        .statusBarsPadding()
        .navigationBarsPadding()
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
          if (event.type != KeyEventType.KeyDown || event.isCtrlPressed) return@onPreviewKeyEvent false
          when (event.key) {
            Key.Enter, Key.NumPadEnter -> { handleKey(CalcKey.EQUALS); true }
            Key.Backspace -> { handleKey(CalcKey.DEL); true }
            Key.Escape -> { handleKey(CalcKey.ON_AC); true }
            Key.DirectionLeft -> { handleKey(CalcKey.LEFT); true }
            Key.DirectionRight -> { handleKey(CalcKey.RIGHT); true }
            Key.DirectionUp -> { handleKey(CalcKey.UP); true }
            Key.DirectionDown -> { handleKey(CalcKey.DOWN); true }
            else -> {
              val unicode = event.nativeKeyEvent.unicodeChar
              if (unicode != 0) {
                viewModel.onKeyboardChar(unicode.toChar())
              } else {
                false
              }
            }
          }
        },
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 480.dp)
          .fillMaxWidth()
          .fillMaxHeight()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 14.dp, vertical = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Clean Hardware Faceplate Header (no menus or navigation icons)
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "SCIENTIFIC PRO",
          fontFamily = SpaceGroteskFontFamily,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold,
          color = Color(0xFFF2F4F5),
          letterSpacing = 1.2.sp,
        )
        Text(
          text = "fx-82MS",
          fontFamily = SpaceGroteskFontFamily,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = Color(0xFFE4E6E8),
        )
      }

      // Centered S-V.P.A.M. & right-aligned 2nd edition above LCD
      Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
          text = "S-V.P.A.M.",
          fontFamily = SpaceGroteskFontFamily,
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold,
          fontStyle = FontStyle.Italic,
          color = Color(0xFFF2F4F5),
          letterSpacing = 1.5.sp,
          modifier = Modifier.align(Alignment.Center),
        )
        Text(
          text = "2nd edition",
          fontFamily = SpaceGroteskFontFamily,
          fontSize = 10.sp,
          color = Color(0xFFD5D7DA),
          modifier = Modifier.align(Alignment.BottomEnd).padding(top = 15.dp),
        )
      }

      // Recessed Two-Line LCD Screen
      LcdDisplay(
        state = state,
        onSelectMenuOption = viewModel::selectMenuOption,
        onInsertStatToken = viewModel::insertStatTokenAndDismiss,
        onDismissMenu = viewModel::dismissMenu,
      )

      Spacer(modifier = Modifier.height(2.dp))

      // Full Casio fx-82MS 2nd Edition Physical Keypad
      CalculatorKeypad(
        state = state,
        onKey = handleKey,
      )
    }
  }
}
