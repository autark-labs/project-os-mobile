package com.projectos.project_os_mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.projectos.project_os_mobile.client.ProjectOsActivityLog
import com.projectos.project_os_mobile.client.StatusFetchResult
import com.projectos.project_os_mobile.client.fetchActivityLogs
import com.projectos.project_os_mobile.client.filterActivityLogs
import com.projectos.project_os_mobile.connection.ProjectOsConnection

@Composable
fun LogsPage(modifier: Modifier = Modifier) {
    var events by remember { mutableStateOf<List<ProjectOsActivityLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var appId by remember { mutableStateOf("") }
    val baseUrl = ProjectOsConnection.baseUrl

    LaunchedEffect(baseUrl, refreshTick) {
        isLoading = true
        errorMessage = null
        when (val result = fetchActivityLogs(baseUrl, limit = 50)) {
            is StatusFetchResult.Success -> events = result.value
            is StatusFetchResult.Failure -> errorMessage = result.message
        }
        isLoading = false
    }

    val filteredEvents = remember(events, search, level, category, appId) {
        filterActivityLogs(events, search, level, category, appId)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LogsTop, LogsBottom)))
            .safeContentPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("Logs", color = LogsGraphite, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Recent Project-os activity in plain language", color = LogsMuted, style = MaterialTheme.typography.bodyMedium)
        }

        LogsFilters(
            search = search,
            onSearchChange = { search = it },
            level = level,
            onLevelChange = { level = it },
            category = category,
            onCategoryChange = { category = it },
            appId = appId,
            onAppIdChange = { appId = it },
            onClear = {
                search = ""
                level = ""
                category = ""
                appId = ""
            },
        )

        when {
            isLoading -> LogsStateCard("Loading activity", "Checking recent Project-os events.")
            errorMessage != null -> LogsStateCard("Could not load Logs", errorMessage.orEmpty(), onRetry = { refreshTick++ })
            events.isEmpty() -> LogsStateCard("No activity", "Project-os has not returned recent activity.")
            filteredEvents.isEmpty() -> LogsStateCard("No matching activity", "Clear filters or try another search.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredEvents, key = { it.id }) { event ->
                    ActivityCard(event)
                }
            }
        }
    }
}

@Composable
private fun LogsFilters(
    search: String,
    onSearchChange: (String) -> Unit,
    level: String,
    onLevelChange: (String) -> Unit,
    category: String,
    onCategoryChange: (String) -> Unit,
    appId: String,
    onAppIdChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = search,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                placeholder = { Text("Search activity...") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SmallFilterField("Level", level, onLevelChange, Modifier.weight(1f))
                SmallFilterField("Category", category, onCategoryChange, Modifier.weight(1f))
                SmallFilterField("App", appId, onAppIdChange, Modifier.weight(1f))
            }
            Button(onClick = onClear, colors = ButtonDefaults.buttonColors(containerColor = LogsCobalt)) {
                Text("Clear filters")
            }
        }
    }
}

@Composable
private fun SmallFilterField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier,
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        label = { Text(label) },
    )
}

@Composable
private fun ActivityCard(event: ProjectOsActivityLog) {
    val accent = when (event.level.lowercase()) {
        "success" -> LogsGreen
        "warning" -> LogsOrange
        "error" -> LogsRed
        else -> LogsCobalt
    }
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(event.level.ifBlank { "info" }, color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                Text(event.category, color = LogsMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(event.title.ifBlank { event.action.ifBlank { "Project-os activity" } }, color = LogsGraphite, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(event.message, color = LogsMuted, style = MaterialTheme.typography.bodySmall)
            Text(listOf(event.appId, event.outcome, event.createdAt).filter { it.isNotBlank() }.joinToString(" | "), color = LogsMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun LogsStateCard(title: String, body: String, onRetry: (() -> Unit)? = null) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = LogsGraphite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, color = LogsMuted, style = MaterialTheme.typography.bodyMedium)
            if (onRetry != null) {
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = LogsCobalt)) {
                    Text("Retry")
                }
            }
        }
    }
}

private val LogsTop = Color(0xFFFBFCFF)
private val LogsBottom = Color(0xFFF3F6FA)
private val LogsGraphite = Color(0xFF12182B)
private val LogsMuted = Color(0xFF748096)
private val LogsCobalt = Color(0xFF2F5CC8)
private val LogsGreen = Color(0xFF2FBF71)
private val LogsOrange = Color(0xFFF59E0B)
private val LogsRed = Color(0xFFE34B3F)
