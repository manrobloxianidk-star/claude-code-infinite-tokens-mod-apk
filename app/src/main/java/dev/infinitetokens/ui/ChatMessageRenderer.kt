package dev.infinitetokens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.infinitetokens.data.MessageEntity

private val CodeFence = Regex("```([^\\n`]*)\\n?([\\s\\S]*?)```")

@Composable
internal fun ChatMessageRenderer(message: MessageEntity, modifier: Modifier = Modifier) {
    val isUser = message.role == "user"
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        if (isUser) {
            Surface(color = RaisedInk, shape = RoundedCornerShape(24.dp)) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        color = Paper,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    )
                }
            }
        } else {
            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    renderAssistantSegments(message.content)
                }
            }
        }
    }
}

@Composable
private fun renderAssistantSegments(content: String) {
    val matches = CodeFence.findAll(content).toList()
    if (matches.isEmpty()) {
        Text(content, color = Paper, style = MaterialTheme.typography.bodyLarge, lineHeight = 25.sp)
        return
    }

    var cursor = 0
    matches.forEach { match ->
        if (match.range.first > cursor) {
            Text(
                content.substring(cursor, match.range.first).trimEnd(),
                color = Paper,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 25.sp,
            )
        }
        val language = match.groupValues[1].trim()
        CodeBlock(code = match.groupValues[2].trimEnd(), language = language)
        cursor = match.range.last + 1
    }
    if (cursor < content.length) {
        Text(content.substring(cursor).trimStart(), color = Paper, style = MaterialTheme.typography.bodyLarge, lineHeight = 25.sp)
    }
}

@Composable
private fun CodeBlock(code: String, language: String) {
    val clipboard: ClipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RaisedInk, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(language.ifBlank { "Code" }, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy code", tint = Muted)
            }
        }
        Text(code, color = Paper, fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 19.sp)
    }
}
