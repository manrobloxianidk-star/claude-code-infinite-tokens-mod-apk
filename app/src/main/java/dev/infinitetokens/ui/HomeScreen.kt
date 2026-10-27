package dev.infinitetokens.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.infinitetokens.data.ChatEntity
import dev.infinitetokens.model.AppSection

@Composable
internal fun HomeScreen(
    state: ChatUiState,
    projectChats: List<ChatEntity>,
    onOpenProject: (String) -> Unit,
    onCreateProject: (String) -> Unit,
    onNewProjectChat: (String) -> Unit,
    onRenameProject: (String) -> Unit,
    onDeleteProject: () -> Unit,
    onSelectChat: (String) -> Unit,
    onNewChat: () -> Unit,
    onSend: (String) -> Unit,
    onCancel: () -> Unit,
    onChooseModel: (dev.infinitetokens.model.ModelAlias) -> Unit,
    onChooseEffort: (dev.infinitetokens.model.ChaosLevel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (state.section) {
            AppSection.CHAT -> ChatScreen(
                state = state,
                onSend = onSend,
                onCancel = onCancel,
                onChooseModel = onChooseModel,
                onChooseEffort = onChooseEffort,
            )
            AppSection.RECENTS -> RecentChatsScreen(state.recentChats, onSelectChat, onNewChat)
            AppSection.PROJECTS -> ProjectsScreen(state.projects, onCreateProject, onOpenProject)
            AppSection.PROJECT -> ProjectDetailScreen(
                project = state.activeProject,
                chats = projectChats,
                onNewChat = { state.activeProject?.id?.let(onNewProjectChat) },
                onSelectChat = onSelectChat,
                onRename = onRenameProject,
                onDelete = onDeleteProject,
            )
        }
    }
}

@Composable
private fun RecentChatsScreen(chats: List<ChatEntity>, onSelect: (String) -> Unit, onNewChat: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
            Text("Chats", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
            IconButton(onClick = onNewChat) {
                Icon(Icons.Default.Add, contentDescription = "New chat", tint = Paper)
            }
        }
        if (chats.isEmpty()) {
            EmptyListMessage("No chats yet")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                items(chats, key = { it.id }) { chat ->
                    Surface(
                        color = SurfaceInk,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(chat.id) },
                    ) {
                        Text(chat.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp), maxLines = 2)
                    }
                }
            }
        }
    }
}
