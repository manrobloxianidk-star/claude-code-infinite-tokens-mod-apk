package dev.infinitetokens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.infinitetokens.model.ChaosLevel
import dev.infinitetokens.model.ModelAlias

@Composable
internal fun ChatComposer(
    value: String,
    model: ModelAlias,
    effort: ChaosLevel,
    isGenerating: Boolean,
    enabled: Boolean,
    canSend: Boolean,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onCancel: () -> Unit,
    onModelPicker: () -> Unit,
    onEffortPicker: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Surface(
            color = RaisedInk,
            border = BorderStroke(1.dp, androidx.compose.ui.graphics.Color(0xFF41403E)),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(start = 14.dp, top = 13.dp, end = 10.dp, bottom = 9.dp)) {
                Box(modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp, max = 150.dp)) {
                    if (value.isEmpty()) {
                        Text("Chat with Claude…", color = Muted, fontSize = 17.sp)
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled && !isGenerating,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Paper, fontSize = 17.sp, lineHeight = 23.sp),
                        cursorBrush = SolidColor(Warm),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            focusManager.clearFocus()
                            onSend()
                        }),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(modifier = Modifier.weight(1f), onClick = onModelPicker, enabled = enabled && !isGenerating, shape = CircleShape) {
                        Text(model.displayName, color = Paper, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = onEffortPicker, enabled = enabled && !isGenerating, shape = CircleShape) {
                        Text(effort.displayName, color = Muted)
                    }
                    IconButton(
                        onClick = if (isGenerating) onCancel else onSend,
                        enabled = isGenerating || (enabled && canSend && value.isNotBlank()),
                        modifier = Modifier
                            .background(if (isGenerating || (enabled && canSend && value.isNotBlank())) Paper else RaisedInk, CircleShape),
                    ) {
                        Icon(
                            imageVector = if (isGenerating) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (isGenerating) "Stop response" else "Send message",
                            tint = if (isGenerating || (enabled && canSend && value.isNotBlank())) Ink else Muted,
                        )
                    }
                }
            }
        }
    }
}
