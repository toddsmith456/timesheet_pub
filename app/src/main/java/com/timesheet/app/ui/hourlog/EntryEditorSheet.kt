@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.timesheet.app.ui.hourlog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.model.HourEntry
import com.timesheet.app.data.model.Job
import com.timesheet.app.ui.common.ConfirmDialog
import com.timesheet.app.ui.common.EmptyState
import com.timesheet.app.ui.common.SheetLabel
import com.timesheet.app.ui.theme.jobColor
import com.timesheet.app.util.Fmt
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryEditorSheet(
    entryId: Long,
    jobs: List<Job>,
    defaultJobId: Long?,
    onDismiss: () -> Unit,
    onSave: (id: Long, job: Job, date: LocalDate, hours: Double, note: String) -> Unit,
    onDelete: ((HourEntry) -> Unit)?
) {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var existing by remember { mutableStateOf<HourEntry?>(null) }
    var selectedJobId by rememberSaveable { mutableStateOf(defaultJobId ?: -1L) }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var hoursText by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var jobMenuExpanded by remember { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(entryId, jobs) {
        if (entryId != 0L) {
            existing = container.database.hourEntryDao().getById(entryId)
            existing?.let {
                selectedJobId = it.jobId ?: -1L
                date = it.date
                hoursText = Fmt.hours(it.hours)
                note = it.note.orEmpty()
            }
        }
        if (selectedJobId == -1L && jobs.isNotEmpty()) {
            // Default to the job snapshot name match, else the first job.
            selectedJobId = jobs.first().id
        }
    }

    val selectedJob = jobs.find { it.id == selectedJobId }
    val hours = Fmt.parseHours(hoursText)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        if (jobs.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                icon = Icons.Rounded.Work,
                title = "No jobs yet",
                body = "Create a job first — every hour entry belongs to a job."
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
            ) {
                Text(
                    text = if (entryId == 0L) "Log hours" else "Edit entry",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(20.dp))

                // Job selector
                SheetLabel("Job")
                Spacer(Modifier.height(10.dp))
                ExposedDropdownMenuBox(
                    expanded = jobMenuExpanded,
                    onExpandedChange = { jobMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedJob?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        leadingIcon = {
                            if (selectedJob != null) {
                                Spacer(Modifier.width(14.dp))
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(jobColor(selectedJob.colorIndex))
                                )
                            }
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = jobMenuExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = jobMenuExpanded,
                        onDismissRequest = { jobMenuExpanded = false }
                    ) {
                        jobs.forEach { job ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clip(CircleShape)
                                                .background(jobColor(job.colorIndex))
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(job.name)
                                    }
                                },
                                onClick = {
                                    selectedJobId = job.id
                                    jobMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Hours with 0.5 steppers
                SheetLabel("Hours")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledIconButton(
                        onClick = {
                            val current = hours ?: 0.0
                            hoursText = Fmt.hours((current - 0.5).coerceAtLeast(0.0))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Decrease half hour")
                    }
                    Spacer(Modifier.width(10.dp))
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = Fmt.sanitizeHoursInput(it) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    FilledIconButton(
                        onClick = {
                            val current = hours ?: 0.0
                            hoursText = Fmt.hours((current + 0.5).coerceAtMost(24.0))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Increase half hour")
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Date
                SheetLabel("Date")
                Spacer(Modifier.height(10.dp))
                Surface(
                    onClick = { showDatePicker = true },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = Fmt.dateFull(date),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Note
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= 140) note = it },
                    label = { Text("Note (optional)") },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(26.dp))

                Button(
                    onClick = {
                        val job = selectedJob ?: return@Button
                        val value = hours ?: return@Button
                        onSave(entryId, job, date, value, note)
                    },
                    enabled = selectedJob != null && hours != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = if (entryId == 0L) "Save entry" else "Save changes",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (onDelete != null && existing != null) {
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
                            text = "Delete entry",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC)
                .toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Delete this entry?",
            body = "${Fmt.hours(existing?.hours ?: 0.0)} h logged for ${existing?.jobName ?: "this job"} will be removed.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                showDeleteConfirm = false
                existing?.let { onDelete?.invoke(it) }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
