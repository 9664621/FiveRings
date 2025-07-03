package com.macsdev.fiverings

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform