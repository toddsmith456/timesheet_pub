package com.timesheet.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.timesheet.app.data.appContainer
import com.timesheet.app.data.prefs.AppSettings
import com.timesheet.app.data.prefs.ThemeMode
import com.timesheet.app.ui.hourlog.HourLogScreen
import com.timesheet.app.ui.jobs.JobsScreen
import com.timesheet.app.ui.settings.SettingsScreen
import com.timesheet.app.ui.theme.TimeSheetTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            TimeSheetRoot()
        }
    }
}

@Composable
fun TimeSheetRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = remember { context.appContainer() }
    val settings by container.settings.settings
        .collectAsStateWithLifecycle(initialValue = AppSettings.Default)

    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    TimeSheetTheme(darkTheme = darkTheme, dynamicColor = settings.dynamicColor) {
        HomeScreen()
    }
}

private enum class Tab(val label: String) {
    JOBS("Jobs"),
    HOUR_LOG("Hour Log"),
    SETTINGS("Settings")
}

@Composable
private fun HomeScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Ask for notification permission once on first launch (Android 13+).
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result doesn't change the UI flow */ }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, tab ->
                    val selected = selectedTab == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = index },
                        icon = {
                            val icon = when (tab) {
                                Tab.JOBS -> if (selected) Icons.Rounded.Work else Icons.Outlined.Work
                                Tab.HOUR_LOG -> if (selected) Icons.AutoMirrored.Rounded.ReceiptLong else Icons.AutoMirrored.Outlined.ReceiptLong
                                Tab.SETTINGS -> if (selected) Icons.Rounded.Settings else Icons.Outlined.Settings
                            }
                            Icon(icon, contentDescription = tab.label)
                        },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
        ) {
            Crossfade(targetState = selectedTab, label = "tab") { tab ->
                when (tab) {
                    0 -> JobsScreen()
                    1 -> HourLogScreen()
                    else -> SettingsScreen()
                }
            }
        }
    }
}
