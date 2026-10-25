package dev.infinitetokens.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatPersistenceTest {
    @Test fun updatingChatAndProjectPreservesMessagesAndMembership() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getInstrumentation().targetContext, AppDatabase::class.java).build()
        try {
            val projects = ProjectRepositoryImpl(db.projectDao())
            val project = projects.createProject("Project")
            val chats = ChatRepositoryImpl(db.chatDao(), db.messageDao())
            val chat = chats.createChat(project.id)
            chats.appendMessage(chat.id, "user", "hello")
            chats.appendMessage(chat.id, "assistant", "hi")
            assertEquals(2, chats.messagesForInference(chat.id).size)
            chats.updateSelection(chat.id, chat.modelAliasId, chat.effortId)
            assertEquals(2, chats.messagesForInference(chat.id).size)
            projects.renameProject(project.id, "Renamed")
            assertEquals(project.id, chats.getChat(chat.id)?.projectId)
            assertEquals(2, chats.messagesForInference(chat.id).size)
        } finally { db.close() }
    }
}
