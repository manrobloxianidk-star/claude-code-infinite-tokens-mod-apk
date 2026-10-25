package dev.infinitetokens.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Upsert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProject(id: String): ProjectEntity?

    @Upsert
    suspend fun upsert(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)
}
