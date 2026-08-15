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

data class AiDiagnosticsResult(
    val isAiCoreInstalled: Boolean,
    val aiCoreVersion: String?,
    val availableModels: List<String>,
    val testPromptResult: String?,
    val isSuccess: Boolean,
    val message: String
)

interface AiEngine {
    suspend fun isAvailable(): Boolean
    suspend fun diagnoseGeminiNano(testPrompt: String = "こんにちは。10文字以内で挨拶を返してください。"): AiDiagnosticsResult
    suspend fun decomposeWbs(taskText: String): List<WbsSubTask>
    suspend fun generateMicroStep(taskText: String): WbsSubTask
    suspend fun analyzePriorityAndMetadata(taskText: String): TaskMetadataResult
    suspend fun generateDraft(task: TaskItem, type: DraftType): String
    suspend fun generateDailySummary(completedTasks: List<TaskItem>, pendingTasks: List<TaskItem>): String
}
