package jp.girky.taskmanage.cephalonGTD.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.TaskStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY priorityScore DESC, createdAt DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE groupId = :groupId ORDER BY priorityScore DESC, createdAt DESC")
    fun getTasksByGroup(groupId: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY priorityScore DESC, createdAt DESC")
    fun getTasksByStatus(status: TaskStatus): Flow<List<TaskItem>>

    @Query("SELECT DISTINCT groupId FROM tasks")
    fun getAllGroupIds(): Flow<List<String>>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskItem>)

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)
}
