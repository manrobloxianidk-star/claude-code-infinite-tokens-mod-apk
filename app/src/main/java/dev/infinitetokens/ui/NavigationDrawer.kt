package dev.infinitetokens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.infinitetokens.data.ChatEntity
import dev.infinitetokens.model.AppSection

@Composable
internal fun NavigationDrawer(
    section: AppSection,
    recentChats: List<ChatEntity>,
    onChats: () -> Unit,
    onProjects: () -> Unit,
    onRecents: () -> Unit,
    onSelectChat: (String) -> Unit,
    onNewChat: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxHeight().fillMaxWidth().background(Ink).padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Text(
            "Infinite Tokens",
            color = Paper,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
            fontSize = 29.sp,
            modifier = Modifier.padding(start = 8.dp, top = 20.dp, bottom = 28.dp),
        )
        DrawerItem("Chats", Icons.Default.ChatBubbleOutline, section == AppSection.CHAT, onChats)
        DrawerItem("Projects", Icons.Default.FolderOpen, section == AppSection.PROJECTS || section == AppSection.PROJECT, onProjects)
        DrawerItem("Recents", Icons.Default.History, section == AppSection.RECENTS, onRecents)

        Text("Recents", color = Muted, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 10.dp, top = 24.dp, bottom = 8.dp))
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            recentChats.take(12).forEach { chat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .clickable { onSelectChat(chat.id) }
                        .padding(horizontal = 10.dp, vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = null, tint = Muted)
                    Text(chat.title, color = Paper, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                }
            }
            if (recentChats.isEmpty()) Text("No chats yet", color = Muted, modifier = Modifier.padding(12.dp))
        }

        TextButton(onClick = onNewChat, modifier = Modifier.align(Alignment.End).padding(top = 14.dp)) {
            Surface(color = Paper, shape = CircleShape) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Ink)
                    Text("New chat", color = Ink, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val foreground = if (selected) Paper else Muted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .background(if (selected) SurfaceInk else Ink)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = foreground)
        Text(label, color = foreground, style = MaterialTheme.typography.titleMedium)
    }
}
