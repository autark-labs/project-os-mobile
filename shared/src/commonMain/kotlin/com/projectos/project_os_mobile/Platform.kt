package com.projectos.project_os_mobile

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform