package com.projectos.project_os_mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.projectos.project_os_mobile.navigation.AppNavHost
import com.projectos.project_os_mobile.navigation.AppNavigationBar


@Composable
@Preview
fun App() {
    val navController = rememberNavController()

<<<<<<< HEAD


=======
>>>>>>> e8b48acc2d8550d2b11a9999136cbcecfb12eb44
    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                AppNavigationBar()
            }
        ) { contentPadding ->
            AppNavHost(navController, modifier = Modifier.padding(contentPadding))
        }
    }
}
