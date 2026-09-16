package com.timesheet.app.alarm

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.prefs.AppSettings
import com.timesheet.app.data.prefs.ThemeMode
import com.timesheet.app.ui.alarm.AlarmScreen
import com.timesheet.app.ui.theme.TimeSheetTheme

/**
 * Full-screen UI shown when a job alarm fires: enter the hours worked and
 * submit — the entry is logged for today instantly. Works over the lock
 * screen and turns the screen on.
 */
class AlarmActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        val jobId = intent.getLongExtra(EXTRA_JOB_ID, -1L)
        if (jobId == -1L) {
            finish()
            return
        }

        setContent {
            val container = remember { application.appContainer() }
            val settings by container.settings.settings
                .collectAsStateWithLifecycle(initialValue = AppSettings.Default)
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            TimeSheetTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
                AlarmScreen(jobId = jobId, settings = settings)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // A different job's alarm fired while this screen was up — reload.
        recreate()
    }

    companion object {
        const val EXTRA_JOB_ID = "extra_job_id"

        fun intent(context: Context, jobId: Long): Intent =
            Intent(context, AlarmActivity::class.java)
                .putExtra(EXTRA_JOB_ID, jobId)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                )
    }
}
