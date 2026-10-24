package dev.infinitetokens.data

import dev.infinitetokens.model.ChaosLevel
import dev.infinitetokens.model.ModelAlias
import dev.infinitetokens.model.PromptMessage
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao,
) : ChatRepository {
    override fun observeRecentChats(): Flow<List<ChatEntity>> = chatDao.observeRecentChats()

    override fun observeProjectChats(projectId: String): Flow<List<ChatEntity>> =
        chatDao.observeProjectChats(projectId)

    override fun observeMessages(chatId: String): Flow<List<MessageEntity>> = messageDao.observeMessages(chatId)

    override suspend fun getChat(id: String): ChatEntity? = chatDao.getChat(id)

    override suspend fun messagesForInference(chatId: String): List<PromptMessage> =
        messageDao.getMessages(chatId).map { PromptMessage(it.role, it.content) }

    override suspend fun createChat(projectId: String?): ChatEntity {
        val now = System.currentTimeMillis()
        val chat = ChatEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            title = "New chat",
            modelAliasId = ModelAlias.DEFAULT.id,
            effortId = ChaosLevel.DEFAULT.id,
            createdAt = now,
            updatedAt = now,
        )
        chatDao.upsert(chat)
        return chat
    }

    override suspend fun updateSelection(chatId: String, modelAliasId: String, effortId: String) {
        val chat = chatDao.getChat(chatId) ?: return
        chatDao.upsert(chat.copy(modelAliasId = modelAliasId, effortId = effortId, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun appendMessage(chatId: String, role: String, content: String) {
        val chat = chatDao.getChat(chatId) ?: return
        val now = System.currentTimeMillis()
        messageDao.insert(MessageEntity(chatId = chatId, role = role, content = content, createdAt = now))
        val title = if (role == "user" && chat.title == "New chat") {
            content.lineSequence().firstOrNull()?.trim()?.take(48)?.ifEmpty { chat.title } ?: chat.title
        } else {
            chat.title
        }
        chatDao.upsert(chat.copy(title = title, updatedAt = now))
    }

    override suspend fun deleteChat(chatId: String) {
        chatDao.getChat(chatId)?.let { chat -> chatDao.delete(chat) }
    }
}
