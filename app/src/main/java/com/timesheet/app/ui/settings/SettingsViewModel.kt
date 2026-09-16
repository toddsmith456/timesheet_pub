package com.timesheet.app.ui.settings

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
import com.timesheet.app.data.prefs.AlarmStyle
import com.timesheet.app.data.prefs.AppSettings
import com.timesheet.app.data.prefs.ThemeMode
import com.timesheet.app.util.shareCsv
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: TimeSheetRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.settings.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.settings.setDynamicColor(enabled) }
    }

    fun setAlarmStyle(style: AlarmStyle) {
        viewModelScope.launch { repository.settings.setAlarmStyle(style) }
    }

    fun setAlarmTone(uriString: String?) {
        viewModelScope.launch { repository.settings.setAlarmToneUri(uriString) }
    }

    fun setVibrateWithTone(enabled: Boolean) {
        viewModelScope.launch { repository.settings.setVibrateWithTone(enabled) }
    }

    fun setSnoozeEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.settings.setSnoozeEnabled(enabled) }
    }

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { repository.settings.setSnoozeMinutes(minutes) }
    }

    fun clearAllData() {
        viewModelScope.launch { repository.clearAllData() }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val uri = repository.exportCsv()
            shareCsv(context, uri)
        }
    }
}

@Composable
fun settingsViewModel(): SettingsViewModel {
    val context = LocalContext.current
    val container = remember { context.appContainer() }
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                SettingsViewModel(container.repository) as T
        }
    )
}
