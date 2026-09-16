@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.timesheet.app.ui.hourlog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timesheet.app.data.model.HourEntry
import com.timesheet.app.ui.common.EmptyState
import com.timesheet.app.ui.theme.jobColor
import com.timesheet.app.util.Fmt
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HourLogScreen() {
    val vm = hourLogViewModel()
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var filterJobId by rememberSaveable { mutableStateOf(-1L) }
    // -1 = closed, 0 = new entry, >0 = edit existing
    var editorEntryId by rememberSaveable { mutableStateOf(-1L) }

    val visibleEntries = remember(state.entries, filterJobId) {
        if (filterJobId == -1L) state.entries else state.entries.filter { it.jobId == filterJobId }
    }
    val dayGroups = remember(visibleEntries) {
        visibleEntries.groupBy { it.dateEpochDay }.toSortedMap(reverseOrder())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hour Log") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { vm.exportCsv(context) }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Export CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editorEntryId = 0L }) {
                Icon(Icons.Rounded.Add, contentDescription = "Log hours")
            }
        }
    ) { padding ->
        if (state.entries.isEmpty() && !state.loading) {
            EmptyState(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                title = "No hours logged yet",
                body = "When a job alarm rings and you submit your hours, they show up here day by day. You can also add entries manually.",
                actionLabel = "Log hours manually",
                onAction = { editorEntryId = 0L }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "summary") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SummaryCard(state)
                        if (state.jobs.isNotEmpty()) {
                            FilterRow(
                                state = state,
                                filterJobId = filterJobId,
                                onFilter = { filterJobId = it }
                            )
                        }
                    }
                }
                dayGroups.forEach { (epochDay, dayEntries) ->
                    item(key = "day-$epochDay") {
                        DayCard(
                            date = LocalDate.ofEpochDay(epochDay),
                            entries = dayEntries,
                            onEntryClick = { editorEntryId = it.id }
                        )
                    }
                }
            }
        }
    }

    if (editorEntryId >= 0L) {
        EntryEditorSheet(
            entryId = editorEntryId,
            jobs = state.jobs,
            defaultJobId = if (filterJobId != -1L) filterJobId else null,
            onDismiss = { editorEntryId = -1L },
            onSave = { id, job, date, hours, note ->
                vm.saveEntry(id, job, date, hours, note)
                editorEntryId = -1L
            },
            onDelete = if (editorEntryId != 0L) {
                { entry ->
                    vm.deleteEntry(entry)
                    editorEntryId = -1L
                }
            } else {
                null
            }
        )
    }
}

@Composable
private fun SummaryCard(state: HourLogUiState) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryStat(Modifier.weight(1f), "Today", state.todayHours)
            VerticalDivider(Modifier.height(38.dp))
            SummaryStat(Modifier.weight(1f), "This week", state.weekHours)
            VerticalDivider(Modifier.height(38.dp))
            SummaryStat(Modifier.weight(1f), "This month", state.monthHours)
        }
    }
}

@Composable
private fun SummaryStat(modifier: Modifier, label: String, hours: Double) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Fmt.hours(hours),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(3.dp))
            Text(
                text = "h",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilterRow(
    state: HourLogUiState,
    filterJobId: Long,
    onFilter: (Long) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = filterJobId == -1L,
            onClick = { onFilter(-1L) },
            label = { Text("All jobs") }
        )
        state.jobs.forEach { job ->
            FilterChip(
                selected = filterJobId == job.id,
                onClick = { onFilter(if (filterJobId == job.id) -1L else job.id) },
                label = { Text(job.name, maxLines = 1) },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(jobColor(job.colorIndex))
                    )
                }
            )
        }
    }
}

@Composable
private fun DayCard(
    date: LocalDate,
    entries: List<HourEntry>,
    onEntryClick: (HourEntry) -> Unit
) {
    val total = entries.sumOf { it.hours }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = Fmt.dateFull(date),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${entries.size} ${if (entries.size == 1) "entry" else "entries"}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${Fmt.hours(total)} h",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            entries.forEach { entry ->
                EntryRow(entry = entry, onClick = { onEntryClick(entry) })
            }
        }
    }
}

@Composable
private fun EntryRow(entry: HourEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(jobColor(entry.colorIndex))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = entry.jobName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!entry.note.isNullOrBlank()) {
                Text(
                    text = entry.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${Fmt.hours(entry.hours)} h",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
    }
}
