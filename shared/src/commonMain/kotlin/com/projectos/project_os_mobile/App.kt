package com.projectos.project_os_mobile

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


enum class Routes {
    HOME, APPS
}

@Composable
@Preview
fun App() {
    val navController = rememberNavController()

    MaterialTheme {
        NavHost (navController = navController, startDestination = Routes.HOME.name) {
            composable(Routes.HOME.name)  { HomePage { navController.navigate(it) } }
            composable(Routes.APPS.name)  { Text("Hello!") }
        }
    }
}
