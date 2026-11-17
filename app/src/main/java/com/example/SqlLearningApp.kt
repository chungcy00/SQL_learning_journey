package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.repository.ContentRepository
import com.example.data.repository.LearningRepository

class AppContainer(application: Application) {
    val database: AppDatabase = AppDatabase.getInstance(application)
    val settingsStore: SettingsDataStore = SettingsDataStore(application)
    val contentRepository: ContentRepository = ContentRepository(application)
    val learningRepository: LearningRepository = LearningRepository(database, settingsStore)
}

class SqlLearningApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
