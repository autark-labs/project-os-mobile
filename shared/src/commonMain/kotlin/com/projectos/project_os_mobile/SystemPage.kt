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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectos.project_os_mobile.client.ProjectOsRecommendedAction
import com.projectos.project_os_mobile.client.ProjectOsSystemSummary
import com.projectos.project_os_mobile.client.StatusFetchResult
import com.projectos.project_os_mobile.client.SystemSummaryCard
import com.projectos.project_os_mobile.client.fetchRecommendedAction
import com.projectos.project_os_mobile.client.fetchSystemSummary
import com.projectos.project_os_mobile.client.systemSummaryCards
import com.projectos.project_os_mobile.connection.ProjectOsConnection
import com.projectos.project_os_mobile.tailscale.ProjectOsAccessStatus
import com.projectos.project_os_mobile.tailscale.TailscaleAccessTone
import com.projectos.project_os_mobile.tailscale.fetchProjectOsAccessStatus
import com.projectos.project_os_mobile.tailscale.tailscaleAccessSummary

@Composable
fun SystemPage(modifier: Modifier = Modifier) {
    var summary by remember { mutableStateOf<ProjectOsSystemSummary?>(null) }
    var recommendedAction by remember { mutableStateOf<ProjectOsRecommendedAction?>(null) }
    var accessStatus by remember { mutableStateOf<ProjectOsAccessStatus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }
    val baseUrl = ProjectOsConnection.baseUrl

    LaunchedEffect(baseUrl, refreshTick) {
        isLoading = true
        errorMessage = null
        when (val result = fetchSystemSummary(baseUrl)) {
            is StatusFetchResult.Success -> summary = result.value
            is StatusFetchResult.Failure -> errorMessage = result.message
        }
        recommendedAction = when (val result = fetchRecommendedAction(baseUrl)) {
            is StatusFetchResult.Success -> result.value.takeIf { it.title.isNotBlank() || it.body.isNotBlank() }
            is StatusFetchResult.Failure -> null
        }
        accessStatus = runCatching { fetchProjectOsAccessStatus(baseUrl) }.getOrNull()
        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(SystemTop, SystemBottom)))
            .safeContentPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PageHeader(title = "System", subtitle = "Project-os host, access, and service readiness")

        when {
            isLoading -> SystemStateCard("Loading system", "Checking Project-os system summary.")
            errorMessage != null -> SystemStateCard("Could not load System", errorMessage.orEmpty(), onRetry = { refreshTick++ })
            summary == null -> SystemStateCard("No system summary", "Project-os did not return system data.", onRetry = { refreshTick++ })
            else -> {
                InstanceCard(summary = summary!!, baseUrl = baseUrl)
                accessStatus?.let {
                    val accessSummary = tailscaleAccessSummary(it, mobileAppInstalled = true)
                    val tone = when (accessSummary.tone) {
                        TailscaleAccessTone.Ready -> SystemGreen
                        TailscaleAccessTone.Info -> SystemCobalt
                        TailscaleAccessTone.Warning -> SystemOrange
                    }
                    InfoCard(title = accessSummary.title, value = it.tailscale.hostname.ifBlank { it.mode }, detail = accessSummary.detail, accent = tone)
                }
                recommendedAction?.let { action ->
                    InfoCard(
                        title = action.title.ifBlank { "Recommended action" },
                        value = action.severity.ifBlank { "info" },
                        detail = action.body,
                        accent = if (action.severity == "warning") SystemOrange else SystemCobalt,
                    )
                }
                systemSummaryCards(summary!!).forEach { card ->
                    SummaryStatusCard(card)
                }
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = SystemGraphite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = SystemMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun InstanceCard(summary: ProjectOsSystemSummary, baseUrl: String) {
    InfoCard(
        title = summary.deviceName.ifBlank { "Project-os instance" },
        value = summary.lanUrl.ifBlank { baseUrl },
        detail = "Instance ${summary.instanceId.ifBlank { "unknown" }}",
        accent = SystemCobalt,
    )
}

@Composable
private fun SummaryStatusCard(card: SystemSummaryCard) {
    val accent = when (card.tone) {
        "success" -> SystemGreen
        "warning" -> SystemOrange
        else -> SystemCobalt
    }
    InfoCard(title = card.title, value = card.value, detail = card.detail, accent = accent)
}

@Composable
private fun InfoCard(title: String, value: String, detail: String, accent: Color) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, color = SystemMuted, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(value.ifBlank { "Unknown" }, color = accent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail.ifBlank { "No detail returned." }, color = SystemGraphite, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SystemStateCard(title: String, body: String, onRetry: (() -> Unit)? = null) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = SystemGraphite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, color = SystemMuted, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                Row {
                    Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = SystemCobalt)) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

private val SystemTop = Color(0xFFFBFCFF)
private val SystemBottom = Color(0xFFF3F6FA)
private val SystemGraphite = Color(0xFF12182B)
private val SystemMuted = Color(0xFF748096)
private val SystemCobalt = Color(0xFF2F5CC8)
private val SystemGreen = Color(0xFF2FBF71)
private val SystemOrange = Color(0xFFF59E0B)
