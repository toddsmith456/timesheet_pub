@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.timesheet.app.ui.jobs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.model.DayMask
import com.timesheet.app.data.model.Job
import com.timesheet.app.ui.common.AppTimePickerDialog
import com.timesheet.app.ui.common.ColorPickerRow
import com.timesheet.app.ui.common.ConfirmDialog
import com.timesheet.app.ui.common.DaysRow
import com.timesheet.app.ui.common.SheetLabel
import com.timesheet.app.util.Fmt
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobEditorSheet(
    jobId: Long,
    onDismiss: () -> Unit,
    onSave: (name: String, colorIndex: Int, hour: Int, minute: Int, repeatMask: Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var existing by remember { mutableStateOf<Job?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var colorIndex by rememberSaveable { mutableStateOf(0) }
    var hour by rememberSaveable { mutableStateOf(17) }
    var minute by rememberSaveable { mutableStateOf(0) }
    var repeatMask by rememberSaveable { mutableStateOf(DayMask.WEEKDAYS) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        existing = if (jobId != 0L) container.database.jobDao().getById(jobId) else null
        existing?.let {
            name = it.name
            colorIndex = it.colorIndex
            hour = it.hour
            minute = it.minute
            repeatMask = it.repeatDaysMask
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
        ) {
            Text(
                text = if (jobId == 0L) "New job" else "Edit job",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Every job has its own reminder alarm.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 40) name = it },
                label = { Text("Job or project name") },
                placeholder = { Text("e.g. Acme redesign") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(22.dp))

            SheetLabel("Color")
            Spacer(Modifier.height(10.dp))
            ColorPickerRow(selected = colorIndex, onSelect = { colorIndex = it })
            Spacer(Modifier.height(22.dp))

            SheetLabel("Alarm time")
            Spacer(Modifier.height(10.dp))
            Surface(
                onClick = { showTimePicker = true },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = Fmt.time(hour, minute),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            SheetLabel("Repeat")
            Spacer(Modifier.height(10.dp))
            DaysRow(
                activeMask = repeatMask,
                onToggleDay = { day -> repeatMask = DayMask.toggle(repeatMask, day) }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (repeatMask == 0) {
                    "Fires once at the next matching time, then stops."
                } else {
                    Fmt.repeatLabel(repeatMask)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(28.dp))

            Button(
                onClick = { onSave(name.trim(), colorIndex, hour, minute, repeatMask) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = if (jobId == 0L) "Create job" else "Save changes",
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Delete job",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initial = LocalTime.of(hour, minute),
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                hour = time.hour
                minute = time.minute
                showTimePicker = false
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete this job?",
            body = "Its alarm will stop ringing. Hours you already logged are kept in the hour log.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                onDelete?.invoke()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
