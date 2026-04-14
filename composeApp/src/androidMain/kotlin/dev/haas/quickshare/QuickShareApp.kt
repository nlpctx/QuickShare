package dev.haas.quickshare

import android.app.Application
import com.posthog.android.PostHogAndroid
import com.posthog.android.PostHogAndroidConfig

class QuickShareApp : Application() {

    companion object {
        const val POSTHOG_API_KEY = "phc_sCjB9GVPTUw1v1wxa57neDkTyX2i16YVjsR0Jk8uEwp"
        const val POSTHOG_HOST = "https://us.i.posthog.com"
    }

    override fun onCreate() {
        super.onCreate()
        dev.haas.quickshare.core.AndroidContext.initialize(this)

        val config = PostHogAndroidConfig(
            apiKey = POSTHOG_API_KEY,
            host = POSTHOG_HOST
        ).apply {
            // Minimal tracking: Disable automatic lifecycle and screen captures
            captureApplicationLifecycleEvents = false
            captureScreenViews = false
        }
        PostHogAndroid.setup(this, config)
    }
}
