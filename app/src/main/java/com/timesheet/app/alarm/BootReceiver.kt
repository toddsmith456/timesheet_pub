package com.timesheet.app.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timesheet.app.data.appContainer
import kotlinx.coroutines.launch

/** Restores all job alarms after device reboot or an app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        val appContext = context.applicationContext
        val container = appContext.appContainer()
        val pendingResult = goAsync()

        container.appScope.launch {
            try {
                container.repository.rescheduleAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
