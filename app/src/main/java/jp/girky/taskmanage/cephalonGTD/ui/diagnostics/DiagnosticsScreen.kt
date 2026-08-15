package jp.girky.taskmanage.cephalonGTD.ui.diagnostics

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import jp.girky.taskmanage.cephalonGTD.ai.DiagnosticStatus
import jp.girky.taskmanage.cephalonGTD.ai.DiagnosticStepItem
import jp.girky.taskmanage.cephalonGTD.ui.task.getSegmentedShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: DiagnosticsViewModel = hiltViewModel()
) {
    val steps by viewModel.steps.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini Nano 動作診断") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runDiagnostics() },
                        enabled = !isRunning
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "再テスト"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "オンデバイス AI 逐次診断ステップ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "AICoreの検出からモデル接続、テストプロンプト推論までを順次検証します。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // SegmentedList による逐次表示
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                itemsIndexed(steps) { index, step ->
                    val shape = getSegmentedShape(index = index, count = steps.size)
                    DiagnosticStepCard(
                        step = step,
                        shape = shape
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            FilledTonalButton(
                onClick = { viewModel.runDiagnostics() },
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("診断を実行中...")
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("再度診断を実行する")
                }
            }
        }
    }
}

@Composable
fun DiagnosticStepCard(
    step: DiagnosticStepItem,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier
) {
    // 成功なら SecondaryContainer、失敗なら ErrorContainer、それ以外は surface
    val containerColor = when (step.status) {
        DiagnosticStatus.SUCCESS -> MaterialTheme.colorScheme.secondaryContainer
        DiagnosticStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
        DiagnosticStatus.RUNNING -> MaterialTheme.colorScheme.surfaceVariant
        DiagnosticStatus.PENDING -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }

    val contentColor = when (step.status) {
        DiagnosticStatus.SUCCESS -> MaterialTheme.colorScheme.onSecondaryContainer
        DiagnosticStatus.FAILED -> MaterialTheme.colorScheme.onErrorContainer
        DiagnosticStatus.RUNNING -> MaterialTheme.colorScheme.onSurfaceVariant
        DiagnosticStatus.PENDING -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    }

    Surface(
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // ステータスアイコン (✓ / ✗ / Progress / 待機)
            when (step.status) {
                DiagnosticStatus.SUCCESS -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "成功",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                DiagnosticStatus.FAILED -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "失敗",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
                DiagnosticStatus.RUNNING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                DiagnosticStatus.PENDING -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    ) {}
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f)
                )
                if (step.detailMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = step.detailMessage,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (step.status == DiagnosticStatus.FAILED) MaterialTheme.colorScheme.error else contentColor
                    )
                }
            }
        }
    }
}
