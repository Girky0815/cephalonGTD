package jp.girky.taskmanage.cephalonGTD.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoPhotography
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import jp.girky.taskmanage.cephalonGTD.data.preferences.AuthType
import jp.girky.taskmanage.cephalonGTD.data.preferences.UserPreferencesRepository
import jp.girky.taskmanage.cephalonGTD.security.SecurityManager
import jp.girky.taskmanage.cephalonGTD.ui.task.getSegmentedShape
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import jp.girky.taskmanage.cephalonGTD.ai.AiDiagnosticsResult
import jp.girky.taskmanage.cephalonGTD.ai.AiEngine
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    val securityManager: SecurityManager,
    private val aiEngine: AiEngine
) : ViewModel() {
    val authType: StateFlow<AuthType> = userPreferencesRepository.authType.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthType.NONE
    )

    val screenProtection: StateFlow<Boolean> = userPreferencesRepository.screenCaptureProtection.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _isDiagnosingAi = MutableStateFlow(false)
    val isDiagnosingAi: StateFlow<Boolean> = _isDiagnosingAi

    private val _aiDiagnosticsResult = MutableStateFlow<AiDiagnosticsResult?>(null)
    val aiDiagnosticsResult: StateFlow<AiDiagnosticsResult?> = _aiDiagnosticsResult

    fun runAiDiagnostics(testPrompt: String = "こんにちは。10文字以内で挨拶を返してください。") {
        viewModelScope.launch {
            _isDiagnosingAi.value = true
            try {
                val result = aiEngine.diagnoseGeminiNano(testPrompt)
                _aiDiagnosticsResult.value = result
            } finally {
                _isDiagnosingAi.value = false
            }
        }
    }

    fun setAuthType(type: AuthType) {
        viewModelScope.launch {
            userPreferencesRepository.setAuthType(type)
        }
    }

    fun setUserPin(pin: String?) {
        viewModelScope.launch {
            userPreferencesRepository.setUserPin(pin)
        }
    }

    fun setScreenProtection(enabled: Boolean, activity: Activity) {
        viewModelScope.launch {
            userPreferencesRepository.setScreenCaptureProtection(enabled)
            securityManager.applyScreenProtection(activity, enabled)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val currentAuthType by viewModel.authType.collectAsState()
    val isScreenProtected by viewModel.screenProtection.collectAsState()

    var otaStatusMessage by remember { mutableStateOf<String?>(null) }

    val authOptions = listOf(
        AuthType.NONE to "認証なし (無効)",
        AuthType.BIOMETRIC to "生体認証 / 端末認証",
        AuthType.PIN to "アプリ専用PINコード (Keystore暗号化)"
    )

    var showPinDialog by remember { mutableStateOf(false) }
    var inputPin by remember { mutableStateOf("") }

    if (showPinDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("4桁PINコードの設定") },
            text = {
                Column {
                    Text("アプリ起動時および復帰時のロック解除に使用する4桁のPINを入力してください。", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) inputPin = it },
                        label = { Text("PINコード (4桁数字)") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword,
                            autoCorrectEnabled = false
                        )
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        if (inputPin.length == 4) {
                            viewModel.setUserPin(inputPin)
                            viewModel.setAuthType(AuthType.PIN)
                            showPinDialog = false
                            inputPin = ""
                        }
                    },
                    enabled = inputPin.length == 4
                ) {
                    Text("設定して有効化")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showPinDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("設定", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る"
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // セキュリティ設定セクション
            item {
                Text(
                    text = "セキュリティ & プライバシー",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // 認証方式 (Segmented List)
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // ヘッダー行
                    val headerShape = getSegmentedShape(index = 0, count = authOptions.size + 1)
                    Surface(
                        shape = headerShape,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(headerShape)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "アプリ起動時認証",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // ラジオボタン項目
                    authOptions.forEachIndexed { index, (type, label) ->
                        val shape = getSegmentedShape(index = index + 1, count = authOptions.size + 1)
                        val onSelect = {
                            if (type == AuthType.PIN) {
                                showPinDialog = true
                            } else {
                                viewModel.setAuthType(type)
                            }
                        }
                        Surface(
                            shape = shape,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .clickable { onSelect() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                RadioButton(
                                    selected = currentAuthType == type,
                                    onClick = { onSelect() }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }

            // 画面キャプチャ・録画保護 (Segmented Single Card)
            item {
                val shape = RoundedCornerShape(20.dp)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NoPhotography, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "画面キャプチャ・録画保護",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "機密情報漏洩防止のためスクリーンショットをブロック",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isScreenProtected,
                            onCheckedChange = {
                                if (activity != null) {
                                    viewModel.setScreenProtection(it, activity)
                                }
                            }
                        )
                    }
                }
            }

            // Gemini Nano / AICore オンデバイスAI 診断セクション
            item {
                Text(
                    text = "オンデバイス AI (Gemini Nano / AICore)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                val shape = RoundedCornerShape(20.dp)
                val isDiagnosing by viewModel.isDiagnosingAi.collectAsState()
                val diagnosticsResult by viewModel.aiDiagnosticsResult.collectAsState()

                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Gemini Nano / AICore 動作チェック",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "AICoreの検出、利用可能モデルの確認、テスト推論を実行",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (diagnosticsResult != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (diagnosticsResult!!.isSuccess) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "【診断結果】",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "・AICore パッケージ: ${if (diagnosticsResult!!.isAiCoreInstalled) "インストール済み (${diagnosticsResult!!.aiCoreVersion})" else "未検出 / 未インストール"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "・利用可能モデル: ${diagnosticsResult!!.availableModels.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    if (diagnosticsResult!!.testPromptResult != null) {
                                        Text(
                                            text = "・テスト応答: 「${diagnosticsResult!!.testPromptResult}」",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "・状態: ${diagnosticsResult!!.message}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (diagnosticsResult!!.isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FilledTonalButton(
                            onClick = { viewModel.runAiDiagnostics() },
                            enabled = !isDiagnosing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isDiagnosing) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AICore / Gemini Nano 診断中...")
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gemini Nano 動作チェックを実行")
                            }
                        }
                    }
                }
            }

            // OTA 更新セクション (GitHub Releases連携)
            item {
                Text(
                    text = "OTA アップデート (Noctua Hub準拠)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                val shape = RoundedCornerShape(20.dp)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "GitHub Releases アップデート確認",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "現在のバージョン: v1.0 (Cephalon GTD)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (otaStatusMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = otaStatusMessage!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FilledTonalButton(
                            onClick = {
                                otaStatusMessage = "最新版です (GitHub Releases API は準備完了)"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("更新を確認")
                        }
                    }
                }
            }

            // アプリ情報セクション
            item {
                val shape = RoundedCornerShape(20.dp)
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "完全オンデバイスAI (Zero Cloud Dependency) 仕様。タスクデータやプロンプトは外部へ送信されません。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
