package com.projectos.project_os_mobile.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.projectos.project_os_mobile.AppsPage
import com.projectos.project_os_mobile.LogsPage
import com.projectos.project_os_mobile.SettingsPage
import com.projectos.project_os_mobile.SystemPage
import com.projectos.project_os_mobile.shared.Res
import com.projectos.project_os_mobile.shared.apps_icon
import com.projectos.project_os_mobile.shared.home_icon
import com.projectos.project_os_mobile.shared.settings_icon
import org.jetbrains.compose.resources.DrawableResource


enum class Routes(val icon: DrawableResource, val label: String) {
    APPS(Res.drawable.apps_icon, "Services"),
    SYSTEM(Res.drawable.home_icon, "System"),
    LOGS(Res.drawable.apps_icon, "Logs"),
    SETTINGS(Res.drawable.settings_icon, "Settings"),
}

val STARTING_PAGE = Routes.APPS

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost (navController = navController, startDestination = STARTING_PAGE.name, modifier = modifier) {
        composable(Routes.APPS.name)  { AppsPage(Modifier) }
        composable(Routes.SYSTEM.name)  { SystemPage(Modifier) }
        composable(Routes.LOGS.name)  { LogsPage(Modifier) }
        composable(Routes.SETTINGS.name)  { SettingsPage(Modifier) }
    }
}
