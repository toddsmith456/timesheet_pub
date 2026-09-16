package com.timesheet.app

import android.app.Application
import com.timesheet.app.data.AppContainer

class TimeSheetApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
