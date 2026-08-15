package jp.girky.taskmanage.cephalonGTD.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class TaskStatus {
    TODO,
    WAITING_APPROVAL,
    DONE
}

enum class TaskContextTag(val label: String) {
    OFFICE("@職場"),
    PC("@PC"),
    PHONE("@電話"),
    EXTERNAL("@外出"),
    WAITING("@連絡待ち")
}

@Serializable
data class WbsSubTask(
    val id: String,
    val title: String,
    val isCompleted: Boolean = false
)

@Entity(tableName = "tasks")
@Serializable
data class TaskItem(
    @PrimaryKey
    val id: String,
    val groupId: String = "default",
    val rawInput: String,
    val title: String,
    val requiresApproval: Boolean = false,
    val status: TaskStatus = TaskStatus.TODO,
    val contextTag: TaskContextTag = TaskContextTag.OFFICE,
    val estimatedMinutes: Int = 15,
    val isTwoMinuteRule: Boolean = false,
    val priorityScore: Int = 0,
    val nextPhysicalActions: List<WbsSubTask> = emptyList(),
    val draftContent: String? = null,
    val approvalInitiatedAt: Long? = null,
    val draftingDeadline: Long? = null,
    val finalDeadline: Long? = null,
    val isStalled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
