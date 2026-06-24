package com.projectos.project_os_mobile.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.projectos.project_os_mobile.HomePage

enum class Routes(val icon: ImageVector, val label: String) {
    HOME(icon = ), APPS
}

val STARTING_PAGE = Routes.APPS

@Composable
fun AppNavHost(navController: NavHostController, startDestination: Routes, modifier: Modifier = Modifier) {
    NavHost (navController = navController, startDestination = startDestination.name, modifier = modifier) {
        composable(Routes.HOME.name)  { HomePage { navController.navigate(it) } }
        composable(Routes.APPS.name)  { Text("Hello!") }
    }
}