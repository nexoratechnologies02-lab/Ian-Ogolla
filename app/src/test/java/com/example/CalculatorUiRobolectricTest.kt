package com.example

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class CalculatorUiRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  private fun tapKey(tag: String) {
    composeTestRule.onNodeWithTag(tag).performScrollTo().performClick()
  }

  @Test
  fun `calculate 25 plus 35 equals 60 via keypad`() {
    composeTestRule.waitForIdle()
    tapKey("key_2")
    tapKey("key_5")
    tapKey("key_plus")
    tapKey("key_3")
    tapKey("key_5")
    tapKey("key_equals")
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("lcd_result").assertTextEquals("60")
  }

  @Test
  fun `SHIFT factorial 5 equals 120 and ALPHA variable STO RCL flow`() {
    composeTestRule.waitForIdle()
    // 5! = 120
    tapKey("key_5")
    tapKey("key_shift")
    tapKey("key_x_inv")
    tapKey("key_equals")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lcd_result").assertTextEquals("120")

    // Store 120 into variable A: SHIFT -> RCL (STO) -> (-) (A)
    tapKey("key_shift")
    tapKey("key_rcl")
    tapKey("key_neg")
    composeTestRule.waitForIdle()

    // AC then ALPHA + A + + + 8 + = -> 128
    tapKey("key_ac")
    tapKey("key_alpha")
    tapKey("key_neg")
    tapKey("key_plus")
    tapKey("key_8")
    tapKey("key_equals")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lcd_result").assertTextEquals("128")
  }

  @Test
  fun `REPLAY up recalls previous calculation and left edits it`() {
    composeTestRule.waitForIdle()
    // 2 * 5 = 10
    tapKey("key_2")
    tapKey("key_mul")
    tapKey("key_5")
    tapKey("key_equals")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lcd_result").assertTextEquals("10")

    // AC, then REPLAY UP, DEL, 6, = -> 2 * 6 = 12
    tapKey("key_ac")
    tapKey("key_replay_up")
    tapKey("key_del")
    tapKey("key_6")
    tapKey("key_equals")
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("lcd_result").assertTextEquals("12")
  }
}
