package dev.infinitetokens.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.infinitetokens.model.AppSection
import kotlinx.coroutines.launch

@Composable
fun InfiniteTokensApp(chatViewModel: ChatViewModel = viewModel()) {
    val state = chatViewModel.state.collectAsStateWithLifecycle().value
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    BackHandler(enabled = state.section != AppSection.CHAT) {
        when (state.section) {
            AppSection.PROJECT -> chatViewModel.showSection(AppSection.PROJECTS)
            AppSection.RECENTS, AppSection.PROJECTS -> chatViewModel.showSection(AppSection.CHAT)
            AppSection.CHAT -> Unit
        }
    }

    BackHandler(enabled = state.section == AppSection.CHAT && !drawerState.isOpen) {
        scope.launch { drawerState.open() }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxHeight().widthIn(max = 370.dp),
                drawerContainerColor = Ink,
                drawerContentColor = Paper,
            ) {
                NavigationDrawer(
                    section = state.section,
                    recentChats = state.recentChats,
                    onChats = { chatViewModel.showSection(AppSection.CHAT); closeDrawer() },
                    onProjects = { chatViewModel.showSection(AppSection.PROJECTS); closeDrawer() },
                    onRecents = { chatViewModel.showSection(AppSection.RECENTS); closeDrawer() },
                    onSelectChat = { chatViewModel.selectChat(it); closeDrawer() },
                    onNewChat = { chatViewModel.createNewChat(); closeDrawer() },
                )
            }
        },
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Ink,
            contentColor = Paper,
            topBar = {
                Row(
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.statusBars).height(56.dp).padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Open navigation", tint = Muted)
                    }
                    InfiniteTokensMark(Modifier.padding(end = 8.dp).size(22.dp))
                }
            },
        ) { innerPadding ->
            HomeScreen(
                state = state,
                projectChats = state.recentChats.filter { it.projectId == state.activeProject?.id },
                onOpenProject = chatViewModel::openProject,
                onCreateProject = chatViewModel::createProject,
                onNewProjectChat = { chatViewModel.createNewChat(it) },
                onRenameProject = chatViewModel::renameProject,
                onDeleteProject = chatViewModel::deleteActiveProject,
                onSelectChat = chatViewModel::selectChat,
                onNewChat = { chatViewModel.createNewChat() },
                onSend = chatViewModel::sendMessage,
                onCancel = chatViewModel::cancelGeneration,
                onRetryModel = chatViewModel::retryModelLoad,
                onChooseModel = chatViewModel::chooseModel,
                onChooseEffort = chatViewModel::chooseEffort,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
