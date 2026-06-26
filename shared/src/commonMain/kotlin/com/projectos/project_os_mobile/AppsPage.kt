package com.projectos.project_os_mobile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.projectos.project_os_mobile.client.App
import com.projectos.project_os_mobile.client.fetchApps
import com.projectos.project_os_mobile.shared.Res
import com.projectos.project_os_mobile.shared.compose_multiplatform
import io.ktor.websocket.Frame
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppsPage(onClick: (toPage: String)-> Unit) {
    var showContent by remember { mutableStateOf(false) }

    var appData by remember { mutableStateOf <List<App>>(emptyList()) }

    // Compose states
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect("apps") {
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            appData = fetchApps()
            scope.launch {
                snackbarHostState.showSnackbar(appData.toString())
            }
        }
    }

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(shape = RoundedCornerShape(12.dp), onClick = { onClick("HOME") }) {
            Text("Click me!")
        }
        Text("This be the apps page")

        SnackbarHost(hostState = snackbarHostState)

        AnimatedVisibility(showContent) {
            val greeting = remember { Greeting().greet() }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(painterResource(Res.drawable.compose_multiplatform), null)
                Text("Compose: $greeting")
            }
        }
    }
}