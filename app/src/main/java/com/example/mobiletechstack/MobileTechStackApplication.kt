package com.example.mobiletechstack

import android.app.Application
import com.example.mobiletechstack.data.db.AppDatabase
import com.example.mobiletechstack.data.repository.PatternRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MobileTechStackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getInstance(this@MobileTechStackApplication).libraryPatternDao()
            PatternRepository(this@MobileTechStackApplication, dao).updateFromGitHub()
        }
    }
}