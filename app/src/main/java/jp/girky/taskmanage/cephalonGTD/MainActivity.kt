package jp.girky.taskmanage.cephalonGTD

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dagger.hilt.android.AndroidEntryPoint
import jp.girky.taskmanage.cephalonGTD.data.preferences.AuthType
import jp.girky.taskmanage.cephalonGTD.data.preferences.UserPreferencesRepository
import jp.girky.taskmanage.cephalonGTD.security.SecurityManager
import jp.girky.taskmanage.cephalonGTD.ui.auth.AppLockOverlay
import jp.girky.taskmanage.cephalonGTD.ui.navigation.CephalonNavGraph
import jp.girky.taskmanage.cephalonGTD.ui.theme.CephalonGTDTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var securityManager: SecurityManager

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isScreenProtectionEnabled by userPreferencesRepository.screenCaptureProtection.collectAsState(initial = false)
            val authType by userPreferencesRepository.authType.collectAsState(initial = AuthType.NONE)
            val userPin by userPreferencesRepository.userPin.collectAsState(initial = null)

            var isLocked by remember { mutableStateOf(authType != AuthType.NONE) }
            val lifecycleOwner = LocalLifecycleOwner.current

            // ライフサイクル監視: バックグラウンドから復帰した際にロックを再適用
            LaunchedEffect(lifecycleOwner, authType) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        if (authType != AuthType.NONE) {
                            // 生体認証の場合は即時プロンプト表示
                            if (authType == AuthType.BIOMETRIC && isLocked) {
                                securityManager.promptBiometric(
                                    activity = this@MainActivity,
                                    onSuccess = { isLocked = false },
                                    onError = { /* PINやリトライ待機 */ }
                                )
                            }
                        } else {
                            isLocked = false
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
            }

            LaunchedEffect(isScreenProtectionEnabled) {
                securityManager.applyScreenProtection(this@MainActivity, isScreenProtectionEnabled)
            }

            LaunchedEffect(authType) {
                if (authType == AuthType.NONE) {
                    isLocked = false
                } else {
                    if (authType == AuthType.BIOMETRIC) {
                        securityManager.promptBiometric(
                            activity = this@MainActivity,
                            onSuccess = { isLocked = false },
                            onError = { }
                        )
                    }
                }
            }

            CephalonGTDTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CephalonNavGraph()

                        if (isLocked && authType != AuthType.NONE) {
                            AppLockOverlay(
                                authType = authType,
                                savedPin = userPin,
                                onBiometricRequested = {
                                    securityManager.promptBiometric(
                                        activity = this@MainActivity,
                                        onSuccess = { isLocked = false },
                                        onError = { }
                                    )
                                },
                                onUnlocked = {
                                    isLocked = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
