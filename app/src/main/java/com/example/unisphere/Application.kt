
package com.example.unisphere

import android.app.Application
import org.osmdroid.config.Configuration
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class UniSphereApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Configuration.getInstance().apply {
            userAgentValue = "UniSphere/1.0 (com.example.unisphere)"
        }
    }
}
