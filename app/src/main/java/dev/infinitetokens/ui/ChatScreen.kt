package dev.infinitetokens.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.infinitetokens.data.MessageEntity
import dev.infinitetokens.model.ChaosLevel
import dev.infinitetokens.model.ModelAlias
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun ChatScreen(
    state: ChatUiState,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onRetryModel: () -> Unit,
    onChooseModel: (ModelAlias) -> Unit,
    onChooseEffort: (ChaosLevel) -> Unit,
) {
    var draft by remember(state.activeChat?.id) { mutableStateOf("") }
    var showModelPicker by remember { mutableStateOf(false) }
    var showEffortPicker by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val currentModel = ModelAlias.fromId(state.activeChat?.modelAliasId ?: ModelAlias.DEFAULT.id)
    val currentEffort = ChaosLevel.fromId(state.activeChat?.effortId ?: ChaosLevel.DEFAULT.id)
    val hasContent = state.messages.isNotEmpty() || state.streamingResponse.isNotEmpty()

    LaunchedEffect(state.messages.size, state.streamingResponse) {
        if (hasContent) listState.animateScrollToItem((state.messages.size - 1 + if (state.streamingResponse.isNotEmpty()) 1 else 0).coerceAtLeast(0))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!hasContent) {
            EmptyChatWelcome(modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 22.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(23.dp),
            ) {
                items(state.messages, key = { it.id }) { message -> ChatMessageRenderer(message) }
                if (state.streamingResponse.isNotEmpty()) {
                    item(key = "streaming-response") {
                        ChatMessageRenderer(
                            MessageEntity(id = Long.MAX_VALUE, chatId = state.activeChat?.id.orEmpty(), role = "assistant", content = state.streamingResponse, createdAt = 0L),
                        )
                    }
                }
            }
        }

        if (state.modelLoading) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 26.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(17.dp), color = Warm, strokeWidth = 2.dp)
                val progress = if (state.totalBytes > 0) "${(state.copiedBytes * 100 / state.totalBytes).coerceIn(0, 100)}%" else ""
                Text(if (progress.isEmpty()) "Preparing the local model" else "Preparing the local model · $progress", color = Muted)
            }
        }
        state.modelError?.let { error ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(error, color = Warm, modifier = Modifier.weight(1f))
                TextButton(onClick = onRetryModel) { Text("Retry") }
            }
        }
        state.generationError?.let { error ->
            Text(error, color = Warm, modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp))
        }

        ChatComposer(
            value = draft,
            model = currentModel,
            effort = currentEffort,
            isGenerating = state.isGenerating,
            enabled = true,
            onValueChange = { draft = it },
            onSend = {
                val value = draft
                if (value.isNotBlank() && !state.isGenerating) {
                    draft = ""
                    onSend(value)
                }
            },
            onCancel = onCancel,
            onModelPicker = { showModelPicker = true },
            onEffortPicker = { showEffortPicker = true },
        )
    }

    if (showModelPicker) {
        ModelPickerSheet(
            selected = currentModel,
            onSelect = { onChooseModel(it); showModelPicker = false },
            onDismiss = { showModelPicker = false },
        )
    }
    if (showEffortPicker) {
        EffortPickerSheet(
            selected = currentEffort,
            onSelect = { onChooseEffort(it); showEffortPicker = false },
            onDismiss = { showEffortPicker = false },
        )
    }
}

@Composable
private fun EmptyChatWelcome(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        InfiniteTokensMark(Modifier.size(52.dp))
        Text(
            "Up late, Что такое бананы?",
            color = Paper,
            fontFamily = FontFamily.Serif,
            fontSize = 25.sp,
            lineHeight = 32.sp,
            modifier = Modifier.padding(top = 18.dp),
        )
        Text("Локальная модель · без аккаунта", color = Muted, modifier = Modifier.padding(top = 28.dp))
    }
}

@Composable
internal fun InfiniteTokensMark(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val innerRadius = size.minDimension * 0.18f
        val outerRadius = size.minDimension * 0.45f
        repeat(12) { index ->
            val angle = (index * 30f - 90f) * (Math.PI / 180.0)
            val start = Offset(center.x + cos(angle).toFloat() * innerRadius, center.y + sin(angle).toFloat() * innerRadius)
            val end = Offset(center.x + cos(angle).toFloat() * outerRadius, center.y + sin(angle).toFloat() * outerRadius)
            drawLine(color = Warm, start = start, end = end, strokeWidth = size.minDimension * 0.085f, cap = StrokeCap.Round)
        }
    }
}
