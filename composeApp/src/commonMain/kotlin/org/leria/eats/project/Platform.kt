package org.leria.eats.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform