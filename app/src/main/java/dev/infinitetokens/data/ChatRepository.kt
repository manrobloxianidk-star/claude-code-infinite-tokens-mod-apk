package dev.infinitetokens.data

import dev.infinitetokens.model.PromptMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeRecentChats(): Flow<List<ChatEntity>>
    fun observeProjectChats(projectId: String): Flow<List<ChatEntity>>
    fun observeMessages(chatId: String): Flow<List<MessageEntity>>
    suspend fun getChat(id: String): ChatEntity?
    suspend fun messagesForInference(chatId: String): List<PromptMessage>
    suspend fun createChat(projectId: String? = null): ChatEntity
    suspend fun updateSelection(chatId: String, modelAliasId: String, effortId: String)
    suspend fun appendMessage(chatId: String, role: String, content: String)
    suspend fun deleteChat(chatId: String)
}
