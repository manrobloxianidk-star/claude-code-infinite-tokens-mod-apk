package dev.infinitetokens.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.infinitetokens.data.AppDatabase
import dev.infinitetokens.data.ChatEntity
import dev.infinitetokens.data.ChatRepositoryImpl
import dev.infinitetokens.data.MessageEntity
import dev.infinitetokens.data.ProjectEntity
import dev.infinitetokens.data.ProjectRepositoryImpl
import dev.infinitetokens.inference.LlamaCppInferenceEngine
import dev.infinitetokens.inference.LocalInferenceEngine
import dev.infinitetokens.inference.ModelAssetInstaller
import dev.infinitetokens.inference.ModelInstallProgress
import dev.infinitetokens.model.AppSection
import dev.infinitetokens.model.ChaosLevel
import dev.infinitetokens.model.ModelAlias
import dev.infinitetokens.model.PromptBuilder
import dev.infinitetokens.model.PromptMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ChatUiState(
    val section: AppSection = AppSection.CHAT,
    val projects: List<ProjectEntity> = emptyList(),
    val recentChats: List<ChatEntity> = emptyList(),
    val activeChat: ChatEntity? = null,
    val activeProject: ProjectEntity? = null,
    val messages: List<MessageEntity> = emptyList(),
    val modelLoading: Boolean = true,
    val modelReady: Boolean = false,
    val modelError: String? = null,
    val copiedBytes: Long = 0,
    val totalBytes: Long = 0,
    val isGenerating: Boolean = false,
    val streamingResponse: String = "",
    val generationError: String? = null,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.get(application)
    private val chats = ChatRepositoryImpl(database.chatDao(), database.messageDao())
    private val projects = ProjectRepositoryImpl(database.projectDao())
    private val installer = ModelAssetInstaller(application)
    private val inference: LocalInferenceEngine = LlamaCppInferenceEngine()
    private val modelMutex = Mutex()
    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var messagesJob: Job? = null
    private var initialChatHandled = false

    init {
        viewModelScope.launch {
            projects.observeProjects().collect { rows ->
                _state.update { current ->
                    current.copy(
                        projects = rows,
                        activeProject = current.activeProject?.let { active -> rows.firstOrNull { it.id == active.id } },
                    )
                }
            }
        }
        viewModelScope.launch {
            chats.observeRecentChats().collect { rows ->
                _state.update { it.copy(recentChats = rows) }
                if (!initialChatHandled) {
                    initialChatHandled = true
                    val firstChat = rows.firstOrNull()
                    if (firstChat == null) createNewChat() else selectChat(firstChat.id)
                } else if (_state.value.activeChat?.let { active -> rows.none { it.id == active.id } } == true) {
                    rows.firstOrNull()?.let { selectChat(it.id) }
                }
            }
        }
        viewModelScope.launch { loadModel() }
    }

    fun showSection(section: AppSection) {
        _state.update { it.copy(section = section, activeProject = if (section == AppSection.PROJECTS) null else it.activeProject) }
    }

    fun selectChat(id: String) {
        viewModelScope.launch {
            val chat = chats.getChat(id) ?: return@launch
            val previousChatId = _state.value.activeChat?.id
            if (previousChatId != null && previousChatId != id && _state.value.isGenerating) inference.cancel()
            messagesJob?.cancel()
            _state.update {
                it.copy(
                    section = AppSection.CHAT,
                    activeChat = chat,
                    messages = emptyList(),
                    isGenerating = if (previousChatId != id) false else it.isGenerating,
                    streamingResponse = "",
                    generationError = null,
                )
            }
            messagesJob = viewModelScope.launch {
                chats.observeMessages(id).collect { rows -> _state.update { it.copy(messages = rows) } }
            }
        }
    }

    fun createNewChat(projectId: String? = null) {
        viewModelScope.launch {
            val chat = chats.createChat(projectId)
            if (projectId != null) {
                val project = _state.value.projects.firstOrNull { it.id == projectId }
                _state.update { it.copy(activeProject = project) }
            }
            selectChat(chat.id)
        }
    }

    fun openProject(id: String) {
        val project = _state.value.projects.firstOrNull { it.id == id } ?: return
        _state.update { it.copy(section = AppSection.PROJECT, activeProject = project) }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val project = projects.createProject(name)
            _state.update { it.copy(section = AppSection.PROJECT, activeProject = project) }
        }
    }

    fun renameProject(name: String) {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch { projects.renameProject(project.id, name) }
    }

    fun deleteActiveProject() {
        val project = _state.value.activeProject ?: return
        viewModelScope.launch {
            projects.deleteProject(project.id)
            _state.update { it.copy(section = AppSection.PROJECTS, activeProject = null) }
        }
    }

    fun chooseModel(model: ModelAlias) = updateSelection(model.id, currentEffort().id)

    fun chooseEffort(effort: ChaosLevel) = updateSelection(currentModel().id, effort.id)

    private fun updateSelection(modelId: String, effortId: String) {
        val chat = _state.value.activeChat ?: return
        _state.update { current ->
            current.copy(activeChat = chat.copy(modelAliasId = modelId, effortId = effortId, updatedAt = System.currentTimeMillis()))
        }
        viewModelScope.launch { chats.updateSelection(chat.id, modelId, effortId) }
    }

    fun retryModelLoad() {
        viewModelScope.launch { loadModel() }
    }

    private suspend fun loadModel() = modelMutex.withLock {
        if (_state.value.modelReady) return@withLock
        _state.update { it.copy(modelLoading = true, modelError = null, copiedBytes = 0, totalBytes = 0) }
        try {
            val file = installer.installIfNeeded { progress: ModelInstallProgress ->
                _state.update { it.copy(copiedBytes = progress.copiedBytes, totalBytes = progress.totalBytes) }
            }
            _state.update { it.copy(copiedBytes = file.length(), totalBytes = file.length()) }
            inference.load(file)
            _state.update { it.copy(modelLoading = false, modelReady = true, modelError = null) }
        } catch (error: Exception) {
            _state.update { it.copy(modelLoading = false, modelReady = false, modelError = error.localizedMessage ?: "Could not load the local model") }
        }
    }

    fun sendMessage(content: String) {
        val cleanContent = content.trim()
        val chat = _state.value.activeChat ?: return
        if (cleanContent.isEmpty() || _state.value.isGenerating) return

        _state.update { it.copy(isGenerating = true, streamingResponse = "", generationError = null) }
        viewModelScope.launch {
            var answer = ""
            try {
                loadModel()
                if (!_state.value.modelReady) throw IllegalStateException("The local model could not be loaded")
                chats.appendMessage(chat.id, "user", cleanContent)
                val history = chats.messagesForInference(chat.id)
                val effort = ChaosLevel.fromId(chat.effortId)
                val context = PromptBuilder.assembleContext(effort, history)
                inference.generate(context).collect { piece ->
                    answer += piece
                    _state.update { current ->
                        if (current.activeChat?.id == chat.id) current.copy(streamingResponse = answer) else current
                    }
                }
                if (answer.isNotBlank()) chats.appendMessage(chat.id, "assistant", answer)
            } catch (error: Exception) {
                _state.update { current ->
                    if (current.activeChat?.id == chat.id) current.copy(generationError = error.localizedMessage ?: "The response could not be generated") else current
                }
            } finally {
                _state.update { current ->
                    if (current.activeChat?.id == chat.id) current.copy(isGenerating = false, streamingResponse = "") else current
                }
            }
        }
    }

    fun cancelGeneration() = inference.cancel()

    private fun currentModel(): ModelAlias = ModelAlias.fromId(_state.value.activeChat?.modelAliasId ?: ModelAlias.DEFAULT.id)
    private fun currentEffort(): ChaosLevel = ChaosLevel.fromId(_state.value.activeChat?.effortId ?: ChaosLevel.DEFAULT.id)

    override fun onCleared() {
        inference.close()
        super.onCleared()
    }
}
