package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CalculatorMode
import com.example.ui.theme.AlphaCoral
import com.example.ui.theme.HypCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.KeyDangerBg
import com.example.ui.theme.KeyDangerFg
import com.example.ui.theme.KeyEqualsBg
import com.example.ui.theme.KeyEqualsFg
import com.example.ui.theme.KeyFuncBg
import com.example.ui.theme.KeyFuncBorder
import com.example.ui.theme.KeyFuncFg
import com.example.ui.theme.KeyFuncTop
import com.example.ui.theme.KeyNumberBg
import com.example.ui.theme.KeyNumberBorder
import com.example.ui.theme.KeyNumberFg
import com.example.ui.theme.KeyNumberTop
import com.example.ui.theme.KeyOperatorBg
import com.example.ui.theme.KeyOperatorFg
import com.example.ui.theme.KeyRoundBg
import com.example.ui.theme.KeyRoundBorder
import com.example.ui.theme.KeyRoundTop
import com.example.ui.theme.ReplayPadBg
import com.example.ui.theme.ReplayPadRim
import com.example.ui.theme.ReplayPadTop
import com.example.ui.theme.ShiftAmber
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.StatCyan
import com.example.viewmodel.CalcKey
import com.example.viewmodel.CalculatorUiState

private data class SciKeySpec(
  val key: CalcKey,
  val primary: String,
  val shiftLabel: String? = null,
  val alphaLabel: String? = null,
  val bottomCyanLeft: String? = null,
  val bottomYellowRight: String? = null,
  val testTag: String,
)

@Composable
fun CalculatorKeypad(
  state: CalculatorUiState,
  onKey: (CalcKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val isStatMode = state.mode != CalculatorMode.COMP

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(5.dp),
  ) {
    // Upper Hardware Cluster matching fx-82MS 2nd Edition image 1:1
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Left 2 columns flanking REPLAY
      Column(
        modifier = Modifier.weight(2f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RoundControlButton(
            topLabel = "SHIFT",
            topColor = ShiftAmber,
            active = state.isShift,
            activeGlow = ShiftAmber,
            onClick = { onKey(CalcKey.SHIFT) },
            testTag = "key_shift",
          )
          RoundControlButton(
            topLabel = "ALPHA",
            topColor = AlphaCoral,
            active = state.isAlpha,
            activeGlow = AlphaCoral,
            onClick = { onKey(CalcKey.ALPHA) },
            testTag = "key_alpha",
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          SciKeyButton(
            spec = SciKeySpec(CalcKey.X_INV, "x⁻¹", "x!", testTag = "key_x_inv"),
            state = state,
            onClick = { onKey(CalcKey.X_INV) },
            modifier = Modifier.weight(1f),
            keyHeight = 38.dp,
          )
          SciKeyButton(
            spec = SciKeySpec(CalcKey.NCR, "nCr", "nPr", testTag = "key_ncr"),
            state = state,
            onClick = { onKey(CalcKey.NCR) },
            modifier = Modifier.weight(1f),
            keyHeight = 38.dp,
          )
        }
      }

      // Center Sculpted 4-Way REPLAY D-Pad
      ReplayDPad(
        onUp = { onKey(CalcKey.UP) },
        onDown = { onKey(CalcKey.DOWN) },
        onLeft = { onKey(CalcKey.LEFT) },
        onRight = { onKey(CalcKey.RIGHT) },
        modifier = Modifier.width(102.dp),
      )

      // Right 2 columns flanking REPLAY
      Column(
        modifier = Modifier.weight(2f),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          RoundControlButton(
            topLabel = "MODE",
            topSecondaryLabel = "CLR",
            topColor = Color(0xFFF0F2F4),
            topSecondaryColor = ShiftAmber,
            active = false,
            activeGlow = ShiftAmber,
            onClick = { onKey(CalcKey.MODE) },
            testTag = "key_mode",
          )
          RoundControlButton(
            topLabel = "ON",
            topColor = Color(0xFFF0F2F4),
            active = false,
            activeGlow = Color.White,
            onClick = { onKey(CalcKey.ON_AC) },
            testTag = "key_on",
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
          SciKeyButton(
            spec = SciKeySpec(CalcKey.POL, "Pol(", "Rec(", ":", testTag = "key_pol"),
            state = state,
            onClick = { onKey(CalcKey.POL) },
            modifier = Modifier.weight(1f),
            keyHeight = 38.dp,
          )
          SciKeyButton(
            spec = SciKeySpec(CalcKey.X_CUBE, "x³", "∛", testTag = "key_x_cube"),
            state = state,
            onClick = { onKey(CalcKey.X_CUBE) },
            modifier = Modifier.weight(1f),
            keyHeight = 38.dp,
          )
        }
      }
    }

    // 3 Rows × 6 Columns Scientific Keypad matching reference photo 1:1
    val sciRows =
      listOf(
        listOf(
          SciKeySpec(CalcKey.FRACTION, "a b/c", "d/c", testTag = "key_fraction"),
          SciKeySpec(CalcKey.SQRT, "√", null, testTag = "key_sqrt"),
          SciKeySpec(CalcKey.X_SQ, "x²", null, testTag = "key_x_sq"),
          SciKeySpec(CalcKey.POW, "^", "ˣ√", testTag = "key_pow"),
          SciKeySpec(CalcKey.LOG, "log", "10ˣ", testTag = "key_log"),
          SciKeySpec(CalcKey.LN, "ln", "eˣ", "e", testTag = "key_ln"),
        ),
        listOf(
          SciKeySpec(CalcKey.NEG, "(−)", null, "A", testTag = "key_neg"),
          SciKeySpec(CalcKey.DMS, "° ' \"", "←", "B", testTag = "key_dms"),
          SciKeySpec(CalcKey.HYP, "hyp", null, "C", testTag = "key_hyp"),
          SciKeySpec(
            CalcKey.SIN,
            if (state.isHyp) "sinh" else "sin",
            if (state.isHyp) "sinh⁻¹" else "sin⁻¹",
            "D",
            testTag = "key_sin",
          ),
          SciKeySpec(
            CalcKey.COS,
            if (state.isHyp) "cosh" else "cos",
            if (state.isHyp) "cosh⁻¹" else "cos⁻¹",
            "E",
            testTag = "key_cos",
          ),
          SciKeySpec(
            CalcKey.TAN,
            if (state.isHyp) "tanh" else "tan",
            if (state.isHyp) "tanh⁻¹" else "tan⁻¹",
            "F",
            testTag = "key_tan",
          ),
        ),
        listOf(
          SciKeySpec(CalcKey.RCL, "RCL", "STO", testTag = "key_rcl"),
          SciKeySpec(CalcKey.ENG, "ENG", "←", testTag = "key_eng"),
          SciKeySpec(CalcKey.LPAREN, "(", null, testTag = "key_lparen"),
          SciKeySpec(CalcKey.RPAREN, ")", null, "X", testTag = "key_rparen"),
          SciKeySpec(CalcKey.COMMA, ",", ";", "Y", testTag = "key_comma"),
          SciKeySpec(
            CalcKey.M_PLUS,
            if (isStatMode) "DT" else "M+",
            "M−",
            "M",
            bottomCyanLeft = "DT",
            bottomYellowRight = "CL",
            testTag = "key_m_plus",
          ),
        ),
      )

    sciRows.forEach { rowSpecs ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        rowSpecs.forEach { spec ->
          SciKeyButton(
            spec = spec,
            state = state,
            onClick = { onKey(spec.key) },
            modifier = Modifier.weight(1f),
            keyHeight = 38.dp,
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(3.dp))

    // Main 4×5 Numeric & Arithmetic Grid matching reference photo 1:1
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      NumberKeyButton("7", null, null, state, { onKey(CalcKey.D7) }, "key_7", Modifier.weight(1f))
      NumberKeyButton("8", null, null, state, { onKey(CalcKey.D8) }, "key_8", Modifier.weight(1f))
      NumberKeyButton("9", null, null, state, { onKey(CalcKey.D9) }, "key_9", Modifier.weight(1f))
      ActionKeyButton(
        primary = "DEL",
        shiftLabel = "INS",
        bg = KeyDangerBg,
        fg = KeyDangerFg,
        state = state,
        onClick = { onKey(CalcKey.DEL) },
        testTag = "key_del",
        fontSize = 15.sp,
        modifier = Modifier.weight(1f),
      )
      ActionKeyButton(
        primary = "AC",
        shiftLabel = "OFF",
        shiftColor = Color(0xFFD0D3D6),
        bg = KeyDangerBg,
        fg = KeyDangerFg,
        state = state,
        onClick = { onKey(CalcKey.ON_AC) },
        testTag = "key_ac",
        fontSize = 15.sp,
        modifier = Modifier.weight(1f),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      NumberKeyButton("4", null, null, state, { onKey(CalcKey.D4) }, "key_4", Modifier.weight(1f))
      NumberKeyButton("5", null, null, state, { onKey(CalcKey.D5) }, "key_5", Modifier.weight(1f))
      NumberKeyButton("6", null, null, state, { onKey(CalcKey.D6) }, "key_6", Modifier.weight(1f))
      ActionKeyButton(
        primary = "×",
        shiftLabel = null,
        bg = KeyOperatorBg,
        fg = KeyOperatorFg,
        state = state,
        onClick = { onKey(CalcKey.MUL) },
        testTag = "key_mul",
        fontSize = 20.sp,
        modifier = Modifier.weight(1f),
      )
      ActionKeyButton(
        primary = "÷",
        shiftLabel = null,
        bg = KeyOperatorBg,
        fg = KeyOperatorFg,
        state = state,
        onClick = { onKey(CalcKey.DIV) },
        testTag = "key_div",
        fontSize = 20.sp,
        modifier = Modifier.weight(1f),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      NumberKeyButton("1", "S-SUM", ShiftAmber, state, { onKey(CalcKey.D1) }, "key_1", Modifier.weight(1f))
      NumberKeyButton("2", "S-VAR", ShiftAmber, state, { onKey(CalcKey.D2) }, "key_2", Modifier.weight(1f))
      NumberKeyButton(
        "3",
        if (isStatMode) "DATA" else null,
        StatCyan,
        state,
        { onKey(CalcKey.D3) },
        "key_3",
        Modifier.weight(1f),
      )
      ActionKeyButton(
        primary = "+",
        shiftLabel = null,
        bg = KeyOperatorBg,
        fg = KeyOperatorFg,
        state = state,
        onClick = { onKey(CalcKey.PLUS) },
        testTag = "key_plus",
        fontSize = 21.sp,
        modifier = Modifier.weight(1f),
      )
      ActionKeyButton(
        primary = "−",
        shiftLabel = null,
        bg = KeyOperatorBg,
        fg = KeyOperatorFg,
        state = state,
        onClick = { onKey(CalcKey.MINUS) },
        testTag = "key_minus",
        fontSize = 21.sp,
        modifier = Modifier.weight(1f),
      )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      NumberKeyButton("0", "Rnd", ShiftAmber, state, { onKey(CalcKey.D0) }, "key_0", Modifier.weight(1f))
      NumberKeyButton(".", "Ran#", ShiftAmber, state, { onKey(CalcKey.DOT) }, "key_dot", Modifier.weight(1f))
      NumberKeyButton("×10ˣ", "π", ShiftAmber, state, { onKey(CalcKey.EXP) }, "key_exp", Modifier.weight(1f), primarySize = 14.sp)
      NumberKeyButton("Ans", "DRG▸", ShiftAmber, state, { onKey(CalcKey.ANS) }, "key_ans", Modifier.weight(1f), primarySize = 14.sp)
      ActionKeyButton(
        primary = "=",
        shiftLabel = "%",
        bg = KeyEqualsBg,
        fg = KeyEqualsFg,
        state = state,
        onClick = { onKey(CalcKey.EQUALS) },
        testTag = "key_equals",
        fontSize = 21.sp,
        modifier = Modifier.weight(1f),
      )
    }
  }
}

@Composable
private fun RoundControlButton(
  topLabel: String,
  topColor: Color,
  active: Boolean,
  activeGlow: Color,
  onClick: () -> Unit,
  testTag: String,
  topSecondaryLabel: String? = null,
  topSecondaryColor: Color = ShiftAmber,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(targetValue = if (pressed) 0.92f else 1f, animationSpec = spring(), label = "roundScale")

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      Text(
        text = topLabel,
        fontFamily = SpaceGroteskFontFamily,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = if (active) activeGlow else topColor,
      )
      if (topSecondaryLabel != null) {
        Text(
          text = topSecondaryLabel,
          fontFamily = SpaceGroteskFontFamily,
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = topSecondaryColor,
        )
      }
    }
    Box(
      modifier =
        Modifier.scale(scale)
          .size(30.dp)
          .clip(CircleShape)
          .background(
            Brush.radialGradient(
              colors = listOf(KeyRoundTop, KeyRoundBg)
            )
          )
          .border(
            width = if (active) 2.dp else 1.2.dp,
            color = if (active) activeGlow else KeyRoundBorder,
            shape = CircleShape,
          )
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = activeGlow),
            onClick = onClick,
          )
          .testTag(testTag)
          .semantics { contentDescription = topLabel },
    )
  }
}

@Composable
private fun ReplayDPad(
  onUp: () -> Unit,
  onDown: () -> Unit,
  onLeft: () -> Unit,
  onRight: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .height(82.dp)
        .clip(RoundedCornerShape(34.dp))
        .background(
          Brush.radialGradient(
            colors = listOf(ReplayPadTop, ReplayPadBg)
          )
        )
        .border(1.5.dp, ReplayPadRim, RoundedCornerShape(34.dp)),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier =
        Modifier.size(22.dp)
          .clip(CircleShape)
          .background(Color(0xFF141517))
          .border(1.dp, Color(0xFF3E4145), CircleShape)
    )

    Box(
      modifier =
        Modifier.align(Alignment.TopCenter)
          .size(width = 44.dp, height = 28.dp)
          .clip(CircleShape)
          .clickable(onClick = onUp)
          .testTag("key_replay_up"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = "Replay Older",
        tint = Color(0xFFDCDFE2),
        modifier = Modifier.size(18.dp),
      )
    }
    Box(
      modifier =
        Modifier.align(Alignment.BottomCenter)
          .size(width = 44.dp, height = 28.dp)
          .clip(CircleShape)
          .clickable(onClick = onDown)
          .testTag("key_replay_down"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = "Replay Newer",
        tint = Color(0xFFDCDFE2),
        modifier = Modifier.size(18.dp),
      )
    }
    Box(
      modifier =
        Modifier.align(Alignment.CenterStart)
          .size(width = 32.dp, height = 42.dp)
          .clip(CircleShape)
          .clickable(onClick = onLeft)
          .testTag("key_replay_left"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
        contentDescription = "Cursor Left",
        tint = Color(0xFFDCDFE2),
        modifier = Modifier.size(18.dp),
      )
    }
    Box(
      modifier =
        Modifier.align(Alignment.CenterEnd)
          .size(width = 32.dp, height = 42.dp)
          .clip(CircleShape)
          .clickable(onClick = onRight)
          .testTag("key_replay_right"),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = "Cursor Right",
        tint = Color(0xFFDCDFE2),
        modifier = Modifier.size(18.dp),
      )
    }
  }
}

@Composable
private fun SciKeyButton(
  spec: SciKeySpec,
  state: CalculatorUiState,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  keyHeight: Dp = 38.dp,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = spring(), label = "sciScale")

  val isVariableKey = spec.alphaLabel != null
  val highlightAlpha = (state.isAlpha || state.isSto || state.isRcl) && isVariableKey
  val highlightShift = state.isShift && spec.shiftLabel != null
  val highlightHyp = state.isHyp && spec.key in listOf(CalcKey.SIN, CalcKey.COS, CalcKey.TAN)

  val borderColor =
    when {
      highlightShift -> ShiftAmber
      highlightAlpha -> AlphaCoral
      highlightHyp -> HypCyan
      else -> KeyFuncBorder
    }

  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Text(
        text = spec.shiftLabel ?: "",
        fontFamily = SpaceGroteskFontFamily,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = ShiftAmber,
        maxLines = 1,
        overflow = TextOverflow.Clip,
      )
      Text(
        text = spec.alphaLabel ?: "",
        fontFamily = SpaceGroteskFontFamily,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = AlphaCoral,
        maxLines = 1,
      )
    }

    Box(
      modifier =
        Modifier.fillMaxWidth()
          .scale(scale)
          .height(keyHeight - 9.dp)
          .clip(RoundedCornerShape(6.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(KeyFuncTop, KeyFuncBg)
            )
          )
          .border(1.dp, borderColor, RoundedCornerShape(6.dp))
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.White),
            onClick = onClick,
          )
          .testTag(spec.testTag)
          .semantics {
            contentDescription = buildString {
              append(spec.primary)
              spec.shiftLabel?.let { append(", Shift: $it") }
              spec.alphaLabel?.let { append(", Alpha: $it") }
            }
          },
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = spec.primary,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = if (spec.primary.length > 4) 10.sp else 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (highlightHyp) HypCyan else KeyFuncFg,
        textAlign = TextAlign.Center,
        maxLines = 1,
      )
    }

    if (spec.bottomCyanLeft != null || spec.bottomYellowRight != null) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 3.dp).offset(y = 1.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(spec.bottomCyanLeft ?: "", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = StatCyan)
        Text(spec.bottomYellowRight ?: "", fontSize = 7.5.sp, fontWeight = FontWeight.Bold, color = ShiftAmber)
      }
    }
  }
}

@Composable
private fun NumberKeyButton(
  primary: String,
  shiftLabel: String?,
  shiftColor: Color?,
  state: CalculatorUiState,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  primarySize: TextUnit = 19.sp,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = spring(), label = "numScale")

  val highlightShift = state.isShift && shiftLabel != null

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    if (shiftLabel != null) {
      Text(
        text = shiftLabel,
        fontFamily = SpaceGroteskFontFamily,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = shiftColor ?: ShiftAmber,
        maxLines = 1,
      )
    } else {
      Spacer(modifier = Modifier.height(10.dp))
    }

    Box(
      modifier =
        Modifier.fillMaxWidth()
          .scale(scale)
          .height(44.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(KeyNumberTop, KeyNumberBg)
            )
          )
          .border(
            width = 1.dp,
            color = if (highlightShift) (shiftColor ?: ShiftAmber) else KeyNumberBorder,
            shape = RoundedCornerShape(8.dp),
          )
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.White),
            onClick = onClick,
          )
          .testTag(testTag)
          .semantics { contentDescription = primary },
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = primary,
        fontFamily = JetBrainsMonoFontFamily,
        fontSize = primarySize,
        fontWeight = FontWeight.SemiBold,
        color = KeyNumberFg,
      )
    }
  }
}

@Composable
private fun ActionKeyButton(
  primary: String,
  shiftLabel: String?,
  bg: Color,
  fg: Color,
  state: CalculatorUiState,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
  shiftColor: Color = ShiftAmber,
  fontSize: TextUnit = 16.sp,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(targetValue = if (pressed) 0.94f else 1f, animationSpec = spring(), label = "actScale")

  val highlightShift = state.isShift && shiftLabel != null

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    if (shiftLabel != null) {
      Text(
        text = shiftLabel,
        fontFamily = SpaceGroteskFontFamily,
        fontSize = 8.5.sp,
        fontWeight = FontWeight.Bold,
        color = shiftColor,
        maxLines = 1,
      )
    } else {
      Spacer(modifier = Modifier.height(10.dp))
    }

    Box(
      modifier =
        Modifier.fillMaxWidth()
          .scale(scale)
          .height(44.dp)
          .clip(RoundedCornerShape(8.dp))
          .background(
            Brush.verticalGradient(
              colors = listOf(KeyNumberTop, bg)
            )
          )
          .border(
            width = 1.dp,
            color = if (highlightShift) shiftColor else KeyNumberBorder,
            shape = RoundedCornerShape(8.dp),
          )
          .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.White),
            onClick = onClick,
          )
          .testTag(testTag)
          .semantics { contentDescription = primary },
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = primary,
        fontFamily = SpaceGroteskFontFamily,
        fontSize = fontSize,
        fontWeight = FontWeight.Bold,
        color = fg,
      )
    }
  }
}
