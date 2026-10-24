package dev.infinitetokens.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["updatedAt"])],
)
data class ChatEntity(
    @PrimaryKey val id: String,
    val projectId: String?,
    val title: String,
    val modelAliasId: String,
    val effortId: String,
    val createdAt: Long,
    val updatedAt: Long,
)
