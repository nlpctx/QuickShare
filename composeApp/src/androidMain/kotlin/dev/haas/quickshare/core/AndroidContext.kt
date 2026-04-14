package dev.haas.quickshare.core

import android.content.Context

object AndroidContext {
    private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun get(): Context {
        return applicationContext ?: throw IllegalStateException("AndroidContext not initialized")
    }
}
