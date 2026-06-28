package com.projectos.project_os_mobile.client

import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
data class ProjectOsSystemSummary(
    val deviceName: String = "",
    val instanceId: String = "",
    val lanUrl: String = "",
    val setup: ProjectOsSummarySection = ProjectOsSummarySection(),
    val docker: ProjectOsSummarySection = ProjectOsSummarySection(),
    val access: ProjectOsSummarySection = ProjectOsSummarySection(),
    val apps: ProjectOsAppsSummary = ProjectOsAppsSummary(),
    val backups: ProjectOsSummarySection = ProjectOsSummarySection(),
    val storage: ProjectOsSummarySection = ProjectOsSummarySection(),
    val issues: List<ProjectOsIssue> = emptyList(),
    val updatedAt: String = "",
)

@Serializable
data class ProjectOsSummarySection(
    val complete: Boolean = false,
    val ready: Boolean = false,
    val status: String = "",
    val state: String = "",
    val mode: String = "",
    val nextStep: String = "",
    val summary: String = "",
)

@Serializable
data class ProjectOsAppsSummary(
    val installed: Int = 0,
    val running: Int = 0,
    val needsAttention: Int = 0,
)

@Serializable
data class ProjectOsRecommendedAction(
    val id: String = "",
    val severity: String = "",
    val title: String = "",
    val body: String = "",
    val primaryAction: ProjectOsAction? = null,
    val secondaryAction: ProjectOsAction? = null,
    val dismissible: Boolean = false,
)

@Serializable
data class ProjectOsActivityLog(
    val id: Long = 0,
    val level: String = "",
    val category: String = "",
    val action: String = "",
    val title: String = "",
    val message: String = "",
    val appId: String = "",
    val outcome: String = "",
    val details: String = "",
    val createdAt: String = "",
)

data class SystemSummaryCard(
    val title: String,
    val value: String,
    val detail: String,
    val tone: String,
)

sealed interface StatusFetchResult<out T> {
    data class Success<T>(val value: T) : StatusFetchResult<T>
    data class Failure(val message: String) : StatusFetchResult<Nothing>
}

suspend fun fetchSystemSummary(baseUrl: String): StatusFetchResult<ProjectOsSystemSummary> {
    return fetchStatusValue("${baseUrl.trimEnd('/')}/api/system-summary")
}

suspend fun fetchRecommendedAction(baseUrl: String): StatusFetchResult<ProjectOsRecommendedAction> {
    return fetchStatusValue("${baseUrl.trimEnd('/')}/api/recommended-action")
}

suspend fun fetchActivityLogs(baseUrl: String, limit: Int = 50): StatusFetchResult<List<ProjectOsActivityLog>> {
    return fetchStatusValue("${baseUrl.trimEnd('/')}/api/activity?limit=$limit")
}

private suspend inline fun <reified T> fetchStatusValue(url: String): StatusFetchResult<T> {
    return try {
        StatusFetchResult.Success(client.get(url).body())
    } catch (error: Exception) {
        StatusFetchResult.Failure(error.message ?: "Project-os could not be reached.")
    }
}

fun filterActivityLogs(
    events: List<ProjectOsActivityLog>,
    search: String,
    level: String,
    category: String,
    appId: String,
): List<ProjectOsActivityLog> {
    val normalizedSearch = search.trim()
    return events.filter { event ->
        val matchesSearch = normalizedSearch.isBlank() ||
            event.title.contains(normalizedSearch, ignoreCase = true) ||
            event.message.contains(normalizedSearch, ignoreCase = true) ||
            event.appId.contains(normalizedSearch, ignoreCase = true) ||
            event.action.contains(normalizedSearch, ignoreCase = true)
        val matchesLevel = level.isBlank() || event.level.equals(level, ignoreCase = true)
        val matchesCategory = category.isBlank() || event.category.equals(category, ignoreCase = true)
        val matchesApp = appId.isBlank() || event.appId.equals(appId, ignoreCase = true)
        matchesSearch && matchesLevel && matchesCategory && matchesApp
    }
}

fun systemSummaryCards(summary: ProjectOsSystemSummary): List<SystemSummaryCard> {
    return listOf(
        SystemSummaryCard("Setup", summary.setup.status.ifBlank { if (summary.setup.complete) "complete" else "pending" }, summary.setup.summary, if (summary.setup.complete) "success" else "warning"),
        SystemSummaryCard("Docker", if (summary.docker.ready) "ready" else "check", summary.docker.summary, if (summary.docker.ready) "success" else "warning"),
        SystemSummaryCard("Access", summary.access.mode.ifBlank { summary.access.state.ifBlank { "unknown" } }, summary.access.summary, if ("ready" in summary.access.mode) "success" else "warning"),
        SystemSummaryCard("Apps", "${summary.apps.installed} installed / ${summary.apps.running} running", "${summary.apps.needsAttention} need attention", if (summary.apps.needsAttention == 0) "success" else "warning"),
        SystemSummaryCard("Backups", summary.backups.state.ifBlank { "unknown" }, summary.backups.summary, if ("needs" in summary.backups.state || "failed" in summary.backups.state) "warning" else "success"),
        SystemSummaryCard("Storage", summary.storage.state.ifBlank { "unknown" }, summary.storage.summary, "info"),
    )
}
