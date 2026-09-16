package com.timesheet.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timesheet.app.data.appContainer
import kotlinx.coroutines.launch

/** Handles the Snooze / Dismiss actions on the ringing notification. */
class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val jobId = intent.getLongExtra(AlarmService.EXTRA_JOB_ID, -1L)
        if (jobId == -1L) return

        val appContext = context.applicationContext
        val container = appContext.appContainer()
        val pendingResult = goAsync()

        container.appScope.launch {
            try {
                when (action) {
                    ACTION_SNOOZE -> {
                        val minutes = container.settings.current().snoozeMinutes
                        container.scheduler.scheduleSnooze(jobId, minutes)
                    }
                }
                appContext.startForegroundService(AlarmService.stopIntent(appContext))
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_DISMISS = "com.timesheet.app.alarm.DISMISS"
        const val ACTION_SNOOZE = "com.timesheet.app.alarm.SNOOZE"
    }
}
