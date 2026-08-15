package jp.girky.taskmanage.cephalonGTD.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import jp.girky.taskmanage.cephalonGTD.ui.settings.SettingsScreen
import jp.girky.taskmanage.cephalonGTD.ui.task.TaskScreen

sealed class Screen(val route: String) {
    data object Tasks : Screen("tasks")
    data object Settings : Screen("settings")
}

@Composable
fun CephalonNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Tasks.route
    ) {
        composable(Screen.Tasks.route) {
            TaskScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
