package com.timesheet.app.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.timesheet.app.R
import com.timesheet.app.data.prefs.AlarmStyle

/**
 * Foreground service that owns the ringing lifecycle: loops the alarm tone
 * and/or vibration, holds a wake lock and posts the full-screen-intent
 * notification. Started by [AlarmReceiver]; stopped via [stopIntent].
 *
 * All configuration arrives through intent extras so the service never has to
 * block reading DataStore on the main thread.
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || intent == null) {
            stopEverything()
            return START_NOT_STICKY
        }

        val jobId = intent.getLongExtra(EXTRA_JOB_ID, -1L)
        if (jobId == -1L) {
            stopEverything()
            return START_NOT_STICKY
        }

        ensureChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(intent),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        acquireWakeLock()
        startRinging(intent)
        return START_NOT_STICKY
    }

    private fun stopEverything() {
        // If we were (re)started just to stop — e.g. a notification action after
        // the process died — satisfy the foreground-service contract first.
        runCatching {
            ensureChannel()
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildStoppedNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        }
        stopRinging()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startRinging(intent: Intent) {
        stopRinging()
        val style = intent.getStringExtra(EXTRA_ALARM_STYLE)
            ?.let { runCatching { AlarmStyle.valueOf(it) }.getOrNull() }
            ?: AlarmStyle.TONE
        val vibrateWithTone = intent.getBooleanExtra(EXTRA_VIBRATE_WITH_TONE, true)

        when (style) {
            AlarmStyle.TONE -> {
                val played = playTone(intent.getStringExtra(EXTRA_TONE_URI))
                if (vibrateWithTone || !played) startVibration()
            }
            AlarmStyle.VIBRATE -> startVibration()
            AlarmStyle.SILENT -> Unit
        }
    }

    /** Returns true when the tone actually started playing. */
    private fun playTone(uriString: String?): Boolean {
        val uri: Uri = uriString?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return false
        return runCatching {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmService, uri)
                isLooping = true
                prepare()
                start()
                mediaPlayer = this
            }
        }.isSuccess
    }

    private fun startVibration() {
        val vib = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vib.hasVibrator()) return
        vibrator = vib
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 900, 900), 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vib.vibrate(
                effect,
                android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                    .build()
            )
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(
                effect,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "TimeSheet::AlarmRinging")
            .apply {
                setReferenceCounted(false)
                acquire(WAKE_LOCK_TIMEOUT_MILLIS)
            }
    }

    private fun stopRinging() {
        mediaPlayer?.runCatching { if (isPlaying) stop() }
        mediaPlayer?.release()
        mediaPlayer = null
        vibrator?.runCatching { cancel() }
        vibrator = null
        wakeLock?.runCatching { if (isHeld) release() }
        wakeLock = null
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    private fun ensureChannel() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_alarms),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notification_channel_alarms_desc)
            // The service loops tone/vibration itself — keep the channel silent
            // so nothing doubles up.
            setSound(null, null)
            enableVibration(false)
            enableLights(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(intent: Intent): Notification {
        val jobId = intent.getLongExtra(EXTRA_JOB_ID, -1L)
        val jobName = intent.getStringExtra(EXTRA_JOB_NAME) ?: getString(R.string.app_name)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 10)

        val openIntent = PendingIntent.getActivity(
            this,
            jobId.toInt(),
            AlarmActivity.intent(this, jobId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Time to log your hours — $jobName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .setFullScreenIntent(openIntent, true)

        if (snoozeEnabled) {
            builder.addAction(
                0,
                getString(R.string.snooze_action, snoozeMinutes),
                actionIntent(AlarmActionReceiver.ACTION_SNOOZE, jobId)
            )
        }
        builder.addAction(
            0,
            getString(R.string.dismiss_action),
            actionIntent(AlarmActionReceiver.ACTION_DISMISS, jobId)
        )
        return builder.build()
    }

    private fun buildStoppedNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_alarm)
            .setContentTitle(getString(R.string.app_name))
            .setOngoing(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    private fun actionIntent(action: String, jobId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            this,
            "$action:$jobId".hashCode(),
            Intent(this, AlarmActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_JOB_ID, jobId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    companion object {
        const val CHANNEL_ID = "job_alarms"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.timesheet.app.alarm.STOP"

        const val EXTRA_JOB_ID = "extra_job_id"
        const val EXTRA_JOB_NAME = "extra_job_name"
        const val EXTRA_COLOR_INDEX = "extra_color_index"
        const val EXTRA_ALARM_STYLE = "extra_alarm_style"
        const val EXTRA_TONE_URI = "extra_tone_uri"
        const val EXTRA_VIBRATE_WITH_TONE = "extra_vibrate_with_tone"
        const val EXTRA_SNOOZE_ENABLED = "extra_snooze_enabled"
        const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

        private const val WAKE_LOCK_TIMEOUT_MILLIS = 15 * 60 * 1000L

        fun stopIntent(context: Context): Intent =
            Intent(context, AlarmService::class.java).setAction(ACTION_STOP)
    }
}
