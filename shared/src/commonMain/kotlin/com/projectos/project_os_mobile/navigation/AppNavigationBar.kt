package com.projectos.project_os_mobile.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppNavigationBar(navController: NavController){
    var selectedDestination by rememberSaveable { mutableStateOf(STARTING_PAGE) }

    NavigationBar(windowInsets = NavigationBarDefaults.windowInsets) {
        Routes.entries.forEachIndexed { index, route ->
            NavigationBarItem(
                selected = selectedDestination.ordinal == index,
                onClick = {
                    navController.navigate(route = route.name)
                    selectedDestination = route
                },
                icon = {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(route.icon),
                        contentDescription = route.name
                    )
                },
                label = { Text(route.label) }
            )
        }
    }
}