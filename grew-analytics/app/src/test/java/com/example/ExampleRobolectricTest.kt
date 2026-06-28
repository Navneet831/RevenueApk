package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToLog
import androidx.test.core.app.ApplicationProvider
import com.example.ui.DashboardScreen
import com.example.ui.GrewViewModel
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
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Grew Analytics", appName)
  }

  @Test
  fun test_dashboard_render_when_logged_in() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("grew_auth", Context.MODE_PRIVATE)
    sharedPrefs.edit().putString("grew_email", "navneet.chaudhary831@gmail.com").commit()

    val viewModel = GrewViewModel()
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
  }

  @Test
  fun test_dashboard_login_flow() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sharedPrefs = context.getSharedPreferences("grew_auth", Context.MODE_PRIVATE)
    sharedPrefs.edit().remove("grew_email").commit()

    val viewModel = GrewViewModel()
    composeTestRule.setContent {
      MyApplicationTheme {
        DashboardScreen(viewModel = viewModel)
      }
    }
    composeTestRule.waitForIdle()
    
    // Tap the Google Sign-In button
    composeTestRule.onNodeWithTag("submit_button").performClick()
    composeTestRule.waitForIdle()

    // Assert that the account chooser displays properly
    composeTestRule.onNodeWithText("Choose an account").assertExists()
  }
}
