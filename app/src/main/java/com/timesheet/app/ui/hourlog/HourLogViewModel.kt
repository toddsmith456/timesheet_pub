package com.timesheet.app.ui.hourlog

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.timesheet.app.data.TimeSheetRepository
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.model.HourEntry
import com.timesheet.app.data.model.Job
import com.timesheet.app.util.shareCsv
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HourLogUiState(
    val loading: Boolean = true,
    val entries: List<HourEntry> = emptyList(),
    val jobs: List<Job> = emptyList(),
    val todayHours: Double = 0.0,
    val weekHours: Double = 0.0,
    val monthHours: Double = 0.0
)

class HourLogViewModel(private val repository: TimeSheetRepository) : ViewModel() {

    val uiState: StateFlow<HourLogUiState> =
        combine(repository.entries, repository.jobs) { entries, jobs ->
            val today = LocalDate.now()
            // Weeks start on Sunday (US convention).
            val weekStart = today.minusDays((today.dayOfWeek.value % 7).toLong())
            HourLogUiState(
                loading = false,
                entries = entries,
                jobs = jobs,
                todayHours = entries
                    .filter { it.dateEpochDay == today.toEpochDay() }
                    .sumOf { it.hours },
                weekHours = entries
                    .filter { it.dateEpochDay in weekStart.toEpochDay()..today.toEpochDay() }
                    .sumOf { it.hours },
                monthHours = entries
                    .filter { it.date.year == today.year && it.date.month == today.month }
                    .sumOf { it.hours }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HourLogUiState())

    fun saveEntry(id: Long, job: Job, date: LocalDate, hours: Double, note: String) {
        viewModelScope.launch {
            val existing = if (id != 0L) repository.getEntry(id) else null
            repository.saveEntry(
                HourEntry(
                    id = id,
                    jobId = job.id,
                    jobName = job.name,
                    colorIndex = job.colorIndex,
                    dateEpochDay = date.toEpochDay(),
                    hours = hours,
                    note = note.trim().ifBlank { null },
                    loggedAt = existing?.loggedAt ?: System.currentTimeMillis(),
                    source = existing?.source ?: HourEntry.SOURCE_MANUAL
                )
            )
        }
    }

    fun deleteEntry(entry: HourEntry) {
        viewModelScope.launch { repository.deleteEntry(entry) }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val uri = repository.exportCsv()
            shareCsv(context, uri)
        }
    }
}

@Composable
fun hourLogViewModel(): HourLogViewModel {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                HourLogViewModel(container.repository) as T
        }
    )
}
