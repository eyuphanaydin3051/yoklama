package com.workhourtracker.di

import android.app.Application
import com.workhourtracker.data.sync.AutoSyncManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class WorkHourTrackerApp : Application() {

    @Inject
    lateinit var autoSyncManager: AutoSyncManager

    override fun onCreate() {
        super.onCreate()
        autoSyncManager.startAutoSync()
    }
}