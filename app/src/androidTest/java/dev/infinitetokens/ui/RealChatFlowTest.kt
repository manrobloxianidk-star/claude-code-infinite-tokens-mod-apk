package dev.infinitetokens.ui

import android.graphics.Bitmap
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.infinitetokens.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RealChatFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun realActivitySendsAndRestoresConversation() {
        saveScreenshot("welcome-screen.png")
        rule.onNode(hasSetTextAction()).performTextInput("Say hello in one sentence.")
        rule.waitUntil(240_000) {
            rule.onAllNodes(hasContentDescription("Send message") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Send message").performClick()
        rule.waitUntil(240_000) {
            rule.onAllNodesWithTag("assistant-message").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Say hello in one sentence.").assertExists()
        rule.onNodeWithContentDescription("Stop response").let {
            if (rule.onAllNodesWithContentDescription("Stop response").fetchSemanticsNodes().isNotEmpty()) it.performClick()
        }
        rule.waitForIdle()
        saveScreenshot("chat-screen.png")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(30_000) {
            rule.onAllNodesWithText("Say hello in one sentence.").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNode(hasSetTextAction()).assertIsDisplayed()
    }
    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        rule.waitForIdle()
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        File(instrumentation.targetContext.filesDir, name).outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
