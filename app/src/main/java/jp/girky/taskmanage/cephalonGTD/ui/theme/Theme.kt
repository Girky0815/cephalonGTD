package jp.girky.taskmanage.cephalonGTD.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

private val DarkColorScheme = darkColorScheme(
  primary = Purple80,
  secondary = PurpleGrey80,
  tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
  primary = Purple40,
  secondary = PurpleGrey40,
  tertiary = Pink40
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CephalonGTDTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  seedColor: Color = Color.Unspecified,
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  blackTheme: Boolean = false,
  content: @Composable () -> Unit
) {
  val defaultColorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as Activity).window
      WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }
  }

  val effectiveSeedColor = if (seedColor != Color.Unspecified) {
    seedColor
  } else {
    defaultColorScheme.primary
  }

  val dynamicColorScheme = rememberDynamicColorScheme(
    seedColor = effectiveSeedColor,
    isDark = darkTheme,
    specVersion = if (blackTheme && darkTheme) ColorSpec.SpecVersion.SPEC_2021 else ColorSpec.SpecVersion.SPEC_2025,
    isAmoled = blackTheme && darkTheme
  )

  val colorScheme = if (seedColor == Color.Unspecified && dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !(blackTheme && darkTheme)) {
    defaultColorScheme
  } else {
    dynamicColorScheme
  }

  MaterialExpressiveTheme(
    colorScheme = colorScheme,
    typography = Typography,
    motionScheme = MotionScheme.expressive(),
    content = content
  )
}