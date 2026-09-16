package com.timesheet.app.data

import android.content.Context
import com.timesheet.app.TimeSheetApp
import com.timesheet.app.alarm.AlarmScheduler
import com.timesheet.app.data.db.AppDatabase
import com.timesheet.app.data.prefs.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Tiny hand-rolled dependency container. Everything is created lazily and
 * lives for the lifetime of the application process.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Scope for work that must outlive a single component (receivers, etc.). */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    val settings: SettingsRepository by lazy { SettingsRepository(appContext) }

    val scheduler: AlarmScheduler by lazy { AlarmScheduler(appContext) }

    val repository: TimeSheetRepository by lazy {
        TimeSheetRepository(appContext, database, settings, scheduler)
    }
}

fun Context.appContainer(): AppContainer =
    (applicationContext as TimeSheetApp).container
