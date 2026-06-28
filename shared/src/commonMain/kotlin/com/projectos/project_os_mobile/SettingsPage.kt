package com.projectos.project_os_mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.projectos.project_os_mobile.connection.ConnectionTestResult
import com.projectos.project_os_mobile.connection.ProjectOsConnection
import com.projectos.project_os_mobile.connection.normalizeProjectOsBaseUrl
import com.projectos.project_os_mobile.connection.testProjectOsConnection
import kotlinx.coroutines.launch

@Composable
fun SettingsPage(modifier: Modifier = Modifier) {
    var draftBaseUrl by remember(ProjectOsConnection.baseUrl) {
        mutableStateOf(ProjectOsConnection.baseUrl)
    }
    val normalizedDraft = normalizeProjectOsBaseUrl(draftBaseUrl)
    val hasChanges = normalizedDraft != null && normalizedDraft != ProjectOsConnection.baseUrl
    val canSave = normalizedDraft != null
    val connectionResult = ProjectOsConnection.lastConnectionResult
    val scope = rememberCoroutineScope()

    fun testConnection(targetUrl: String) {
        ProjectOsConnection.setConnectionResult(ConnectionTestResult.Checking)
        scope.launch {
            ProjectOsConnection.setConnectionResult(testProjectOsConnection(targetUrl))
        }
    }

    Column(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(SettingsTop, SettingsBottom)))
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Settings",
                color = SettingsGraphite,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Connect Project-os-mobile to your Project-os instance.",
                color = SettingsMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "Project-os connection",
                    color = SettingsGraphite,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Enter your Project-os LAN or Tailscale URL. Local emulator testing can use the default Android host URL.",
                    color = SettingsMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
                OutlinedTextField(
                    value = draftBaseUrl,
                    onValueChange = { draftBaseUrl = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Base URL") },
                    placeholder = { Text(ProjectOsConnection.DEFAULT_BASE_URL) },
                    supportingText = {
                        Text(
                            when {
                                draftBaseUrl.isBlank() -> "Enter a Project-os URL."
                                canSave -> "Services will load from ${normalizedDraft}/api/apps"
                                else -> "Enter a URL starting with http:// or https://"
                            }
                        )
                    },
                    isError = draftBaseUrl.isNotBlank() && !canSave,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { ProjectOsConnection.updateBaseUrl(draftBaseUrl) },
                        enabled = hasChanges && canSave,
                        colors = ButtonDefaults.buttonColors(containerColor = SettingsCobalt),
                    ) {
                        Text("Apply")
                    }
                    OutlinedButton(
                        onClick = {
                            normalizedDraft?.let {
                                ProjectOsConnection.updateBaseUrl(it)
                                testConnection(it)
                            }
                        },
                        enabled = canSave && connectionResult != ConnectionTestResult.Checking,
                    ) {
                        Text(if (connectionResult == ConnectionTestResult.Checking) "Checking" else "Test")
                    }
                }
                OutlinedButton(
                    onClick = {
                        ProjectOsConnection.reset()
                        draftBaseUrl = ProjectOsConnection.baseUrl
                    },
                ) {
                    Text("Use emulator default")
                }
            }
        }

        ConnectionStatusCard(result = connectionResult)

        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Current endpoint",
                    color = SettingsMuted,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "${ProjectOsConnection.baseUrl}/api/health",
                    color = SettingsCobalt,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "This app is read-only for now. It stores only the Project-os base URL; no admin token, password, or service credential is stored.",
                    color = SettingsMuted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(result: ConnectionTestResult) {
    val title = when (result) {
        ConnectionTestResult.Idle -> "Connection not tested"
        ConnectionTestResult.Checking -> "Checking Project-os"
        is ConnectionTestResult.Connected -> "Connected"
        is ConnectionTestResult.Failed -> "Connection failed"
    }
    val detail = when (result) {
        ConnectionTestResult.Idle -> "Use Test to verify the current Project-os URL."
        ConnectionTestResult.Checking -> "Calling /api/health..."
        is ConnectionTestResult.Connected -> result.message
        is ConnectionTestResult.Failed -> result.message
    }
    val accent = when (result) {
        is ConnectionTestResult.Connected -> SettingsGreen
        is ConnectionTestResult.Failed -> SettingsRed
        else -> SettingsMuted
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = title,
                color = accent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = detail,
                color = SettingsMuted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private val SettingsTop = Color(0xFFFBFCFF)
private val SettingsBottom = Color(0xFFF3F6FA)
private val SettingsGraphite = Color(0xFF12182B)
private val SettingsMuted = Color(0xFF748096)
private val SettingsCobalt = Color(0xFF2F5CC8)
private val SettingsGreen = Color(0xFF2FBF71)
private val SettingsRed = Color(0xFFE34B3F)
