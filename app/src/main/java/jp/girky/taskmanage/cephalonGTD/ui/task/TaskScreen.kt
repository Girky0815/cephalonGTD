package jp.girky.taskmanage.cephalonGTD.ui.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jp.girky.taskmanage.cephalonGTD.data.model.TaskContextTag
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.TaskStatus
import jp.girky.taskmanage.cephalonGTD.ui.components.TaskCardItem
import jp.girky.taskmanage.cephalonGTD.ui.components.TopFocusCard

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputTaskText by remember { mutableStateOf("") }
    var isRequiresApprovalInput by remember { mutableStateOf(false) }
    var selectedTagInput by remember { mutableStateOf(TaskContextTag.OFFICE) }

    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Cephalon GTD",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            // 連結ボタングループ (Bottom Action Strip) & タスク入力バー
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // クイックタスク入力バー (IMEシークレットモード強制)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputTaskText,
                        onValueChange = { inputTaskText = it },
                        placeholder = { Text("タスクを把握 (Inbox)...") },
                        singleLine = true,
                        // IMEシークレットモード強制
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (inputTaskText.isNotBlank()) {
                                    viewModel.addTask(
                                        rawInput = inputTaskText,
                                        groupId = uiState.selectedGroup,
                                        requiresApproval = isRequiresApprovalInput,
                                        contextTag = selectedTagInput
                                    )
                                    inputTaskText = ""
                                }
                            }
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (inputTaskText.isNotBlank()) {
                                viewModel.addTask(
                                    rawInput = inputTaskText,
                                    groupId = uiState.selectedGroup,
                                    requiresApproval = isRequiresApprovalInput,
                                    contextTag = selectedTagInput
                                    )
                                inputTaskText = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "追加",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // タグ・要決裁フラグ選択バー
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    FilterChip(
                        selected = isRequiresApprovalInput,
                        onClick = { isRequiresApprovalInput = !isRequiresApprovalInput },
                        label = { Text("要決裁", style = MaterialTheme.typography.labelSmall) }
                    )

                    TaskContextTag.entries.forEach { tag ->
                        FilterChip(
                            selected = selectedTagInput == tag,
                            onClick = { selectedTagInput = tag },
                            label = { Text(tag.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                // 連結ボタングループ (Bottom Action Strip)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilledTonalButton(
                            onClick = { viewModel.batchDecomposeWbs() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI WBS分解", style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalButton(
                            onClick = { viewModel.batchAnalyzePriority() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI 優先度解析", style = MaterialTheme.typography.labelSmall)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        FilledTonalButton(
                            onClick = { viewModel.generateDailySummary() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("日次締め", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // グループ FilterChip 列
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.availableGroups.forEach { group ->
                        FilterChip(
                            selected = uiState.selectedGroup == group,
                            onClick = { viewModel.setSelectedGroup(group) },
                            label = { Text(group) }
                        )
                    }
                }
            }

            // Top Focus Card (最優先タスク)
            if (uiState.topFocusTask != null) {
                item {
                    TopFocusCard(
                        task = uiState.topFocusTask!!,
                        onStatusAdvance = { viewModel.advanceTaskStatus(it) },
                        onSubTaskToggle = { task, subId, isCompleted ->
                            viewModel.toggleSubTask(task, subId, isCompleted)
                        },
                        onDecompose = { viewModel.decomposeTaskWbs(it) }
                    )
                }
            }

            // 要決裁 / 決裁待ちタスクコンテナ (Segmented List)
            val waitingApprovalTasks = uiState.tasks.filter { it.status == TaskStatus.WAITING_APPROVAL }
            if (waitingApprovalTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "決裁待ち (${waitingApprovalTasks.size}件)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        waitingApprovalTasks.forEachIndexed { index, task ->
                            val shape = getSegmentedShape(index = index, count = waitingApprovalTasks.size)
                            TaskCardItem(
                                task = task,
                                onStatusAdvance = { viewModel.advanceTaskStatus(it) },
                                onSubTaskToggle = { t, sId, c -> viewModel.toggleSubTask(t, sId, c) },
                                onAddSubTask = { t, title -> viewModel.addSubTask(t, title) },
                                onDeleteSubTask = { t, sId -> viewModel.deleteSubTask(t, sId) },
                                onUpdateDeadline = { t, dl -> viewModel.updateTaskDeadline(t, dl) },
                                onUpdateTask = { viewModel.updateTask(it) },
                                onDecompose = { viewModel.decomposeTaskWbs(it) },
                                onMicroStep = { viewModel.generateMicroStep(it) },
                                onGenerateDraft = { t, dType -> viewModel.generateDraft(t, dType) },
                                onDelete = { viewModel.deleteTask(it) },
                                shape = shape,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // 通常 TODO タスクリスト (Segmented List)
            val todoTasks = uiState.tasks.filter { it.status == TaskStatus.TODO }
            item {
                Text(
                    text = "実行可能 TODO (${todoTasks.size}件)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (todoTasks.isNotEmpty()) {
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        todoTasks.forEachIndexed { index, task ->
                            val shape = getSegmentedShape(index = index, count = todoTasks.size)
                            TaskCardItem(
                                task = task,
                                onStatusAdvance = { viewModel.advanceTaskStatus(it) },
                                onSubTaskToggle = { t, sId, c -> viewModel.toggleSubTask(t, sId, c) },
                                onAddSubTask = { t, title -> viewModel.addSubTask(t, title) },
                                onDeleteSubTask = { t, sId -> viewModel.deleteSubTask(t, sId) },
                                onUpdateDeadline = { t, dl -> viewModel.updateTaskDeadline(t, dl) },
                                onUpdateTask = { viewModel.updateTask(it) },
                                onDecompose = { viewModel.decomposeTaskWbs(it) },
                                onMicroStep = { viewModel.generateMicroStep(it) },
                                onGenerateDraft = { t, dType -> viewModel.generateDraft(t, dType) },
                                onDelete = { viewModel.deleteTask(it) },
                                shape = shape,
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            } else {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TODOタスクはありません。タスクを追加してください。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }

            // 完了済みタスク (DONE) (Segmented List)
            val doneTasks = uiState.tasks.filter { it.status == TaskStatus.DONE }
            if (doneTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "完了 (${doneTasks.size}件)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        doneTasks.forEachIndexed { index, task ->
                            val shape = getSegmentedShape(index = index, count = doneTasks.size)
                            TaskCardItem(
                                task = task,
                                onStatusAdvance = { viewModel.advanceTaskStatus(it) },
                                onSubTaskToggle = { t, sId, c -> viewModel.toggleSubTask(t, sId, c) },
                                onAddSubTask = { t, title -> viewModel.addSubTask(t, title) },
                                onDeleteSubTask = { t, sId -> viewModel.deleteSubTask(t, sId) },
                                onUpdateDeadline = { t, dl -> viewModel.updateTaskDeadline(t, dl) },
                                onUpdateTask = { viewModel.updateTask(it) },
                                onDecompose = { viewModel.decomposeTaskWbs(it) },
                                onMicroStep = { viewModel.generateMicroStep(it) },
                                onGenerateDraft = { t, dType -> viewModel.generateDraft(t, dType) },
                                onDelete = { viewModel.deleteTask(it) },
                                shape = shape,
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // AI 処理中インジケータ
    if (uiState.isLoadingAi) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = uiState.aiProgressMessage ?: "オンデバイスAI推論中...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        )
    }

    // 日次サマリーダイアログ
    if (uiState.dailySummaryDialogText != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDailySummaryDialog() },
            title = { Text("日次締めサマリー") },
            text = {
                Text(
                    text = uiState.dailySummaryDialogText!!,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(uiState.dailySummaryDialogText!!))
                        viewModel.dismissDailySummaryDialog()
                    }
                ) {
                    Text("コピーして閉じる")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDailySummaryDialog() }) {
                    Text("閉じる")
                }
            }
        )
    }
}

/**
 * SegmentedList用アイテム形状算出ヘルパー
 * 先頭は上部角丸、末尾は下部角丸、中間は矩形（角丸4dp）、単一アイテムは完全角丸
 */
@Composable
fun getSegmentedShape(
    index: Int,
    count: Int,
    outerCornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    innerCornerRadius: androidx.compose.ui.unit.Dp = 4.dp
): androidx.compose.ui.graphics.Shape {
    return when {
        count == 1 -> RoundedCornerShape(outerCornerRadius)
        index == 0 -> RoundedCornerShape(
            topStart = outerCornerRadius,
            topEnd = outerCornerRadius,
            bottomStart = innerCornerRadius,
            bottomEnd = innerCornerRadius
        )
        index == count - 1 -> RoundedCornerShape(
            topStart = innerCornerRadius,
            topEnd = innerCornerRadius,
            bottomStart = outerCornerRadius,
            bottomEnd = outerCornerRadius
        )
        else -> RoundedCornerShape(innerCornerRadius)
    }
}

