package com.projectos.project_os_mobile.client

import kotlin.test.Test
import kotlin.test.assertEquals

class AppsClientTest {
    @Test
    fun applicationStateMappingUsesManagedStatusAndPrivateOpenUrl() {
        val apps = applicationStateToApps(
            ProjectOsApplicationState(
                managedApps = listOf(
                    ProjectOsManagedApp(
                        appInstanceId = "appinst_vaultwarden",
                        catalogAppId = "vaultwarden",
                        name = "Vaultwarden",
                        category = "Security",
                        icon = "/app-images/vaultwarden.svg",
                        userStatus = "Ready",
                        runtimeState = "running",
                        accessState = "private_ready",
                        localUrl = "http://localhost:8090",
                        privateUrl = "https://vaultwarden.tailnet.ts.net",
                    )
                ),
                runtimeApps = listOf(
                    App(
                        appId = "vaultwarden",
                        appName = "Vaultwarden",
                        accessUrl = "http://localhost:8090",
                        accessRoute = AppAccessRoute(
                            primaryOpenUrl = "http://localhost:8090",
                            localUrl = "http://localhost:8090",
                            privateUrl = "https://vaultwarden.tailnet.ts.net",
                        ),
                        friendlyStatus = "Needs attention",
                    )
                )
            )
        )

        val app = apps.single()
        assertEquals("vaultwarden", app.appId)
        assertEquals("Ready", app.friendlyStatus)
        assertEquals("Ready", app.canonicalUserStatus)
        assertEquals("https://vaultwarden.tailnet.ts.net", app.accessUrl)
        assertEquals("https://vaultwarden.tailnet.ts.net", app.accessRoute?.primaryOpenUrl)
        assertEquals("/app-images/vaultwarden.svg", app.image)
    }

    @Test
    fun applicationStateMappingFallsBackToRuntimeAppsWhenManagedAppsAreMissing() {
        val apps = applicationStateToApps(
            ProjectOsApplicationState(
                managedApps = emptyList(),
                runtimeApps = listOf(
                    App(
                        appId = "actual-budget",
                        appName = "Actual Budget",
                        friendlyStatus = "Ready",
                        accessUrl = "http://localhost:5006",
                    )
                )
            )
        )

        assertEquals("actual-budget", apps.single().appId)
        assertEquals("http://localhost:5006", apps.single().accessUrl)
    }

    @Test
    fun applicationStateMappingCarriesIssuesGuidesAndEventsForDetails() {
        val apps = applicationStateToApps(
            ProjectOsApplicationState(
                managedApps = listOf(
                    ProjectOsManagedApp(
                        catalogAppId = "actual-budget",
                        name = "Actual Budget",
                        userStatus = "Ready",
                        issues = listOf(
                            ProjectOsIssue(
                                severity = "info",
                                title = "Backup not protected",
                                summary = "Create a restore point.",
                            )
                        ),
                    )
                ),
                runtimeApps = listOf(
                    App(
                        appId = "actual-budget",
                        appName = "Actual Budget",
                        usageGuide = AppUsageGuide(
                            headline = "Start a private household budget",
                            setupSteps = listOf("Open Actual Budget."),
                            values = listOf(AppGuideValue(label = "Budget URL", value = "https://budget.tailnet.ts.net")),
                        ),
                        setupGuide = AppSetupGuide(
                            userSteps = listOf("Create or import a budget."),
                            copyableFields = listOf(AppSetupField(label = "Server URL", value = "https://budget.tailnet.ts.net")),
                        ),
                        recentEvents = listOf(
                            AppEvent(type = "private_access_enabled", message = "Private link ready."),
                        ),
                    )
                )
            )
        )

        val app = apps.single()
        assertEquals("Backup not protected", app.issues.single().title)
        assertEquals("Start a private household budget", app.usageGuide?.headline)
        assertEquals("Create or import a budget.", app.setupGuide?.userSteps?.single())
        assertEquals("Private link ready.", app.recentEvents.single().message)
    }
}
