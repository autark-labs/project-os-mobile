package com.projectos.project_os_mobile.connection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectOsConnectionTest {
    @Test
    fun normalizeBaseUrlTrimsWhitespaceAndTrailingSlashes() {
        assertEquals(
            "http://10.0.2.2:8082",
            normalizeProjectOsBaseUrl("  http://10.0.2.2:8082///  ")
        )
    }

    @Test
    fun normalizeBaseUrlRejectsNonHttpUrls() {
        assertNull(normalizeProjectOsBaseUrl("ftp://project-os.local:8082"))
        assertNull(normalizeProjectOsBaseUrl("project-os.local:8082"))
        assertNull(normalizeProjectOsBaseUrl(""))
    }

    @Test
    fun deepLinkParserExtractsEncodedBaseUrl() {
        assertEquals(
            "http://10.0.2.2:8082",
            projectOsBaseUrlFromDeepLink("projectosmobile://connect?baseUrl=http%3A%2F%2F10.0.2.2%3A8082")
        )
    }

    @Test
    fun deepLinkParserRejectsWrongSchemeOrMissingBaseUrl() {
        assertNull(projectOsBaseUrlFromDeepLink("https://connect?baseUrl=http%3A%2F%2F10.0.2.2%3A8082"))
        assertNull(projectOsBaseUrlFromDeepLink("projectosmobile://connect"))
    }
}
