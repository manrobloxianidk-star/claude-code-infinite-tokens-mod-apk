package dev.infinitetokens.data

import java.util.UUID
import kotlinx.coroutines.flow.Flow

class ProjectRepositoryImpl(
    private val projectDao: ProjectDao,
) : ProjectRepository {
    override fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeProjects()

    override suspend fun createProject(name: String): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { "Untitled project" },
            createdAt = now,
            updatedAt = now,
        )
        projectDao.upsert(project)
        return project
    }

    override suspend fun renameProject(id: String, name: String) {
        val existing = projectDao.getProject(id) ?: return
        projectDao.upsert(existing.copy(name = name.trim().ifEmpty { existing.name }, updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deleteProject(id: String) {
        projectDao.getProject(id)?.let { project -> projectDao.delete(project) }
    }
}
