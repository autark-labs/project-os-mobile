package com.projectos.project_os_mobile.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.projectos.project_os_mobile.AppsPage
import com.projectos.project_os_mobile.HomePage
import com.projectos.project_os_mobile.shared.Res
import com.projectos.project_os_mobile.shared.apps_icon
import com.projectos.project_os_mobile.shared.home_icon
import com.projectos.project_os_mobile.shared.settings_icon
import org.jetbrains.compose.resources.DrawableResource


enum class Routes(val icon: DrawableResource, val label: String) {
    HOME(Res.drawable.home_icon, "Home"),
    APPS(Res.drawable.apps_icon, "Apps"),
    SETTINGS(Res.drawable.settings_icon, "Settings"),
}

val STARTING_PAGE = Routes.APPS

@Composable
fun AppNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost (navController = navController, startDestination = STARTING_PAGE.name, modifier = modifier) {
        composable(Routes.HOME.name)  { HomePage { navController.navigate(it) } }
        composable(Routes.APPS.name)  { AppsPage() }
        composable(Routes.SETTINGS.name)  { Text("Hello Settings!") }
    }
}