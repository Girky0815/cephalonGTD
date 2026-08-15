package jp.girky.taskmanage.cephalonGTD.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import jp.girky.taskmanage.cephalonGTD.ai.DraftType
import jp.girky.taskmanage.cephalonGTD.data.model.TaskItem
import jp.girky.taskmanage.cephalonGTD.data.model.TaskStatus
import jp.girky.taskmanage.cephalonGTD.data.model.WbsSubTask

@Composable
fun TopFocusCard(
    task: TaskItem,
    onStatusAdvance: (TaskItem) -> Unit,
    onSubTaskToggle: (TaskItem, String, Boolean) -> Unit,
    onDecompose: (TaskItem) -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "最優先着手 (TOP FOCUS)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(task.contextTag.label, style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    ),
                    border = null
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 大型チェックボックス / アクションボタン
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onStatusAdvance(task) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (task.requiresApproval) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "起案回付"
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "完了"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.requiresApproval) {
                        Text(
                            text = "※決裁フロー対象 (チェックで決裁待ちへ遷移)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // 直近の物理的初動アクション
            val nextAction = task.nextPhysicalActions.firstOrNull { !it.isCompleted }
            if (nextAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = nextAction.isCompleted,
                            onCheckedChange = { onSubTaskToggle(task, nextAction.id, it) },
                            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "次の物理的初動アクション",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = nextAction.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                    onClick = { onDecompose(task) },
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AIでWBS分解する", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCardItem(
    task: TaskItem,
    onStatusAdvance: (TaskItem) -> Unit,
    onSubTaskToggle: (TaskItem, String, Boolean) -> Unit,
    onDecompose: (TaskItem) -> Unit,
    onMicroStep: (TaskItem) -> Unit,
    onGenerateDraft: (TaskItem, DraftType) -> Unit,
    onDelete: (TaskItem) -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp),
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface
) {
    var expanded by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    var showDraftMenu by remember { mutableStateOf(false) }

    Surface(
        shape = shape,
        color = containerColor,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
            // ステータスに応じたチェックボックス/アクションボタン
            when (task.status) {
                TaskStatus.TODO -> {
                    Checkbox(
                        checked = false,
                        onCheckedChange = { onStatusAdvance(task) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            uncheckedColor = if (task.requiresApproval) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                        )
                    )
                }
                TaskStatus.WAITING_APPROVAL -> {
                    IconButton(onClick = { onStatusAdvance(task) }) {
                        Icon(
                            imageVector = Icons.Default.HourglassTop,
                            contentDescription = "決裁完了へ",
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
                TaskStatus.DONE -> {
                    Checkbox(
                        checked = true,
                        onCheckedChange = { onStatusAdvance(task) },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.status == TaskStatus.DONE) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.status == TaskStatus.DONE) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // タグ
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = task.contextTag.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (task.requiresApproval) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = if (task.status == TaskStatus.WAITING_APPROVAL) "決裁待ち" else "要決裁",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (task.isTwoMinuteRule) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "2分ルール",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "約${task.estimatedMinutes}分",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "閉じる" else "開く",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 展開詳細
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, start = 8.dp, end = 8.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // WBS サブタスク一覧
                if (task.nextPhysicalActions.isNotEmpty()) {
                    Text(
                        text = "WBS / 行動ステップ:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    task.nextPhysicalActions.forEach { subTask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = subTask.isCompleted,
                                onCheckedChange = { onSubTaskToggle(task, subTask.id, it) },
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = subTask.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (subTask.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (subTask.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // 下書きドラフト表示エリア
                if (!task.draftContent.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "AI生成ドラフト",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(task.draftContent))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("コピー", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Text(
                                text = task.draftContent,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // アクションボタン列
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { onDecompose(task) },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AI分解", style = MaterialTheme.typography.labelSmall)
                    }

                    OutlinedButton(
                        onClick = { onMicroStep(task) },
                        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("初動5秒", style = MaterialTheme.typography.labelSmall)
                    }

                    Box {
                        OutlinedButton(
                            onClick = { showDraftMenu = true },
                            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ドラフト作成", style = MaterialTheme.typography.labelSmall)
                        }

                        DropdownMenu(
                            expanded = showDraftMenu,
                            onDismissRequest = { showDraftMenu = false }
                        ) {
                            DraftType.entries.forEach { draftType ->
                                DropdownMenuItem(
                                    text = { Text(draftType.displayName) },
                                    onClick = {
                                        showDraftMenu = false
                                        onGenerateDraft(task, draftType)
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { onDelete(task) },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "削除",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
}
