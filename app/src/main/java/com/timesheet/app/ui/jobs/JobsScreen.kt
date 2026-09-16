@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.timesheet.app.ui.jobs

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timesheet.app.data.model.Job
import com.timesheet.app.ui.common.DaysRow
import com.timesheet.app.ui.common.EmptyState
import com.timesheet.app.ui.common.JobColorDot
import com.timesheet.app.util.Fmt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsScreen() {
    val vm = jobsViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()

    // -1 = closed, 0 = new job, >0 = edit existing
    var editorJobId by rememberSaveable { mutableStateOf(-1L) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jobs") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editorJobId = 0L },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New job", fontWeight = FontWeight.SemiBold) }
            )
        }
    ) { padding ->
        if (state.jobs.isEmpty() && !state.loading) {
            EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                icon = Icons.Outlined.Work,
                title = "No jobs yet",
                body = "Add a job for each project you work on and pick when its reminder alarm should ring. When it fires, log your hours in seconds.",
                actionLabel = "Add your first job",
                onAction = { editorJobId = 0L }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ExactAlarmPermissionCard() }
                items(state.jobs, key = { it.job.id }) { item ->
                    JobCard(
                        item = item,
                        onClick = { editorJobId = item.job.id },
                        onToggle = { enabled -> vm.toggleAlarm(item.job, enabled) }
                    )
                }
            }
        }
    }

    if (editorJobId >= 0L) {
        JobEditorSheet(
            jobId = editorJobId,
            onDismiss = { editorJobId = -1L },
            onSave = { name, colorIndex, hour, minute, mask ->
                vm.saveJob(editorJobId, name, colorIndex, hour, minute, mask)
                editorJobId = -1L
            },
            onDelete = if (editorJobId != 0L) {
                {
                    vm.deleteJobById(editorJobId)
                    editorJobId = -1L
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun JobCard(
    item: JobUi,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    val job = item.job
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JobColorDot(
                    index = job.colorIndex,
                    letter = job.name.firstOrNull()?.toString()?.uppercase() ?: "?"
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = job.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = Fmt.repeatLabel(job.repeatDaysMask),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = job.alarmEnabled, onCheckedChange = onToggle)
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = Fmt.time(job.hour, job.minute),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (job.alarmEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(Modifier.weight(1f))
                NextAlarmCaption(item)
            }

            if (job.repeatDaysMask != 0) {
                Spacer(Modifier.height(10.dp))
                DaysRow(activeMask = job.repeatDaysMask, size = 28.dp)
            }
        }
    }
}

@Composable
private fun NextAlarmCaption(item: JobUi) {
    val next = item.nextAlarm
    when {
        !item.job.alarmEnabled -> CaptionChip(
            icon = Icons.Rounded.NotificationsOff,
            text = "Alarm off",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        next == null -> CaptionChip(
            icon = Icons.Rounded.NotificationsOff,
            text = "No upcoming alarm",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        else -> CaptionChip(
            icon = Icons.Rounded.Schedule,
            text = "${Fmt.dayLabel(next)} · ${Fmt.zonedTime(next)}",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CaptionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = tint
        )
    }
}

/** Shown only when the user has revoked exact-alarm access (Android 12+). */
@Composable
private fun ExactAlarmPermissionCard() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val context = LocalContext.current
    var canExact by remember { mutableStateOf(canScheduleExactAlarms(context)) }

    LifecycleResumeEffect(Unit) {
        canExact = canScheduleExactAlarms(context)
        onPauseOrDispose { }
    }
    if (canExact) return

    Card(
        onClick = {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:${context.packageName}")
                    )
                )
            }
        },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "Exact alarms are restricted",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Tap to allow Time Sheet to ring reminders exactly on time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

private fun canScheduleExactAlarms(context: Context): Boolean {
    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        am.canScheduleExactAlarms()
    } else {
        true
    }
}
