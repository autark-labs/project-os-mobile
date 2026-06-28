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
}
