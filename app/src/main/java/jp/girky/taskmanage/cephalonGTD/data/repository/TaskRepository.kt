package jp.girky.taskmanage.cephalonGTD.data.repository

import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun getAllTasks(): Flow<List<TaskItem>>
    fun getTasksByGroup(groupId: String): Flow<List<TaskItem>>
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskItem>>
    fun getAllGroupIds(): Flow<List<String>>
    suspend fun getTaskById(id: String): TaskItem?
    suspend fun upsertTask(task: TaskItem)
    suspend fun upsertTasks(tasks: List<TaskItem>)
    suspend fun deleteTask(task: TaskItem)
    suspend fun deleteTaskById(id: String)
}
