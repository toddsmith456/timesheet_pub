package com.timesheet.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timesheet.app.data.AppContainer
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.model.Job
import kotlinx.coroutines.launch

/**
 * Fired by [android.app.AlarmManager] when a job reminder triggers.
 * Reschedules the next occurrence, starts the ringing foreground service and
 * launches the full-screen hours-entry UI.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val jobId = intent.getLongExtra(EXTRA_JOB_ID, -1L)
        if (jobId == -1L) return
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)

        val appContext = context.applicationContext
        val container = appContext.appContainer()
        val pendingResult = goAsync()

        container.appScope.launch {
            try {
                val job = container.database.jobDao().getById(jobId) ?: return@launch
                handleAlarmFired(appContext, container, job, isSnooze)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_JOB_ID = "extra_job_id"
        const val EXTRA_FIRE_TIME = "extra_fire_time"
        const val EXTRA_IS_SNOOZE = "extra_is_snooze"
    }
}

/**
 * Shared alarm-fire handling: schedules the next weekly occurrence, then
 * starts the ringing service and the full-screen hours entry screen.
 */
internal suspend fun handleAlarmFired(
    context: Context,
    container: AppContainer,
    job: Job,
    isSnooze: Boolean
) {
    if (!job.alarmEnabled && !isSnooze) return

    if (!isSnooze) {
        if (job.repeatDaysMask != 0) {
            container.scheduler.schedule(job)
        } else {
            // One-time alarm: it just fired, switch it off.
            container.database.jobDao().update(job.copy(alarmEnabled = false))
        }
    }

    val settings = container.settings.current()

    val serviceIntent = Intent(context, AlarmService::class.java).apply {
        putExtra(AlarmService.EXTRA_JOB_ID, job.id)
        putExtra(AlarmService.EXTRA_JOB_NAME, job.name)
        putExtra(AlarmService.EXTRA_COLOR_INDEX, job.colorIndex)
        putExtra(AlarmService.EXTRA_ALARM_STYLE, settings.alarmStyle.name)
        putExtra(AlarmService.EXTRA_TONE_URI, settings.alarmToneUri)
        putExtra(AlarmService.EXTRA_VIBRATE_WITH_TONE, settings.vibrateWithTone)
        putExtra(AlarmService.EXTRA_SNOOZE_ENABLED, settings.snoozeEnabled)
        putExtra(AlarmService.EXTRA_SNOOZE_MINUTES, settings.snoozeMinutes)
    }
    context.startForegroundService(serviceIntent)

    // Direct launch works thanks to the exact-alarm BAL exemption; the
    // notification's full-screen intent is the fallback when it doesn't.
    runCatching {
        context.startActivity(
            AlarmActivity.intent(context, job.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
