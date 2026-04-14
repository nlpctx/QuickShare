package dev.haas.quickshare.core.security

import kotlin.random.Random

object TokenGenerator {
    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    fun generate(length: Int = 12): String {
        return (1..length)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")
    }
}

object RequestValidator {
    fun validate(token: String?, expectedToken: String?): Boolean {
        if (expectedToken == null) return false
        return token == expectedToken
    }
}
