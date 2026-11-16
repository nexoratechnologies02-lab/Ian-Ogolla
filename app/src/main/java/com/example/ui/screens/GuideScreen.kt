package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.example.ui.theme.JetBrainsMonoFontFamily

private enum class GuideSection(val title: String) {
  MODIFIERS("SHIFT & ALPHA"),
  BUTTONS("Every Button"),
  MODES("Modes & Stats"),
  EXAMPLES("Examples"),
}

private data class ButtonDoc(
  val key: String,
  val shift: String,
  val alpha: String,
  val description: String,
)

private data class ExampleDoc(
  val title: String,
  val expression: String,
  val expected: String,
  val note: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(
  onBack: () -> Unit,
  onTryExample: (String) -> Unit,
) {
  var selectedSection by remember { mutableStateOf(GuideSection.MODIFIERS) }

  val buttonDocs = remember {
    listOf(
      ButtonDoc("SHIFT", "—", "—", "Arms amber secondary functions printed above keys (sin⁻¹, 10ˣ, x!, nPr, STO, CLR, etc.)."),
      ButtonDoc("ALPHA", "—", "—", "Arms coral variable letters A, B, C, D, E, F, X, Y, M and Euler's number e."),
      ButtonDoc("REPLAY ▲▼◀▶", "—", "—", "◀ ▶ move cursor left/right to edit expressions; ▲ ▼ recall previous calculation history."),
      ButtonDoc("MODE", "CLR", "—", "Press once for COMP / SD / REG mode menu; press twice for SETUP (Deg/Rad/Gra, Fix, Sci, Norm). SHIFT+MODE opens CLR menu."),
      ButtonDoc("x⁻¹", "x!", "—", "Reciprocal (1/x). SHIFT executes factorial (n! up to 69!)."),
      ButtonDoc("nCr", "nPr", "—", "Combinations (nCr) and Permutations (SHIFT: nPr). Example: 10 nCr 2 = 45."),
      ButtonDoc("Pol(", "Rec(", "—", "Polar/Rectangular coordinate conversion. Stores results in E & F and displays both values."),
      ButtonDoc("x³ / x²", "∛ / √", "—", "Cube, square, cube root (∛), and square root (√)."),
      ButtonDoc("a b/c", "d/c", "—", "Enters mixed/simple fractions (1⌟2) and toggles decimal ↔ fraction. SHIFT toggles mixed ↔ improper."),
      ButtonDoc("^", "ˣ√", "—", "General power xʸ and general root ˣ√y (e.g., 4ˣ√16 = 2)."),
      ButtonDoc("log / ln", "10ˣ / eˣ", "—", "Base-10 log & natural log (ln); SHIFT computes 10ˣ and eˣ."),
      ButtonDoc("(−)", "—", "A", "Negation sign (−). ALPHA inserts variable A."),
      ButtonDoc("° ' \"", "←DMS", "B", "Enters Degrees-Minutes-Seconds (12°30°45°) or toggles result between decimal and D°M′S″."),
      ButtonDoc("hyp", "—", "C", "Arms hyperbolic mode so sin/cos/tan compute sinh/cosh/tanh (combine with SHIFT for sinh⁻¹/cosh⁻¹/tanh⁻¹)."),
      ButtonDoc("sin / cos / tan", "sin⁻¹ / cos⁻¹ / tan⁻¹", "D / E / F", "Trig & inverse trig using active DEG/RAD/GRA angle unit. ALPHA inserts D, E, or F."),
      ButtonDoc("RCL", "STO", "—", "Recall variable value (RCL + A..M). SHIFT+RCL (STO) stores current result into A..M."),
      ButtonDoc("ENG", "←", "—", "Converts result to engineering notation (exponent multiple of 3) and shifts exponent step by ±3."),
      ButtonDoc("( / ) / ,", "Abs / ;", "X / Y", "Brackets, Abs(x), argument separator, frequency separator (;), and ALPHA variables X, Y."),
      ButtonDoc("M+ / DT", "M−", "M", "Adds to independent memory M (SHIFT subtracts). In SD/REG mode, enters statistical data (DT)."),
      ButtonDoc("0 / . / EXP", "Rnd / Ran# / π", "e", "Digits, rounding (Rnd), random 0.000–0.999 (Ran#), scientific notation ×10ˣ (EXP), π, and e."),
      ButtonDoc("1 / 2 / 3", "S-SUM / S-VAR / DATA", "—", "SHIFT+1 inserts Σx², Σx, n, Σy, Σxy. SHIFT+2 inserts x̄, xσn, xσn-1, A, B, C, r, x̂, ŷ. SHIFT+3 opens Stat Data Editor."),
      ButtonDoc("Ans / =", "DRG▸ / %", "—", "Recalls last answer (Ans), converts angle unit suffix °/ʳ/ᵍ (DRG▸), evaluates (=), or inserts % (SHIFT+=)."),
    )
  }

  val examples = remember {
    listOf(
      ExampleDoc("Basic Precedence", "25+35", "60", "Standard arithmetic with instant Replay"),
      ExampleDoc("Square Root & Powers", "√(144)+5²−2³", "29", "Combines √, x², and x³"),
      ExampleDoc("Fractions", "1⌟2+1⌟4", "3⌟4", "Exact fraction arithmetic; press a b/c to toggle decimal"),
      ExampleDoc("Trigonometry (DEG)", "sin(30)+cos(60)+tan(45)", "2", "0.5 + 0.5 + 1 in DEG mode"),
      ExampleDoc("Logarithms", "log(100)+ln(e)", "3", "2 + 1 = 3"),
      ExampleDoc("Combinatorics & Factorial", "10nCr2+10nPr2+5!", "255", "45 + 90 + 120 = 255"),
      ExampleDoc("Polar Conversion", "Pol(3,4)", "r=5, θ=53.13°", "Also stores r in E and θ in F"),
      ExampleDoc("DMS Angle Entry", "12°30°0°", "12.5", "Converts 12°30′0″ to decimal degrees; press ° ' \" on result to toggle back"),
    )
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Calculator Guide") },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("guide_back")) {
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
      Column(modifier = Modifier.widthIn(max = 700.dp).fillMaxSize()) {
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          items(GuideSection.entries) { section ->
            FilterChip(
              selected = selectedSection == section,
              onClick = { selectedSection = section },
              label = { Text(section.title) },
            )
          }
        }

        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          when (selectedSection) {
            GuideSection.MODIFIERS -> {
              item {
                GuideCard(
                  title = "Using SHIFT (Amber Functions)",
                  body = "Tap SHIFT once so the [S] indicator appears on the LCD, then tap any key to invoke its amber secondary function (e.g., SHIFT + sin = sin⁻¹, SHIFT + x⁻¹ = x!, SHIFT + RCL = STO, SHIFT + = = %).",
                )
              }
              item {
                GuideCard(
                  title = "Using ALPHA & Memory Variables",
                  body = "Tap ALPHA so the [A] indicator appears, then tap (−) for A, ° ' \" for B, hyp for C, sin for D, cos for E, tan for F, ) for X, , for Y, M+ for M, or EXP for e. Store results with SHIFT+RCL (STO) + variable key, and inspect anytime with RCL + variable key.",
                )
              }
              item {
                GuideCard(
                  title = "Hyperbolic (hyp) & Angle Overrides (DRG▸)",
                  body = "Press hyp before sin/cos/tan (or with SHIFT) for sinh/cosh/tanh and sinh⁻¹/cosh⁻¹/tanh⁻¹. Press SHIFT + Ans (DRG▸) to append explicit °, ʳ, or ᵍ units inside expressions regardless of global angle mode.",
                )
              }
            }

            GuideSection.BUTTONS -> {
              items(buttonDocs) { doc ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                      Text(doc.key, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                      Text("SHIFT: ${doc.shift}  •  ALPHA: ${doc.alpha}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(doc.description, style = MaterialTheme.typography.bodyMedium)
                  }
                }
              }
            }

            GuideSection.MODES -> {
              item {
                GuideCard(
                  title = "1. COMP (General Calculations)",
                  body = "Default scientific mode supporting arithmetic, fractions, powers, roots, trigonometry, hyperbolic functions, logarithms, combinatorics, DMS, Pol/Rec coordinates, and 9 memory variables.",
                )
              }
              item {
                GuideCard(
                  title = "2. SD (Standard Deviation Statistics)",
                  body = "Switch via MODE -> 2. Enter a value (or value;freq) and press M+ (DT) to record observations, or open the Data sheet from the top bar. Recall n, Σx, Σx² via SHIFT+1 (S-SUM), and x̄, xσn, xσn-1 via SHIFT+2 (S-VAR).",
                )
              }
              item {
                GuideCard(
                  title = "3. REG (6 Regression Models)",
                  body = "Switch via MODE -> 3 and choose Lin, Log, Exp, Pwr, Inv, or Quad. Enter x,y pairs (with optional ;freq) and press M+ (DT), or manage points in the Data sheet. Query regression coefficients RegA, RegB, RegC, correlation r, and predictions ŷ(x) or x̂(y) via SHIFT+2 (S-VAR).",
                )
              }
            }

            GuideSection.EXAMPLES -> {
              items(examples) { ex ->
                Card(
                  colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                  shape = RoundedCornerShape(12.dp),
                ) {
                  Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(ex.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "${ex.expression}  =  ${ex.expected}",
                      fontFamily = JetBrainsMonoFontFamily,
                      fontWeight = FontWeight.Bold,
                      fontSize = 14.sp,
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(ex.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = { onTryExample(ex.expression) }) {
                      Icon(Icons.Default.PlayArrow, contentDescription = null)
                      Text("Try in Calculator")
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GuideCard(title: String, body: String) {
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
