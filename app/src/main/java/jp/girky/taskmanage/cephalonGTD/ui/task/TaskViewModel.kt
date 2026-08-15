package jp.girky.taskmanage.cephalonGTD.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.girky.taskmanage.cephalonGTD.ai.AiEngine
import jp.girky.taskmanage.cephalonGTD.ai.DraftType
import jp.girky.taskmanage.cephalonGTD.data.model.TaskContextTag
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.TaskStatus
import jp.girky.taskmanage.cephalonGTD.data.model.WbsSubTask
import jp.girky.taskmanage.cephalonGTD.data.preferences.UserPreferencesRepository
import jp.girky.taskmanage.cephalonGTD.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TaskUiState(
    val tasks: List<TaskItem> = emptyList(),
    val topFocusTask: TaskItem? = null,
    val selectedGroup: String = "すべて",
    val availableGroups: List<String> = listOf("すべて", "default", "ケースワーク", "庶務・起案"),
    val isLoadingAi: Boolean = false,
    val aiProgressMessage: String? = null,
    val dailySummaryDialogText: String? = null
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val aiEngine: AiEngine
) : ViewModel() {

    private val _isLoadingAi = MutableStateFlow(false)
    private val _aiProgressMessage = MutableStateFlow<String?>(null)
    private val _dailySummaryDialogText = MutableStateFlow<String?>(null)

    val uiState: StateFlow<TaskUiState> = combine(
        taskRepository.getAllTasks(),
        userPreferencesRepository.selectedGroup,
        _isLoadingAi,
        _aiProgressMessage,
        _dailySummaryDialogText
    ) { rawTasks, selectedGroup, isLoading, progressMsg, summaryText ->
        val now = System.currentTimeMillis()
        val tasks = rawTasks.map { task ->
            // 決裁待ちが24時間（86400000ms）以上経過、または起案締切超過の場合に停滞と判定
            val isStalledNow = (task.status == TaskStatus.WAITING_APPROVAL && task.approvalInitiatedAt != null && (now - task.approvalInitiatedAt > 86400000L)) ||
                    (task.status == TaskStatus.TODO && task.draftingDeadline != null && now > task.draftingDeadline)
            if (task.isStalled != isStalledNow) task.copy(isStalled = isStalledNow) else task
        }

        val filteredTasks = if (selectedGroup == "すべて") {
            tasks
        } else {
            tasks.filter { it.groupId == selectedGroup }
        }

        // Top Focus Task: TODO の中で priorityScore が最も高いもの
        val topFocus = filteredTasks
            .filter { it.status == TaskStatus.TODO }
            .maxByOrNull { it.priorityScore }

        val groups = (listOf("すべて", "default", "ケースワーク", "庶務・起案") + tasks.map { it.groupId }).distinct()

        TaskUiState(
            tasks = filteredTasks,
            topFocusTask = topFocus,
            selectedGroup = selectedGroup,
            availableGroups = groups,
            isLoadingAi = isLoading,
            aiProgressMessage = progressMsg,
            dailySummaryDialogText = summaryText
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TaskUiState()
    )

    fun setSelectedGroup(group: String) {
        viewModelScope.launch {
            userPreferencesRepository.setSelectedGroup(group)
        }
    }

    // 1. 把握 (Inbox): 即時ローカル保存
    fun addTask(
        rawInput: String,
        groupId: String = "default",
        requiresApproval: Boolean = false,
        contextTag: TaskContextTag = TaskContextTag.OFFICE
    ) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return

        viewModelScope.launch {
            val isTwoMin = trimmed.length < 15 && (trimmed.contains("確認") || trimmed.contains("返信"))
            val now = System.currentTimeMillis()
            // 決裁リードタイム（1日＝86400000ms）を逆算した起案締切日の自動算出
            val finalDl = if (requiresApproval) now + (3 * 86400000L) else null
            val draftDl = if (requiresApproval) now + 86400000L else null

            val newTask = TaskItem(
                id = UUID.randomUUID().toString(),
                groupId = if (groupId == "すべて") "default" else groupId,
                rawInput = trimmed,
                title = trimmed,
                requiresApproval = requiresApproval,
                status = TaskStatus.TODO,
                contextTag = contextTag,
                estimatedMinutes = if (isTwoMin) 2 else 15,
                isTwoMinuteRule = isTwoMin,
                priorityScore = if (requiresApproval) 70 else 50,
                draftingDeadline = draftDl,
                finalDeadline = finalDl,
                createdAt = now
            )
            taskRepository.upsertTask(newTask)
        }
    }

    // ステータス遷移 (業務ステートマシン)
    // 通常タスク: TODO -> DONE
    // 要決裁タスク: TODO -> WAITING_APPROVAL -> DONE
    fun advanceTaskStatus(task: TaskItem) {
        viewModelScope.launch {
            val updatedTask = when (task.status) {
                TaskStatus.TODO -> {
                    if (task.requiresApproval) {
                        task.copy(
                            status = TaskStatus.WAITING_APPROVAL,
                            approvalInitiatedAt = System.currentTimeMillis()
                        )
                    } else {
                        task.copy(status = TaskStatus.DONE)
                    }
                }
                TaskStatus.WAITING_APPROVAL -> {
                    task.copy(status = TaskStatus.DONE)
                }
                TaskStatus.DONE -> {
                    task.copy(status = TaskStatus.TODO)
                }
            }
            taskRepository.upsertTask(updatedTask)
        }
    }

    fun toggleSubTask(task: TaskItem, subTaskId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val updatedActions = task.nextPhysicalActions.map {
                if (it.id == subTaskId) it.copy(isCompleted = isCompleted) else it
            }
            taskRepository.upsertTask(task.copy(nextPhysicalActions = updatedActions))
        }
    }

    // ユーザー自身によるサブタスクの追加
    fun addSubTask(task: TaskItem, subTaskTitle: String) {
        val trimmed = subTaskTitle.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val newSubTask = WbsSubTask(
                id = UUID.randomUUID().toString(),
                title = trimmed,
                isCompleted = false
            )
            val updatedActions = task.nextPhysicalActions + newSubTask
            taskRepository.upsertTask(task.copy(nextPhysicalActions = updatedActions))
        }
    }

    // サブタスクの削除
    fun deleteSubTask(task: TaskItem, subTaskId: String) {
        viewModelScope.launch {
            val updatedActions = task.nextPhysicalActions.filterNot { it.id == subTaskId }
            taskRepository.upsertTask(task.copy(nextPhysicalActions = updatedActions))
        }
    }

    // タスクの締切日（期限）更新
    fun updateTaskDeadline(task: TaskItem, finalDeadline: Long?) {
        viewModelScope.launch {
            taskRepository.upsertTask(task.copy(finalDeadline = finalDeadline))
        }
    }

    // タスクの編集・更新
    fun updateTask(updatedTask: TaskItem) {
        viewModelScope.launch {
            taskRepository.upsertTask(updatedTask)
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    // 個別 AI WBS 分解
    fun decomposeTaskWbs(task: TaskItem) {
        viewModelScope.launch {
            _isLoadingAi.value = true
            _aiProgressMessage.value = "『${task.title}』をWBS分解中..."
            try {
                val subTasks = aiEngine.decomposeWbs(task.title)
                val updated = task.copy(nextPhysicalActions = subTasks)
                taskRepository.upsertTask(updated)
            } finally {
                _isLoadingAi.value = false
                _aiProgressMessage.value = null
            }
        }
    }

    // 初動5秒マイクロステップ生成
    fun generateMicroStep(task: TaskItem) {
        viewModelScope.launch {
            _isLoadingAi.value = true
            _aiProgressMessage.value = "初動5秒ステップを生成中..."
            try {
                val micro = aiEngine.generateMicroStep(task.title)
                val updated = task.copy(nextPhysicalActions = listOf(micro) + task.nextPhysicalActions)
                taskRepository.upsertTask(updated)
            } finally {
                _isLoadingAi.value = false
                _aiProgressMessage.value = null
            }
        }
    }

    // ドラフト生成
    fun generateDraft(task: TaskItem, type: DraftType) {
        viewModelScope.launch {
            _isLoadingAi.value = true
            _aiProgressMessage.value = "${type.displayName}を作成中..."
            try {
                val draft = aiEngine.generateDraft(task, type)
                val updated = task.copy(draftContent = draft)
                taskRepository.upsertTask(updated)
            } finally {
                _isLoadingAi.value = false
                _aiProgressMessage.value = null
            }
        }
    }

    // 一括 AI WBS 分解（未分解の未完了タスク対象）
    fun batchDecomposeWbs() {
        viewModelScope.launch {
            val currentTasks = uiState.value.tasks
            val targets = currentTasks.filter { it.status != TaskStatus.DONE && it.nextPhysicalActions.isEmpty() }
            if (targets.isEmpty()) return@launch

            _isLoadingAi.value = true
            try {
                targets.forEachIndexed { index, task ->
                    _aiProgressMessage.value = "WBS一括分解中 (${index + 1}/${targets.size}): ${task.title}"
                    val subTasks = aiEngine.decomposeWbs(task.title)
                    taskRepository.upsertTask(task.copy(nextPhysicalActions = subTasks))
                }
            } finally {
                _isLoadingAi.value = false
                _aiProgressMessage.value = null
            }
        }
    }

    // 一括 AI 優先度解析
    fun batchAnalyzePriority() {
        viewModelScope.launch {
            val currentTasks = uiState.value.tasks
            val targets = currentTasks.filter { it.status != TaskStatus.DONE }
            if (targets.isEmpty()) return@launch

            _isLoadingAi.value = true
            _aiProgressMessage.value = "全タスクの優先度・メタデータを解析中..."
            try {
                targets.forEach { task ->
                    val result = aiEngine.analyzePriorityAndMetadata(task.title)
                    val updated = task.copy(
                        priorityScore = result.priorityScore,
                        isTwoMinuteRule = result.isTwoMinuteRule,
                        estimatedMinutes = result.estimatedMinutes,
                        contextTag = result.contextTag
                    )
                    taskRepository.upsertTask(updated)
                }
            } finally {
                _isLoadingAi.value = false
                _aiProgressMessage.value = null
            }
        }
    }

    // 日次締めサマリー生成
    fun generateDailySummary() {
        viewModelScope.launch {
            val currentTasks = uiState.value.tasks
            val completed = currentTasks.filter { it.status == TaskStatus.DONE }
            val pending = currentTasks.filter { it.status != TaskStatus.DONE }

            _isLoadingAi.value = true
            _aiProgressMessage.value = "日次サマリーを作成中..."
            try {
                val summary = aiEngine.generateDailySummary(completed, pending)
                _dailySummaryDialogText.value = summary
            } finally {
                _isLoadingAi.value = false
                _aiProgressMessage.value = null
            }
        }
    }

    fun dismissDailySummaryDialog() {
        _dailySummaryDialogText.value = null
    }
}
