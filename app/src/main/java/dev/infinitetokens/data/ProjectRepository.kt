package dev.infinitetokens.data

import kotlinx.coroutines.flow.Flow

interface ProjectRepository {
    fun observeProjects(): Flow<List<ProjectEntity>>
    suspend fun createProject(name: String): ProjectEntity
    suspend fun renameProject(id: String, name: String)
    suspend fun deleteProject(id: String)
}
