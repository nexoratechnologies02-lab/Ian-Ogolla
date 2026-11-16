package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CalculatorMode
import com.example.engine.DisplayMode
import com.example.ui.theme.HardwareBezelDark
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LcdInkDark
import com.example.ui.theme.LcdInkMutedDark
import com.example.ui.theme.LcdScreenBgDark
import com.example.ui.theme.LcdScreenBorderDark
import com.example.viewmodel.ActiveCalcMenu
import com.example.viewmodel.CalculatorUiState
import kotlinx.coroutines.delay

@Composable
fun LcdDisplay(
  state: CalculatorUiState,
  onSelectMenuOption: (ActiveCalcMenu, Int) -> Unit,
  onInsertStatToken: (String) -> Unit,
  onDismissMenu: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val screenBg = LcdScreenBgDark
  val screenBorder = LcdScreenBorderDark
  val inkColor = LcdInkDark
  val inkMuted = LcdInkMutedDark

  var cursorVisible by remember { mutableStateOf(true) }
  LaunchedEffect(state.cursorIndex, state.expression) {
    cursorVisible = true
    while (true) {
      delay(530)
      cursorVisible = !cursorVisible
    }
  }

  val exprScroll = rememberScrollState()
  LaunchedEffect(state.expression, state.cursorIndex) {
    exprScroll.animateScrollTo(exprScroll.maxValue)
  }

  Box(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .background(HardwareBezelDark)
        .border(1.2.dp, Color(0xFF424548), RoundedCornerShape(10.dp))
        .padding(5.dp)
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(screenBg, Color(0xFFC8D7C9))
            )
          )
          .border(1.5.dp, screenBorder, RoundedCornerShape(6.dp))
          .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
      Column(modifier = Modifier.fillMaxWidth()) {
        // Authentic LCD Status Indicators Strip ([D] SCI etc.)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (state.isShift) LcdBoxBadge("S", inkColor, screenBg)
            if (state.isAlpha) LcdBoxBadge("A", inkColor, screenBg)
            if (state.isSto) LcdTextIndicator("STO", inkColor)
            if (state.isRcl) LcdTextIndicator("RCL", inkColor)
            if (state.isHyp) LcdTextIndicator("HYP", inkColor)
            if (state.hasMemoryM) LcdTextIndicator("M", inkColor)
            if (state.mode != CalculatorMode.COMP) {
              val mText =
                if (state.mode == CalculatorMode.REG) "REG:${state.regressionType.code}"
                else state.mode.badge
              LcdTextIndicator(mText, inkColor)
            }
          }

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            LcdBoxBadge(state.angleUnit.label, inkColor, screenBg)
            val dispLabel =
              when (state.displayMode) {
                DisplayMode.FIX -> "FIX"
                DisplayMode.SCI -> "SCI"
                DisplayMode.NORM1 -> ""
                DisplayMode.NORM2 -> "N2"
              }
            if (dispLabel.isNotEmpty()) {
              LcdTextIndicator(dispLabel, inkColor)
            }
            if (state.history.isNotEmpty()) {
              Text(
                text = "▲▼",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 9.sp,
                color = inkMuted,
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Line 1: Crisp dot-matrix expression line with cursor
        val safeCur = state.cursorIndex.coerceIn(0, state.expression.length)
        val exprAnnotated = buildAnnotatedString {
          val before = state.expression.substring(0, safeCur)
          val after = state.expression.substring(safeCur)
          append(before)
          if (!state.justEvaluated) {
            withStyle(
              SpanStyle(
                color = if (cursorVisible) inkColor else Color.Transparent,
                background =
                  if (!state.isInsertMode && cursorVisible) inkColor.copy(alpha = 0.22f)
                  else Color.Transparent,
                fontWeight = FontWeight.Bold,
              )
            ) {
              append(if (state.isInsertMode) "▏" else "▯")
            }
          }
          append(after)
        }

        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(26.dp)
              .horizontalScroll(exprScroll),
          contentAlignment = Alignment.CenterStart,
        ) {
          Text(
            text =
              if (state.expression.isEmpty() && state.justEvaluated) buildAnnotatedString { append("0") }
              else exprAnnotated,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = inkColor,
            maxLines = 1,
            modifier = Modifier.testTag("lcd_expression"),
          )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Line 2: Bold Answer / Result line
        Row(
          modifier = Modifier.fillMaxWidth().height(38.dp),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.Bottom,
        ) {
          if (state.primaryLabel != null || state.secondaryResultDisplay != null) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
              if (state.primaryLabel != null) {
                Text(
                  text = state.primaryLabel,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = inkMuted,
                )
              }
              if (state.secondaryResultDisplay != null) {
                Text(
                  text = state.secondaryResultDisplay,
                  fontFamily = JetBrainsMonoFontFamily,
                  fontSize = 11.sp,
                  color = inkMuted,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.testTag("lcd_secondary_result"),
                )
              }
            }
          }

          val resultLen = state.resultDisplay.length
          val resultFontSize =
            when {
              resultLen > 16 -> 20.sp
              resultLen > 12 -> 24.sp
              else -> 29.sp
            }
          Text(
            text = state.resultDisplay,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = resultFontSize,
            fontWeight = FontWeight.Bold,
            color = if (state.errorType != null) Color(0xFF911B1B) else inkColor,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("lcd_result"),
          )
        }
      }

      // Interactive fx-82MS LCD Menu Overlay
      AnimatedVisibility(
        visible = state.activeMenu != ActiveCalcMenu.None,
        enter = fadeIn(),
        exit = fadeOut(),
      ) {
        LcdMenuOverlay(
          menu = state.activeMenu,
          isRegMode = state.mode == CalculatorMode.REG,
          screenBg = screenBg,
          inkColor = inkColor,
          inkMuted = inkMuted,
          onSelectOption = { opt -> onSelectMenuOption(state.activeMenu, opt) },
          onInsertToken = onInsertStatToken,
          onDismiss = onDismissMenu,
        )
      }
    }
  }
}

@Composable
private fun LcdBoxBadge(text: String, inkColor: Color, screenBg: Color) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(2.dp))
        .background(inkColor)
        .padding(horizontal = 4.dp, vertical = 0.5.dp)
  ) {
    Text(
      text = text,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 9.sp,
      fontWeight = FontWeight.Bold,
      color = screenBg,
    )
  }
}

@Composable
private fun LcdTextIndicator(text: String, inkColor: Color) {
  Text(
    text = text,
    fontFamily = JetBrainsMonoFontFamily,
    fontSize = 9.5.sp,
    fontWeight = FontWeight.Bold,
    color = inkColor,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LcdMenuOverlay(
  menu: ActiveCalcMenu,
  isRegMode: Boolean,
  screenBg: Color,
  inkColor: Color,
  inkMuted: Color,
  onSelectOption: (Int) -> Unit,
  onInsertToken: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(4.dp))
        .background(screenBg.copy(alpha = 0.99f))
        .padding(4.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val title =
        when (menu) {
          ActiveCalcMenu.ModeSelect -> "MODE"
          ActiveCalcMenu.RegTypeSelect -> "REG TYPE"
          ActiveCalcMenu.SetupSelect -> "SETUP"
          ActiveCalcMenu.FixDigitsSelect -> "FIX (0~9)?"
          ActiveCalcMenu.SciDigitsSelect -> "SCI (1~10)?"
          ActiveCalcMenu.NormSelect -> "NORM"
          ActiveCalcMenu.ClearSelect -> "CLR"
          ActiveCalcMenu.DrgSelect -> "DRG▸"
          ActiveCalcMenu.StatSumSelect -> "S-SUM"
          ActiveCalcMenu.StatVarSelect -> "S-VAR"
          ActiveCalcMenu.None -> ""
        }
      Text(
        text = title,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = inkColor,
      )
      Text(
        text = "AC",
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = inkMuted,
        modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 6.dp, vertical = 2.dp),
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    when (menu) {
      ActiveCalcMenu.ModeSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          MenuChip("1: COMP", inkColor) { onSelectOption(1) }
          MenuChip("2: SD", inkColor) { onSelectOption(2) }
          MenuChip("3: REG", inkColor) { onSelectOption(3) }
        }
      }
      ActiveCalcMenu.RegTypeSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          MenuChip("1: Lin", inkColor) { onSelectOption(1) }
          MenuChip("2: Log", inkColor) { onSelectOption(2) }
          MenuChip("3: Exp", inkColor) { onSelectOption(3) }
          MenuChip("4: Pwr", inkColor) { onSelectOption(4) }
          MenuChip("5: Inv", inkColor) { onSelectOption(5) }
          MenuChip("6: Quad", inkColor) { onSelectOption(6) }
        }
      }
      ActiveCalcMenu.SetupSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          MenuChip("1: Deg", inkColor) { onSelectOption(1) }
          MenuChip("2: Rad", inkColor) { onSelectOption(2) }
          MenuChip("3: Gra", inkColor) { onSelectOption(3) }
          MenuChip("4: Fix", inkColor) { onSelectOption(4) }
          MenuChip("5: Sci", inkColor) { onSelectOption(5) }
          MenuChip("6: Norm", inkColor) { onSelectOption(6) }
          MenuChip("7: ab/c↔d/c", inkColor) { onSelectOption(7) }
        }
      }
      ActiveCalcMenu.FixDigitsSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          for (d in 0..9) MenuChip("$d", inkColor) { onSelectOption(d) }
        }
      }
      ActiveCalcMenu.SciDigitsSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
          for (d in 1..9) MenuChip("$d", inkColor) { onSelectOption(d) }
          MenuChip("10", inkColor) { onSelectOption(0) }
        }
      }
      ActiveCalcMenu.NormSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MenuChip("1: Norm 1", inkColor) { onSelectOption(1) }
          MenuChip("2: Norm 2", inkColor) { onSelectOption(2) }
        }
      }
      ActiveCalcMenu.ClearSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          MenuChip("1: Mcl", inkColor) { onSelectOption(1) }
          MenuChip("2: Mode", inkColor) { onSelectOption(2) }
          MenuChip("3: All", inkColor) { onSelectOption(3) }
        }
      }
      ActiveCalcMenu.DrgSelect -> {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          MenuChip("1: D (°)", inkColor) { onSelectOption(1) }
          MenuChip("2: R (ʳ)", inkColor) { onSelectOption(2) }
          MenuChip("3: G (ᵍ)", inkColor) { onSelectOption(3) }
        }
      }
      ActiveCalcMenu.StatSumSelect -> {
        val itemsList =
          buildList {
            add("1: Σx²" to "Σx²")
            add("2: Σx" to "Σx")
            add("3: n" to "n")
            if (isRegMode) {
              add("4: Σy²" to "Σy²")
              add("5: Σy" to "Σy")
              add("6: Σxy" to "Σxy")
            }
          }
        LazyColumn(modifier = Modifier.height(64.dp)) {
          items(itemsList.chunked(3)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
              row.forEach { (label, tok) -> MenuChip(label, inkColor) { onInsertToken(tok) } }
            }
          }
        }
      }
      ActiveCalcMenu.StatVarSelect -> {
        val itemsList =
          buildList {
            add("1: x̄" to "x̄")
            add("2: xσn" to "xσn")
            add("3: xσn-1" to "xσn-1")
            if (isRegMode) {
              add("4: ȳ" to "ȳ")
              add("5: yσn" to "yσn")
              add("6: yσn-1" to "yσn-1")
              add("RegA" to "RegA")
              add("RegB" to "RegB")
              add("RegC" to "RegC")
              add("7: r" to "r")
              add("8: ŷ(" to "ŷ(")
              add("9: x̂(" to "x̂(")
            }
          }
        LazyColumn(modifier = Modifier.height(68.dp)) {
          items(itemsList.chunked(3)) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
              row.forEach { (label, tok) -> MenuChip(label, inkColor) { onInsertToken(tok) } }
            }
          }
        }
      }
      ActiveCalcMenu.None -> Unit
    }
  }
}

@Composable
private fun MenuChip(label: String, inkColor: Color, onClick: () -> Unit) {
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(4.dp))
        .border(1.dp, inkColor.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
        .background(inkColor.copy(alpha = 0.08f))
        .clickable(onClick = onClick)
        .padding(horizontal = 7.dp, vertical = 4.dp)
  ) {
    Text(
      text = label,
      fontFamily = JetBrainsMonoFontFamily,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold,
      color = inkColor,
    )
  }
}
