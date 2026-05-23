package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w1080dp-h1920dp", sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BillBuddy", appName)
  }

  @Test
  fun testMainAppFlowExecution() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.state.UserProfileManager.initialize(context)
    composeTestRule.setContent {
      MyApplicationTheme {
        MainAppFlow()
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun testFallbackParser_IndianRupeesShorthand() {
    val sms = "Your AC X1234 has been debited by Rs. 500.50 on 22-May-26 sent to Starbucks."
    val result = com.example.service.GeminiParser.parseSMSLocally(sms)
    assertEquals(500.50, result.amount, 0.001)
    assertEquals("Starbucks", result.merchant)
  }

  @Test
  fun testFallbackParser_UpiSymbol() {
    val sms = "Paid ₹120 to Amazon India for retail order."
    val result = com.example.service.GeminiParser.parseSMSLocally(sms)
    assertEquals(120.0, result.amount, 0.001)
    assertEquals("Amazon India", result.merchant)
  }

  @Test
  fun testFallbackParser_InrText() {
    val sms = "Transfer of INR 25.00 to rajesh successful."
    val result = com.example.service.GeminiParser.parseSMSLocally(sms)
    assertEquals(25.0, result.amount, 0.001)
    assertEquals("rajesh", result.merchant)
  }
}

