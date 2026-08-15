package jp.girky.taskmanage.cephalonGTD

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.AndroidEntryPoint
import jp.girky.taskmanage.cephalonGTD.data.preferences.UserPreferencesRepository
import jp.girky.taskmanage.cephalonGTD.security.SecurityManager
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

            LaunchedEffect(isScreenProtectionEnabled) {
                securityManager.applyScreenProtection(this@MainActivity, isScreenProtectionEnabled)
            }

            CephalonGTDTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    CephalonNavGraph()
                }
            }
        }
    }
}
