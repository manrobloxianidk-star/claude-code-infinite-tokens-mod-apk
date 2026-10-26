package dev.infinitetokens.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.infinitetokens.data.ChatEntity
import dev.infinitetokens.model.ChaosLevel
import dev.infinitetokens.model.ModelAlias
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatComposerFlowTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun keepsDraftWhenTheFirstChatLoads() {
        var state by mutableStateOf(readyState())
        composeRule.setContent {
            InfiniteTokensTheme {
                ChatScreen(
                    state = state,
                    onSend = {},
                    onCancel = {},
                    onRetryModel = {},
                    onChooseModel = {},
                    onChooseEffort = {},
                )
            }
        }

        val input = composeRule.onNode(hasSetTextAction())
        input.assertIsDisplayed()
        input.performTextInput("hello")
        input.assertTextEquals("hello")

        state = state.copy(activeChat = testChat)
        composeRule.waitForIdle()

        input.assertTextEquals("hello")
    }

    @Test
    fun doesNotSendUntilAChatIsSelected() {
        var state by mutableStateOf(readyState())
        var sentMessage by mutableStateOf<String?>(null)
        composeRule.setContent {
            InfiniteTokensTheme {
                ChatScreen(
                    state = state,
                    onSend = { sentMessage = it },
                    onCancel = {},
                    onRetryModel = {},
                    onChooseModel = {},
                    onChooseEffort = {},
                )
            }
        }

        composeRule.onNode(hasSetTextAction()).performTextInput("hello")
        composeRule.onNodeWithContentDescription("Send message").assertIsNotEnabled()

        state = state.copy(activeChat = testChat)
        composeRule.waitForIdle()

        composeRule.onNode(hasSetTextAction()).assertTextEquals("hello")
        composeRule.onNodeWithContentDescription("Send message").assertIsEnabled().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { sentMessage == "hello" }
        assertEquals("hello", sentMessage)
    }

    private fun readyState() = ChatUiState(modelLoading = false, modelReady = true)

    private companion object {
        val testChat = ChatEntity(
            id = "test-chat",
            projectId = null,
            title = "New chat",
            modelAliasId = ModelAlias.DEFAULT.id,
            effortId = ChaosLevel.DEFAULT.id,
            createdAt = 0L,
            updatedAt = 0L,
        )
    }
}
