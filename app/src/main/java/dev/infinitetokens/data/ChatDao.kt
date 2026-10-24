package dev.infinitetokens.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY updatedAt DESC")
    fun observeRecentChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeProjectChats(projectId: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE id = :id LIMIT 1")
    suspend fun getChat(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(chat: ChatEntity)

    @Delete
    suspend fun delete(chat: ChatEntity)
}
