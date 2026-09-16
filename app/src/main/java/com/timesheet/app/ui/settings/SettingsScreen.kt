@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.timesheet.app.ui.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timesheet.app.data.prefs.AlarmStyle
import com.timesheet.app.data.prefs.ThemeMode
import com.timesheet.app.ui.common.ConfirmDialog
import com.timesheet.app.ui.common.SettingsGroup
import com.timesheet.app.ui.common.SettingsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val vm = settingsViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showThemeDialog by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri: Uri? = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        vm.setAlarmTone(uri?.toString())
    }

    val toneTitle = remember(settings.alarmToneUri) {
        val uri = settings.alarmToneUri?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        uri?.let {
            runCatching { RingtoneManager.getRingtone(context, it)?.getTitle(context) }.getOrNull()
        } ?: "Default alarm tone"
    }

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0.0"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Appearance ──────────────────────────────────────────────
            SettingsGroup(title = "Appearance") {
                SettingsRow(
                    icon = Icons.Rounded.DarkMode,
                    title = "Theme",
                    subtitle = when (settings.themeMode) {
                        ThemeMode.SYSTEM -> "Follow system"
                        ThemeMode.LIGHT -> "Light"
                        ThemeMode.DARK -> "Dark"
                    },
                    onClick = { showThemeDialog = true }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon = Icons.Rounded.Palette,
                        title = "Material You colors",
                        subtitle = "Match the app to your wallpaper",
                        trailing = {
                            Switch(
                                checked = settings.dynamicColor,
                                onCheckedChange = { vm.setDynamicColor(it) }
                            )
                        }
                    )
                }
            }

            // ── Alarm ───────────────────────────────────────────────────
            SettingsGroup(title = "Alarm") {
                Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)) {
                    Text(
                        text = "Alarm style",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "How the reminder announces itself",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    val styles = listOf(
                        AlarmStyle.TONE to "Tone",
                        AlarmStyle.VIBRATE to "Vibrate",
                        AlarmStyle.SILENT to "Silent"
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        styles.forEachIndexed { index, (style, label) ->
                            SegmentedButton(
                                selected = settings.alarmStyle == style,
                                onClick = { vm.setAlarmStyle(style) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = styles.size
                                )
                            ) {
                                Text(label)
                            }
                        }
                    }
                }

                if (settings.alarmStyle == AlarmStyle.TONE) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon = Icons.Rounded.MusicNote,
                        title = "Alarm tone",
                        subtitle = toneTitle,
                        onClick = {
                            val picker = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(
                                    RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                                )
                                settings.alarmToneUri?.let {
                                    putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(it))
                                }
                            }
                            runCatching { ringtonePicker.launch(picker) }
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon = Icons.Rounded.Vibration,
                        title = "Vibrate with tone",
                        trailing = {
                            Switch(
                                checked = settings.vibrateWithTone,
                                onCheckedChange = { vm.setVibrateWithTone(it) }
                            )
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Rounded.Snooze,
                    title = "Snooze",
                    subtitle = if (settings.snoozeEnabled) {
                        "Remind again after ${settings.snoozeMinutes} minutes"
                    } else {
                        "Off — only Submit or Dismiss"
                    },
                    trailing = {
                        Switch(
                            checked = settings.snoozeEnabled,
                            onCheckedChange = { vm.setSnoozeEnabled(it) }
                        )
                    }
                )
                if (settings.snoozeEnabled) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SettingsRow(
                        icon = Icons.Rounded.Timer,
                        title = "Snooze duration",
                        subtitle = "${settings.snoozeMinutes} minutes",
                        onClick = { showSnoozeDialog = true }
                    )
                }
            }

            // ── Data ────────────────────────────────────────────────────
            SettingsGroup(title = "Data") {
                SettingsRow(
                    icon = Icons.Rounded.Share,
                    title = "Export CSV",
                    subtitle = "Share your full hour log as a spreadsheet file",
                    onClick = { vm.exportCsv(context) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Rounded.Delete,
                    title = "Clear all data",
                    subtitle = "Delete every job, alarm and hour entry",
                    destructive = true,
                    onClick = { showClearConfirm = true }
                )
            }

            // ── About ───────────────────────────────────────────────────
            SettingsGroup(title = "About") {
                SettingsRow(
                    icon = Icons.Rounded.Info,
                    title = "Time Sheet",
                    subtitle = "Version $versionName · Alarm-driven time tracking"
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon = Icons.Rounded.Description,
                    title = "Open-source licenses",
                    subtitle = "MIT licensed · built with Jetpack Compose & Material 3",
                    onClick = { showLicenses = true }
                )
            }
        }
    }

    if (showThemeDialog) {
        OptionDialog(
            title = "Theme",
            options = listOf(
                "Follow system" to ThemeMode.SYSTEM,
                "Light" to ThemeMode.LIGHT,
                "Dark" to ThemeMode.DARK
            ),
            selected = settings.themeMode,
            onSelect = { vm.setThemeMode(it) },
            onDismiss = { showThemeDialog = false }
        )
    }

    if (showSnoozeDialog) {
        OptionDialog(
            title = "Snooze duration",
            options = listOf(
                "5 minutes" to 5,
                "10 minutes" to 10,
                "15 minutes" to 15,
                "20 minutes" to 20
            ),
            selected = settings.snoozeMinutes,
            onSelect = { vm.setSnoozeMinutes(it) },
            onDismiss = { showSnoozeDialog = false }
        )
    }

    if (showClearConfirm) {
        ConfirmDialog(
            title = "Clear all data?",
            body = "This permanently deletes every job, alarm and hour entry. Consider exporting a CSV first.",
            confirmText = "Delete everything",
            destructive = true,
            onConfirm = {
                showClearConfirm = false
                vm.clearAllData()
            },
            onDismiss = { showClearConfirm = false }
        )
    }

    if (showLicenses) {
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text("Open-source licenses") },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Time Sheet is released under the MIT License.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "It is built entirely with open-source libraries:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Jetpack Compose & Material 3 — Apache 2.0",
                        "Material Symbols icons — Apache 2.0",
                        "AndroidX Core / Activity / Lifecycle — Apache 2.0",
                        "Room — Apache 2.0",
                        "DataStore Preferences — Apache 2.0",
                        "Kotlin Coroutines — Apache 2.0"
                    ).forEach {
                        Text(
                            text = "•  $it",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) { Text("Close") }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }
}

@Composable
private fun <T> OptionDialog(
    title: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                options.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(value)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected == value,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        shape = MaterialTheme.shapes.extraLarge
    )
}
