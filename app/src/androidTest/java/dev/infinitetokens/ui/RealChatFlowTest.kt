package dev.infinitetokens.ui

import android.graphics.Bitmap
import java.io.File
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
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
        val vm = ViewModelProvider(rule.activity)[ChatViewModel::class.java]
        saveScreenshot("welcome-screen.png")
        rule.onNode(hasSetTextAction()).performTextInput("Say hello in one sentence.")
        rule.waitUntil(240_000) {
            rule.onAllNodes(hasContentDescription("Send message") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Send message").performClick()
        check(vm.state.value.isGenerating || vm.state.value.messages.any { it.role == "user" }) { "Send action did not start a request" }
        try {
            rule.waitUntil(240_000) {
                val state = vm.state.value
                check(state.generationError == null) { "Generation error: ${state.generationError}" }
                check(state.modelError == null) { "Startup error: ${state.modelError}" }
                check(state.isGenerating || state.messages.none { it.role == "user" } || state.streamingResponse.isNotEmpty() || state.messages.any { it.role == "assistant" }) { "Generation finished with an empty response" }
                state.streamingResponse.isNotBlank() || state.messages.any { it.role == "assistant" }
            }
        } catch (failure: Throwable) {
            saveScreenshot("failure-screen.png")
            println(rule.onRoot(useUnmergedTree = true).printToString())
            throw failure
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
