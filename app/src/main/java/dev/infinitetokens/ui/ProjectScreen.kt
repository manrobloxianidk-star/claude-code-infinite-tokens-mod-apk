package dev.infinitetokens.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.infinitetokens.data.ChatEntity
import dev.infinitetokens.data.ProjectEntity
import dev.infinitetokens.model.ModelAlias
import dev.infinitetokens.model.ChaosLevel

@Composable
internal fun ProjectsScreen(
    projects: List<ProjectEntity>,
    onCreate: (String) -> Unit,
    onSelect: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Projects", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
            IconButton(onClick = { name = ""; showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Create project", tint = Paper)
            }
        }
        Text("Keep related chats together.", color = Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
        if (projects.isEmpty()) {
            EmptyListMessage("No projects yet")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(projects, key = { it.id }) { project ->
                    Surface(color = SurfaceInk, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onSelect(project.id) }) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleLarge)
                            Text("Project", color = Muted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 5.dp))
                        }
                    }
                }
            }
        }
    }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New project") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Project name") }) },
            confirmButton = {
                Button(onClick = { onCreate(name); showCreate = false }, enabled = name.isNotBlank()) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun ProjectDetailScreen(
    project: ProjectEntity?,
    chats: List<ChatEntity>,
    onNewChat: () -> Unit,
    onSelectChat: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
) {
    var showRename by remember(project?.id) { mutableStateOf(false) }
    var showDelete by remember(project?.id) { mutableStateOf(false) }
    var name by remember(project?.id) { mutableStateOf(project?.name.orEmpty()) }
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(project?.name ?: "Project", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { name = project?.name.orEmpty(); showRename = true }) { Icon(Icons.Default.Edit, contentDescription = "Rename project", tint = Muted) }
            IconButton(onClick = { showDelete = true }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete project", tint = Muted) }
        }
        Button(onClick = onNewChat, shape = CircleShape, modifier = Modifier.padding(top = 16.dp, bottom = 22.dp)) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("  New chat")
        }
        if (chats.isEmpty()) {
            EmptyListMessage("No chats in this project")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(chats, key = { it.id }) { chat ->
                    Surface(color = SurfaceInk, shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable { onSelectChat(chat.id) }) {
                        Column(modifier = Modifier.padding(17.dp)) {
                            Text(chat.title, style = MaterialTheme.typography.titleMedium)
                            Text("${ModelAlias.fromId(chat.modelAliasId).displayName} · ${ChaosLevel.fromId(chat.effortId).displayName}", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            }
        }
    }
    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Rename project") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true) },
            confirmButton = { Button(onClick = { onRename(name); showRename = false }, enabled = name.isNotBlank()) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("Cancel") } },
        )
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete project?") },
            text = { Text("Its chats will stay in Recents.") },
            confirmButton = { Button(onClick = { onDelete(); showDelete = false }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
internal fun EmptyListMessage(text: String) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, color = Muted, style = MaterialTheme.typography.bodyLarge)
    }
}
