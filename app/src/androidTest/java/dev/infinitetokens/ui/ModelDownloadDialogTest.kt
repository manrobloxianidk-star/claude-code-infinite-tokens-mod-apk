package dev.infinitetokens.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ModelDownloadDialogTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun offersTheModelDownloadOrExit() {
        val downloadRequests = AtomicInteger()
        val exitRequests = AtomicInteger()
        composeRule.setContent {
            InfiniteTokensTheme {
                ModelDownloadDialog(
                    state = ChatUiState(),
                    modelSizeMegabytes = 101,
                    onDownload = { downloadRequests.incrementAndGet() },
                    onExit = { exitRequests.incrementAndGet() },
                )
            }
        }

        composeRule.onNodeWithText("Срочно скачайте обновление, или Клавд умрёт😭😭😭").assertIsDisplayed()
        composeRule.onNodeWithText("Скачать 101 МБ обновление✅️").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Иди нахуй ❌️").assertIsDisplayed().performClick()

        assertEquals(1, downloadRequests.get())
        assertEquals(1, exitRequests.get())
    }

    @Test
    fun keepsTheExitChoiceWhileTheUpdateIsInstalling() {
        composeRule.setContent {
            InfiniteTokensTheme {
                ModelDownloadDialog(
                    state = ChatUiState(
                        modelLoading = true,
                        modelInstallAccepted = true,
                        copiedBytes = 52_727_216,
                        totalBytes = 105_454_432,
                    ),
                    modelSizeMegabytes = 101,
                    onDownload = {},
                    onExit = {},
                )
            }
        }

        composeRule.onNodeWithText("Клавд держится. Обновление: 50%.").assertIsDisplayed()
        composeRule.onNodeWithTag("download-model-button").assertIsNotEnabled()
        composeRule.onNodeWithText("Иди нахуй ❌️").assertIsDisplayed()
    }
}
