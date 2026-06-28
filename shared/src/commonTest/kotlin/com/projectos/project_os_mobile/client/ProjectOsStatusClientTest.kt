package com.projectos.project_os_mobile.client

import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectOsStatusClientTest {
    @Test
    fun activityFilterMatchesSearchLevelCategoryAndApp() {
        val events = listOf(
            ProjectOsActivityLog(
                level = "warning",
                category = "applications",
                title = "Repair started for Memos",
                message = "Private link is not responding.",
                appId = "memos",
            ),
            ProjectOsActivityLog(
                level = "success",
                category = "backups",
                title = "Backup created",
                message = "Vaultwarden restore point is ready.",
                appId = "vaultwarden",
            ),
        )

        val filtered = filterActivityLogs(
            events = events,
            search = "private",
            level = "warning",
            category = "applications",
            appId = "memos",
        )

        assertEquals(listOf("Repair started for Memos"), filtered.map { it.title })
    }

    @Test
    fun activityFilterAllowsBlankFilters() {
        val events = listOf(
            ProjectOsActivityLog(level = "info", category = "system", title = "Setup complete"),
            ProjectOsActivityLog(level = "warning", category = "applications", title = "App needs review"),
        )

        assertEquals(2, filterActivityLogs(events, search = "", level = "", category = "", appId = "").size)
    }

    @Test
    fun systemSummaryCardsUseExistingEndpointSections() {
        val summary = ProjectOsSystemSummary(
            deviceName = "beast",
            setup = ProjectOsSummarySection(summary = "Setup is complete."),
            docker = ProjectOsSummarySection(summary = "Docker is ready."),
            access = ProjectOsSummarySection(mode = "private_ready", summary = "Private access is ready."),
            apps = ProjectOsAppsSummary(installed = 13, running = 11, needsAttention = 0),
            backups = ProjectOsSummarySection(state = "needs_restore_point", summary = "At least one app needs a restore point."),
            storage = ProjectOsSummarySection(state = "unknown", summary = "Storage details are available."),
        )

        val cards = systemSummaryCards(summary)

        assertEquals(
            listOf("Setup", "Docker", "Access", "Apps", "Backups", "Storage"),
            cards.map { it.title },
        )
        assertEquals("13 installed / 11 running", cards.first { it.title == "Apps" }.value)
    }
}
