package jp.girky.taskmanage.cephalonGTD.data.repository

import jp.girky.taskmanage.cephalonGTD.data.local.TaskDao
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getAllTasks(): Flow<List<TaskItem>> = taskDao.getAllTasks()

    override fun getTasksByGroup(groupId: String): Flow<List<TaskItem>> = taskDao.getTasksByGroup(groupId)

    override fun getTasksByStatus(status: TaskStatus): Flow<List<TaskItem>> = taskDao.getTasksByStatus(status)

    override fun getAllGroupIds(): Flow<List<String>> = taskDao.getAllGroupIds()

    override suspend fun getTaskById(id: String): TaskItem? = taskDao.getTaskById(id)

    override suspend fun upsertTask(task: TaskItem) = taskDao.insertTask(task)

    override suspend fun upsertTasks(tasks: List<TaskItem>) = taskDao.insertTasks(tasks)

    override suspend fun deleteTask(task: TaskItem) = taskDao.deleteTask(task)

    override suspend fun deleteTaskById(id: String) = taskDao.deleteTaskById(id)
}
