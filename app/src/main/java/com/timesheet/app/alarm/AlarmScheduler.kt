package com.timesheet.app.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.timesheet.app.MainActivity
import com.timesheet.app.data.model.Job
import com.timesheet.app.util.NextAlarm
import java.time.ZonedDateTime

/**
 * Schedules job reminder alarms with [AlarmManager.setAlarmClock] so they fire
 * exactly, survive Doze and show the system alarm icon in the status bar.
 */
class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    /** (Re)schedules the next fire of [job]'s alarm, or cancels it when disabled. */
    fun schedule(job: Job) {
        if (!job.alarmEnabled) {
            cancel(job.id)
            return
        }
        val next = NextAlarm.nextOccurrence(job.repeatDays, job.hour, job.minute, ZonedDateTime.now())
        if (next == null) {
            cancel(job.id)
            return
        }
        val triggerMillis = next.toInstant().toEpochMilli()
        setExact(triggerMillis, alarmIntent(job.id, triggerMillis, isSnooze = false))
    }

    fun scheduleSnooze(jobId: Long, minutes: Int) {
        val triggerMillis = System.currentTimeMillis() + minutes * 60_000L
        setExact(triggerMillis, alarmIntent(jobId, triggerMillis, isSnooze = true))
    }

    fun cancel(jobId: Long) {
        alarmManager.cancel(alarmIntent(jobId, 0L, isSnooze = false))
        alarmManager.cancel(alarmIntent(jobId, 0L, isSnooze = true))
    }

    private fun alarmIntent(jobId: Long, triggerMillis: Long, isSnooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = if (isSnooze) ACTION_SNOOZE_FIRE else ACTION_ALARM_FIRE
            // Fixed data (no trigger time) so cancel() can recreate a matching intent.
            data = Uri.parse(if (isSnooze) "timesheet://snooze/$jobId" else "timesheet://alarm/$jobId")
            putExtra(AlarmReceiver.EXTRA_JOB_ID, jobId)
            putExtra(AlarmReceiver.EXTRA_FIRE_TIME, triggerMillis)
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, isSnooze)
        }
        return PendingIntent.getBroadcast(
            context,
            if (isSnooze) (jobId + SNOOZE_CODE_OFFSET).toInt() else jobId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun setExact(triggerMillis: Long, pendingIntent: PendingIntent) {
        if (canScheduleExactAlarms()) {
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
                pendingIntent
            )
        } else {
            // Rare: user revoked exact-alarm access. Degrade to inexact.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    companion object {
        const val ACTION_ALARM_FIRE = "com.timesheet.app.action.ALARM_FIRE"
        const val ACTION_SNOOZE_FIRE = "com.timesheet.app.action.SNOOZE_FIRE"
        private const val SNOOZE_CODE_OFFSET = 1_000_000L
    }
}
