package com.projectos.project_os_mobile

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.projectos.project_os_mobile.connection.AndroidProjectOsConnectionStorage
import com.projectos.project_os_mobile.connection.ConnectionTestResult
import com.projectos.project_os_mobile.connection.ProjectOsConnection
import com.projectos.project_os_mobile.connection.testProjectOsConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ProjectOsConnection.initialize(AndroidProjectOsConnectionStorage(this))
        handleConnectionIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleConnectionIntent(intent)
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun handleConnectionIntent(intent: Intent?) {
        val updated = ProjectOsConnection.updateFromDeepLink(intent?.dataString)
        if (!updated) return

        ProjectOsConnection.setConnectionResult(ConnectionTestResult.Checking)
        activityScope.launch {
            ProjectOsConnection.setConnectionResult(testProjectOsConnection(ProjectOsConnection.baseUrl))
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
