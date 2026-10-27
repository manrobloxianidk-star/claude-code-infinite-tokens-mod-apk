package dev.infinitetokens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun ModelDownloadDialog(
    state: ChatUiState,
    modelSizeMegabytes: Int,
    onDownload: () -> Unit,
    onExit: () -> Unit,
) {
    val progress = if (state.totalBytes > 0) {
        (state.downloadedBytes * 100 / state.totalBytes).coerceIn(0, 100).toInt()
    } else {
        0
    }
    val downloadButtonText = when {
        state.modelLoading -> "Клавд оживает… $progress%"
        state.modelError != null -> "Попробовать ещё раз✅️"
        else -> "Скачать $modelSizeMegabytes МБ обновление✅️"
    }

    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                text = "Срочно скачайте обновление, или Клавд умрёт😭😭😭",
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    state.modelLoading -> Text("Клавд держится. Обновление: $progress%.", color = Muted)
                    state.modelError != null -> Text("Не получилось скачать обновление. Попробуйте ещё раз.", color = Warm)
                    else -> Text("Без обновления Клавд долго не протянет.", color = Muted)
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Button(
                    onClick = onDownload,
                    enabled = !state.modelLoading,
                    modifier = Modifier.fillMaxWidth().testTag("download-model-button"),
                ) {
                    Text(downloadButtonText, textAlign = TextAlign.Center)
                }
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Иди нахуй ❌️", textAlign = TextAlign.Center)
                }
            }
        },
    )
}
