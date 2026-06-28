package com.projectos.project_os_mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.projectos.project_os_mobile.client.App
import com.projectos.project_os_mobile.client.AppsFetchResult
import com.projectos.project_os_mobile.client.fetchApps
import com.projectos.project_os_mobile.connection.ProjectOsConnection
import com.projectos.project_os_mobile.tailscale.ProjectOsAccessStatus
import com.projectos.project_os_mobile.tailscale.TailscaleAccessSummary
import com.projectos.project_os_mobile.tailscale.TailscaleAccessTone
import com.projectos.project_os_mobile.tailscale.fetchProjectOsAccessStatus
import com.projectos.project_os_mobile.tailscale.rememberTailscaleAppController
import com.projectos.project_os_mobile.tailscale.tailscaleAccessSummary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppsPage(modifier: Modifier) {
    var services by remember { mutableStateOf<List<ServiceCardModel>>(emptyList()) }
    var accessStatus by remember { mutableStateOf<ProjectOsAccessStatus?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ServiceFilter.All) }
    var selectedService by remember { mutableStateOf<ServiceCardModel?>(null) }
    var refreshTick by remember { mutableStateOf(0) }
    val baseUrl = ProjectOsConnection.baseUrl
    val snackbarHostState = remember { SnackbarHostState() }
    val tailscaleAppController = rememberTailscaleAppController()
    val tailscaleSummary = tailscaleAccessSummary(accessStatus, tailscaleAppController.isInstalled)

    LaunchedEffect(refreshTick, baseUrl) {
        isLoading = true
        errorMessage = null
        when (val result = fetchApps(baseUrl)) {
            is AppsFetchResult.Success -> services = result.apps.map { it.toServiceCardModel(baseUrl) }
            is AppsFetchResult.Failure -> errorMessage = result.message
        }
        accessStatus = runCatching { fetchProjectOsAccessStatus(baseUrl) }.getOrNull()
        isLoading = false
    }

    LaunchedEffect(baseUrl) {
        while (true) {
            delay(30_000)
            refreshTick++
        }
    }

    val filteredServices = remember(services, searchQuery, selectedFilter) {
        filterServiceCards(services, searchQuery, selectedFilter)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ScreenTop, ScreenBottom)))
            .padding(horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        ServicesHeader(baseUrl = baseUrl, onRefresh = { refreshTick++ })
        SummaryCards(services)
        TailscaleAccessBanner(
            summary = tailscaleSummary,
            onAction = {
                if (tailscaleAppController.isInstalled) {
                    tailscaleAppController.openTailscale()
                } else {
                    tailscaleAppController.openInstallPage()
                }
            },
        )
        SearchAndFilters(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            onFilterSelected = { selectedFilter = it },
            allCount = services.size,
            onlineCount = services.count { it.status == ServiceStatus.Online },
            offlineCount = services.count { it.status != ServiceStatus.Online },
            onClearFilters = {
                searchQuery = ""
                selectedFilter = ServiceFilter.All
            },
        )

        when {
            isLoading -> StatePanelSlot { LoadingPanel() }
            errorMessage != null -> StatePanelSlot { ErrorPanel(errorMessage.orEmpty(), onRetry = { refreshTick++ }) }
            services.isEmpty() -> StatePanelSlot { EmptyPanel("No installed services were returned by Project-os.") }
            filteredServices.isEmpty() -> StatePanelSlot { EmptyPanel("No services match the current search and filter.") }
            else -> ServicesList(
                services = filteredServices,
                modifier = Modifier.weight(1f),
                onOpenError = { message -> snackbarHostState.showSnackbar(message) },
                onServiceSelected = { selectedService = it },
            )
        }
        SnackbarHost(hostState = snackbarHostState)
    }

    selectedService?.let { service ->
        ServiceDetailSheet(
            service = service,
            onDismiss = { selectedService = null },
            onOpenError = { message -> snackbarHostState.showSnackbar(message) },
        )
    }
}

@Composable
private fun ServicesHeader(baseUrl: String, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "My Services",
                color = Graphite,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Monitor and manage your Project-os services",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = baseUrl,
                color = MutedText.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        ElevatedButton(
            onClick = onRefresh,
            modifier = Modifier.size(36.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.elevatedButtonColors(containerColor = Color.White, contentColor = Cobalt),
        ) {
            Text("R", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SummaryCards(services: List<ServiceCardModel>) {
    val installedCount = services.size
    val unhealthyCount = services.count { it.status != ServiceStatus.Online }
    val healthLabel = when {
        services.isEmpty() -> "Waiting"
        unhealthyCount == 0 -> "Excellent"
        unhealthyCount == 1 -> "Review"
        else -> "Attention"
    }
    val healthDetail = when {
        services.isEmpty() -> "No services loaded"
        unhealthyCount == 0 -> "All services healthy"
        else -> "$unhealthyCount service${if (unhealthyCount == 1) "" else "s"} need review"
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SummaryCard(
            modifier = Modifier.weight(1f),
            iconText = "4",
            title = "Installed Apps",
            value = installedCount.toString(),
            detail = "All systems go",
            accent = Cobalt,
        )
        SummaryCard(
            modifier = Modifier.weight(1f),
            iconText = "H",
            title = "System Health",
            value = healthLabel,
            detail = healthDetail,
            accent = if (unhealthyCount == 0 && services.isNotEmpty()) OnlineGreen else WarningOrange,
        )
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier,
    iconText: String,
    title: String,
    value: String,
    detail: String,
    accent: Color,
) {
    ElevatedCard(
        modifier = modifier.height(74.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(11.dp)).background(accent.copy(alpha = 0.11f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconText, color = accent, fontWeight = FontWeight.ExtraBold)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(title, color = MutedText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Text(value, color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(detail, color = MutedText, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun TailscaleAccessBanner(summary: TailscaleAccessSummary, onAction: () -> Unit) {
    val accent = when (summary.tone) {
        TailscaleAccessTone.Ready -> OnlineGreen
        TailscaleAccessTone.Info -> Cobalt
        TailscaleAccessTone.Warning -> WarningOrange
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(10.dp).clip(CircleShape).background(accent),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = summary.title,
                    color = Graphite,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = summary.detail,
                    color = MutedText,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onAction,
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.copy(alpha = 0.12f), contentColor = accent),
            ) {
                Text(summary.actionLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SearchAndFilters(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: ServiceFilter,
    onFilterSelected: (ServiceFilter) -> Unit,
    allCount: Int,
    onlineCount: Int,
    offlineCount: Int,
    onClearFilters: () -> Unit,
) {
    val hasActiveFilters = query.isNotBlank() || selectedFilter != ServiceFilter.All

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f).height(52.dp),
                singleLine = true,
                shape = RoundedCornerShape(15.dp),
                placeholder = { Text("Search services...") },
            )
            ElevatedButton(
                onClick = onClearFilters,
                enabled = hasActiveFilters,
                modifier = Modifier.height(42.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.elevatedButtonColors(containerColor = Color.White, contentColor = Slate),
            ) {
                Text(if (hasActiveFilters) "Clear" else "Filter")
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            Row(modifier = Modifier.padding(2.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                ServiceFilter.entries.forEach { filter ->
                    FilterTab(
                        modifier = Modifier.weight(1f),
                        filter = filter,
                        count = when (filter) {
                            ServiceFilter.All -> allCount
                            ServiceFilter.Online -> onlineCount
                            ServiceFilter.Offline -> offlineCount
                        },
                        selected = selectedFilter == filter,
                        onClick = { onFilterSelected(filter) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterTab(modifier: Modifier, filter: ServiceFilter, count: Int, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Cobalt else Color.Transparent)
            .clickable(onClick = onClick)
                    .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filter != ServiceFilter.All) {
                Box(
                    modifier = Modifier.size(8.dp).clip(CircleShape)
                        .background(if (filter == ServiceFilter.Online) OnlineGreen else OfflineRed)
                )
            }
            Text(
                text = "${filter.label} $count",
                color = if (selected) Color.White else MutedText,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ServicesList(
    services: List<ServiceCardModel>,
    modifier: Modifier = Modifier,
    onOpenError: suspend (String) -> Unit,
    onServiceSelected: (ServiceCardModel) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(services, key = { it.id }) { service ->
            ServiceCard(service, onOpenError, onServiceSelected)
        }
    }
}

@Composable
private fun ServiceCard(
    service: ServiceCardModel,
    onOpenError: suspend (String) -> Unit,
    onServiceSelected: (ServiceCardModel) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable { onServiceSelected(service) },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ServiceIcon(service)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        service.name,
                        color = Graphite,
                    style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                StatusChip(service.status)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    service.url.ifBlank { "No service link configured" },
                    modifier = Modifier.weight(1f),
                    color = if (service.url.isBlank()) MutedText else Cobalt,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Button(
                    onClick = {
                        val result = runCatching { uriHandler.openUri(service.url) }
                        if (result.isFailure) {
                            scope.launch {
                                onOpenError("Could not open ${service.name}. Check that the service URL is valid.")
                            }
                        }
                    },
                    enabled = service.url.isNotBlank(),
                    modifier = Modifier.height(30.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 9.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Cobalt),
                ) {
                    Text("Open", style = MaterialTheme.typography.labelMedium)
                }
            }
            if (service.hasDetailedMetrics) {
                HorizontalDivider(color = Border)
                MetricsRow(service)
            }
        }
    }
}

@Composable
private fun ServiceIcon(service: ServiceCardModel) {
    Box(
        modifier = Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(service.status.accent.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        CachedServiceIcon(
            iconUrl = service.iconUrl,
            cacheKey = service.id,
            fallbackText = service.name.firstOrNull()?.uppercase() ?: "?",
            tint = service.status.accent,
            modifier = Modifier.size(27.dp),
        )
    }
}

@Composable
private fun StatusChip(status: ServiceStatus) {
    Surface(
        shape = RoundedCornerShape(50),
        color = status.accent.copy(alpha = 0.10f),
        contentColor = status.accent,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(status.accent))
            Text(status.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MetricsRow(service: ServiceCardModel) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Metric("Health", service.healthLabel, service.status.accent)
        MetricDivider()
        Metric("CPU", service.cpuLabel, Slate)
        MetricDivider()
        Metric("Mem", service.memoryLabel, Slate)
    }
}

@Composable
private fun Metric(label: String, value: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
        Text(label, color = MutedText, style = MaterialTheme.typography.labelMedium)
        Text(value, color = Graphite, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MetricDivider() {
    Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(Border))
}

@Composable
private fun ColumnScope.StatePanelSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        content()
    }
}

@Composable
private fun LoadingPanel() {
    StatePanel(title = "Loading services", body = "Checking your Project-os instance.") {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Cobalt)
    }
}

@Composable
private fun ErrorPanel(message: String, onRetry: () -> Unit) {
    StatePanel(title = "Could not load services", body = message) {
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = Cobalt)) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyPanel(message: String) {
    StatePanel(title = "Nothing to show", body = message)
}

@Composable
private fun StatePanel(title: String, body: String, action: @Composable (() -> Unit)? = null) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = Graphite, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(body, color = MutedText, style = MaterialTheme.typography.bodyMedium)
            action?.invoke()
        }
    }
}

internal data class ServiceCardModel(
    val id: String,
    val name: String,
    val category: String,
    val url: String,
    val iconUrl: String,
    val status: ServiceStatus,
    val healthLabel: String,
    val cpuLabel: String,
    val memoryLabel: String,
    val source: App,
) {
    val hasDetailedMetrics: Boolean
        get() = cpuLabel != "Unavailable" || memoryLabel != "Unavailable"
}

internal enum class ServiceFilter(val label: String) {
    All("All"),
    Online("Online"),
    Offline("Offline"),
}

internal fun filterServiceCards(
    services: List<ServiceCardModel>,
    searchQuery: String,
    selectedFilter: ServiceFilter,
): List<ServiceCardModel> {
    return services.filter { service ->
        val matchesQuery = searchQuery.isBlank() ||
            service.name.contains(searchQuery, ignoreCase = true) ||
            service.id.contains(searchQuery, ignoreCase = true) ||
            service.url.contains(searchQuery, ignoreCase = true) ||
            service.category.contains(searchQuery, ignoreCase = true) ||
            service.status.label.contains(searchQuery, ignoreCase = true) ||
            service.healthLabel.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            ServiceFilter.All -> true
            ServiceFilter.Online -> service.status == ServiceStatus.Online
            ServiceFilter.Offline -> service.status != ServiceStatus.Online
        }

        matchesQuery && matchesFilter
    }
}

internal enum class ServiceStatus(val label: String, val accent: Color) {
    Online("Online", OnlineGreen),
    Offline("Offline", OfflineRed),
    Updating("Updating", Cobalt),
    Unhealthy("Unhealthy", WarningOrange),
    Unknown("Unknown", MutedText),
}

private fun App.toServiceCardModel(baseUrl: String): ServiceCardModel {
    val bestUrl = accessRoute?.primaryOpenUrl
        ?: accessRoute?.privateUrl
        ?: observedAccess?.privateUrl
        ?: accessUrl
        ?: ""
    val resolvedIconUrl = image?.toAbsoluteProjectOsUrl(baseUrl).orEmpty()
    val displayStatus = displayStatus()
    val normalizedStatus = displayStatus.toServiceStatus()
    val appTelemetry = telemetry
    return ServiceCardModel(
        id = appId.ifBlank { appName },
        name = appName.ifBlank { appId.ifBlank { "Unknown service" } },
        category = category,
        url = bestUrl,
        iconUrl = resolvedIconUrl,
        status = normalizedStatus,
        healthLabel = displayStatus,
        cpuLabel = appTelemetry?.cpuPercent?.ifBlank { "Unavailable" } ?: "Unavailable",
        memoryLabel = appTelemetry?.memoryUsage?.ifBlank { appTelemetry.memoryPercent } ?: "Unavailable",
        source = this,
    )
}

private fun String.toAbsoluteProjectOsUrl(baseUrl: String): String {
    val trimmed = trim()
    if (trimmed.isBlank()) return ""
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (trimmed.startsWith("/")) return "${baseUrl.trimEnd('/')}$trimmed"
    return "${baseUrl.trimEnd('/')}/$trimmed"
}

private fun App.displayStatus(): String {
    canonicalUserStatus?.takeIf { it.isNotBlank() }?.let { return it }
    if (isPrivateAccessOnlyWarning()) {
        return normalizeDisplayStatus(friendlyStatus.ifBlank { "Ready" })
    }
    healthSnapshot?.status?.takeIf { it.isNotBlank() }?.let { return normalizeDisplayStatus(it) }
    return normalizeDisplayStatus(friendlyStatus)
}

private fun normalizeDisplayStatus(status: String?): String {
    return when {
        status.isNullOrBlank() -> "Starting"
        status == "Stopped" -> "Paused"
        else -> status
    }
}

private fun App.isPrivateAccessOnlyWarning(): Boolean {
    val health = healthSnapshot ?: return false
    if (health.status != "Needs attention") return false
    val appLooksReady = friendlyStatus.isBlank() || friendlyStatus == "Ready"
    val containerLooksReady = health.dockerStatus.isBlank() || health.dockerStatus == "Ready"
    val localAccessWorks = health.localAccessStatus == "reachable" || health.localAccessStatus == "not_configured"
    val privateAccessProblem = health.privateAccessStatus in setOf("missing", "unreachable", "not_configured")
    return appLooksReady && containerLooksReady && localAccessWorks && privateAccessProblem
}

private fun String.toServiceStatus(): ServiceStatus {
    val source = lowercase()
    return when {
        "ready" in source || "healthy" in source || "running" in source -> ServiceStatus.Online
        "starting" in source || "updating" in source || "installing" in source -> ServiceStatus.Updating
        "needs" in source || "unhealthy" in source || "degraded" in source -> ServiceStatus.Unhealthy
        "paused" in source || "stopped" in source || "offline" in source || "unavailable" in source || "missing" in source -> ServiceStatus.Offline
        else -> ServiceStatus.Unknown
    }
}

private val ScreenTop = Color(0xFFFBFCFF)
private val ScreenBottom = Color(0xFFF3F6FA)
private val Graphite = Color(0xFF12182B)
private val Slate = Color(0xFF647087)
private val MutedText = Color(0xFF748096)
private val Border = Color(0xFFE5EAF2)
private val Cobalt = Color(0xFF2F5CC8)
private val OnlineGreen = Color(0xFF2FBF71)
private val OfflineRed = Color(0xFFFF5A45)
private val WarningOrange = Color(0xFFF59E0B)

@Preview
@Composable
fun AppsPagePreview() {
    MaterialTheme {
        AppsPage(modifier = Modifier.padding(5.dp))
    }
}
