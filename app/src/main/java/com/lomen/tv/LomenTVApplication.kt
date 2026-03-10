package com.lomen.tv

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class LomenTVApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LomenTVApplication
            private set
    }
}
