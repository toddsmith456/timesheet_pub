@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.timesheet.app.ui.alarm

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timesheet.app.alarm.AlarmService
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.model.HourEntry
import com.timesheet.app.data.model.Job
import com.timesheet.app.data.prefs.AppSettings
import com.timesheet.app.ui.theme.jobColor
import com.timesheet.app.util.Fmt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * The screen the alarm shows: big clock, job chip, hours field with quick
 * picks, and Submit / Snooze / Dismiss. Submitting logs the hours for today.
 */
@Composable
fun AlarmScreen(jobId: Long, settings: AppSettings) {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    val scope = rememberCoroutineScope()
    val activity = context as? Activity

    var job by remember { mutableStateOf<Job?>(null) }
    var hoursText by rememberSaveable { mutableStateOf("") }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    var submitted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1000)
        }
    }
    LaunchedEffect(jobId) {
        job = container.database.jobDao().getById(jobId)
    }

    val hours = Fmt.parseHours(hoursText)
    val jobName = job?.name ?: "Your job"
    val color = jobColor(job?.colorIndex ?: 0)

    fun stopRinging() {
        context.startService(AlarmService.stopIntent(context))
    }

    fun dismiss() {
        stopRinging()
        activity?.finish()
    }

    fun snooze() {
        container.scheduler.scheduleSnooze(jobId, settings.snoozeMinutes)
        stopRinging()
        activity?.finish()
    }

    fun submit() {
        val value = hours ?: return
        if (submitted) return
        submitted = true
        scope.launch {
            container.repository.logHours(
                job = job,
                jobName = jobName,
                colorIndex = job?.colorIndex ?: 0,
                date = now.toLocalDate(),
                hours = value,
                note = null,
                source = HourEntry.SOURCE_ALARM
            )
            stopRinging()
            activity?.finish()
        }
    }

    BackHandler { dismiss() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(0.5f))

        // Live clock
        Text(
            text = Fmt.zonedTime(now),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = Fmt.dateFull(now.toLocalDate()),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.weight(0.8f))

        // Job chip
        Surface(shape = CircleShape, color = color.copy(alpha = 0.14f)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = jobName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Text(
            text = "How many hours did you work?",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = hoursText,
            onValueChange = { hoursText = Fmt.sanitizeHoursInput(it) },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            ),
            placeholder = {
                Text(
                    "0.0",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                )
            },
            suffix = {
                Text(
                    "hrs",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { if (hours != null) submit() }),
            singleLine = true,
            isError = hoursText.isNotBlank() && hours == null,
            supportingText = if (hoursText.isNotBlank() && hours == null) {
                { Text("Enter a number between 0 and 24") }
            } else {
                null
            },
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.width(230.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2.0, 4.0, 6.0, 8.0).forEach { quick ->
                AssistChip(
                    onClick = { hoursText = Fmt.hours(quick) },
                    label = { Text("${Fmt.hours(quick)} h") }
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { submit() },
            enabled = hours != null && !submitted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (hours != null) "Log ${Fmt.hours(hours)} hours" else "Enter hours to submit",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (settings.snoozeEnabled) {
                FilledTonalButton(
                    onClick = { snooze() },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                ) {
                    Icon(Icons.Rounded.Snooze, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Snooze ${settings.snoozeMinutes}m")
                }
            }
            TextButton(
                onClick = { dismiss() },
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            ) {
                Text(
                    "Dismiss",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
