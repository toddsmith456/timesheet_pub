package com.timesheet.app.ui.jobs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.TimeSheetRepository
import com.timesheet.app.data.model.Job
import com.timesheet.app.util.NextAlarm
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

data class JobUi(
    val job: Job,
    val nextAlarm: ZonedDateTime?
)

data class JobsUiState(
    val loading: Boolean = true,
    val jobs: List<JobUi> = emptyList()
)

class JobsViewModel(private val repository: TimeSheetRepository) : ViewModel() {

    val uiState: StateFlow<JobsUiState> = repository.jobs
        .map { jobs ->
            val now = ZonedDateTime.now()
            JobsUiState(
                loading = false,
                jobs = jobs.map { job ->
                    JobUi(
                        job = job,
                        nextAlarm = if (job.alarmEnabled) {
                            NextAlarm.nextOccurrence(job.repeatDays, job.hour, job.minute, now)
                        } else {
                            null
                        }
                    )
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JobsUiState())

    fun saveJob(
        id: Long,
        name: String,
        colorIndex: Int,
        hour: Int,
        minute: Int,
        repeatMask: Int
    ) {
        viewModelScope.launch {
            val existing = if (id != 0L) repository.getJob(id) else null
            val job = (existing ?: Job(name = name)).copy(
                name = name,
                colorIndex = colorIndex,
                hour = hour,
                minute = minute,
                repeatDaysMask = repeatMask
            )
            repository.saveJob(job)
        }
    }

    fun toggleAlarm(job: Job, enabled: Boolean) {
        viewModelScope.launch { repository.setJobAlarmEnabled(job, enabled) }
    }

    fun deleteJobById(id: Long) {
        viewModelScope.launch {
            repository.getJob(id)?.let { repository.deleteJob(it) }
        }
    }
}

@Composable
fun jobsViewModel(): JobsViewModel {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                JobsViewModel(container.repository) as T
        }
    )
}
