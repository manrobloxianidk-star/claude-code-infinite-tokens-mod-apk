package dev.infinitetokens.ui

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import dev.infinitetokens.model.AppSection
import androidx.core.view.WindowInsetsCompat
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
        rule.waitUntil(30_000) { vm.state.value.activeChat != null }
        val previousChatId = vm.state.value.activeChat?.id
        rule.runOnIdle { vm.createNewChat() }
        rule.waitUntil(30_000) { vm.state.value.activeChat?.id != previousChatId }
        rule.waitForIdle()
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
        assertEquals(AppSection.CHAT, vm.state.value.section)
        rule.waitUntil(240_000) { !vm.state.value.isGenerating && vm.state.value.messages.any { it.role == "assistant" } }
        check(vm.state.value.messages.any { it.role == "assistant" }) { "Response was not saved" }
        // Back while editing must dismiss the IME without opening the chat list.
        rule.onNode(hasSetTextAction()).performClick()
        rule.waitUntil(15_000) {
            rule.activity.window.decorView.rootWindowInsets?.let {
                WindowInsetsCompat.toWindowInsetsCompat(it).isVisible(WindowInsetsCompat.Type.ime())
            } == true
        }
        rule.onNode(hasSetTextAction()).assertIsDisplayed()
        rule.onNodeWithContentDescription("Send message").assertIsDisplayed()
        val decor = rule.activity.window.decorView
        val keyboardTop = decor.height - WindowInsetsCompat.toWindowInsetsCompat(decor.rootWindowInsets).getInsets(WindowInsetsCompat.Type.ime()).bottom
        val inputBottom = rule.onNode(hasSetTextAction()).fetchSemanticsNode().boundsInWindow.bottom
        val sendBottom = rule.onNodeWithContentDescription("Send message").fetchSemanticsNode().boundsInWindow.bottom
        check(inputBottom <= keyboardTop + 1f && sendBottom <= keyboardTop + 1f) {
            "Composer is covered by IME: input=$inputBottom send=$sendBottom keyboardTop=$keyboardTop"
        }
        saveScreenshot("keyboard-screen.png")
        rule.onNode(hasSetTextAction()).performTextInput("Second draft")
        InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("input keyevent KEYCODE_BACK").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        rule.waitUntil(15_000) {
            rule.activity.window.decorView.rootWindowInsets?.let {
                WindowInsetsCompat.toWindowInsetsCompat(it).isVisible(WindowInsetsCompat.Type.ime())
            } != true
        }
        rule.waitForIdle()
        rule.onNodeWithText("Claude").assertIsNotDisplayed()
        rule.onNode(hasSetTextAction()).assertIsDisplayed()
        check(vm.state.value.messages.any { it.role == "user" && it.content == "Say hello in one sentence." })
        assertEquals(AppSection.CHAT, vm.state.value.section)
        rule.waitForIdle()
        saveScreenshot("chat-screen.png")
        rule.activityRule.scenario.recreate()
        rule.waitUntil(30_000) {
            vm.state.value.messages.any { it.role == "user" && it.content == "Say hello in one sentence." }
        }
        rule.onNode(hasSetTextAction()).assertIsDisplayed()
        rule.onNodeWithTag("assistant-message").assertExists()
        rule.onNode(hasSetTextAction()).assertTextEquals("Second draft")
        assertEquals(AppSection.CHAT, vm.state.value.section)
    }
    private fun saveScreenshot(name: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/$name").use { descriptor ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).readBytes()
        }
    }
}
