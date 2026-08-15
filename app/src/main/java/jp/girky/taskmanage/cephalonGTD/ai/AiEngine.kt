package jp.girky.taskmanage.cephalonGTD.ai

import jp.girky.taskmanage.cephalonGTD.data.model.TaskContextTag
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.WbsSubTask
import kotlinx.serialization.Serializable

@Serializable
data class TaskMetadataResult(
    val title: String,
    val isTwoMinuteRule: Boolean = false,
    val estimatedMinutes: Int = 15,
    val contextTag: TaskContextTag = TaskContextTag.OFFICE,
    val priorityScore: Int = 50,
    val nextActions: List<String> = emptyList()
)

enum class DraftType(val displayName: String) {
    CASE_RECORD("ケース記録下書き"),
    APPROVAL_REASON("起案・伺い理由書"),
    REPLY_MESSAGE("返信・連絡文面")
}

enum class DiagnosticStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED
}

data class DiagnosticStepItem(
    val id: String,
    val title: String,
    val description: String,
    val status: DiagnosticStatus = DiagnosticStatus.PENDING,
    val detailMessage: String? = null
)

interface AiEngine {
    suspend fun isAvailable(): Boolean
    fun runStepByStepDiagnostics(testPrompt: String = "こんにちは。10文字以内で挨拶を返してください。"): kotlinx.coroutines.flow.Flow<List<DiagnosticStepItem>>
    suspend fun decomposeWbs(taskText: String): List<WbsSubTask>
    suspend fun generateMicroStep(taskText: String): WbsSubTask
    suspend fun analyzePriorityAndMetadata(taskText: String): TaskMetadataResult
    suspend fun generateDraft(task: TaskItem, type: DraftType): String
    suspend fun generateDailySummary(completedTasks: List<TaskItem>, pendingTasks: List<TaskItem>): String
}
