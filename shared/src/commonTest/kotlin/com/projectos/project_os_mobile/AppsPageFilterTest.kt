package com.projectos.project_os_mobile

import com.projectos.project_os_mobile.client.App
import kotlin.test.Test
import kotlin.test.assertEquals

class AppsPageFilterTest {
    @Test
    fun filtersServicesBySearchTextIncludingStatusAndCategory() {
        val services = listOf(
            service("vaultwarden", "Vaultwarden", "Security", "https://vault.tailnet", ServiceStatus.Online),
            service("memos", "Memos", "Productivity", "https://memos.tailnet", ServiceStatus.Unhealthy),
        )

        assertEquals(listOf("Vaultwarden"), filterServiceCards(services, "security", ServiceFilter.All).map { it.name })
        assertEquals(listOf("Memos"), filterServiceCards(services, "unhealthy", ServiceFilter.All).map { it.name })
    }

    @Test
    fun offlineFilterReturnsAnyServiceThatIsNotOnline() {
        val services = listOf(
            service("ready", "Ready", "System", "https://ready.tailnet", ServiceStatus.Online),
            service("starting", "Starting", "System", "https://starting.tailnet", ServiceStatus.Updating),
            service("offline", "Offline", "System", "https://offline.tailnet", ServiceStatus.Offline),
        )

        assertEquals(listOf("Starting", "Offline"), filterServiceCards(services, "", ServiceFilter.Offline).map { it.name })
    }

    private fun service(id: String, name: String, category: String, url: String, status: ServiceStatus): ServiceCardModel {
        return ServiceCardModel(
            id = id,
            name = name,
            category = category,
            url = url,
            iconUrl = "",
            status = status,
            healthLabel = status.label,
            cpuLabel = "Unavailable",
            memoryLabel = "Unavailable",
            source = App(appId = id, appName = name, category = category, accessUrl = url),
        )
    }
}
