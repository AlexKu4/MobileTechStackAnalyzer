package com.example.mobiletechstack

import android.app.Application
import timber.log.Timber

class MobileTechStackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}