package com.timesheet.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.timesheet.app.alarm.AlarmScheduler
import com.timesheet.app.data.db.AppDatabase
import com.timesheet.app.data.model.HourEntry
import com.timesheet.app.data.model.Job
import com.timesheet.app.data.prefs.SettingsRepository
import com.timesheet.app.util.Csv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Single source of truth for jobs + hour entries. Keeps the alarm scheduler in
 * sync with every job mutation so callers never have to think about it.
 */
class TimeSheetRepository(
    private val context: Context,
    private val db: AppDatabase,
    val settings: SettingsRepository,
    private val scheduler: AlarmScheduler
) {
    private val jobDao get() = db.jobDao()
    private val entryDao get() = db.hourEntryDao()

    val jobs: Flow<List<Job>> = jobDao.observeJobs()
    val entries: Flow<List<HourEntry>> = entryDao.observeEntries()

    suspend fun getJob(id: Long): Job? = jobDao.getById(id)

    suspend fun getEntry(id: Long): HourEntry? = entryDao.getById(id)

    /** Inserts (id == 0) or updates a job, then reschedules its alarm. */
    suspend fun saveJob(job: Job) {
        val id = if (job.id == 0L) jobDao.insert(job) else {
            jobDao.update(job)
            job.id
        }
        scheduler.schedule(job.copy(id = id))
    }

    suspend fun deleteJob(job: Job) {
        scheduler.cancel(job.id)
        jobDao.delete(job)
    }

    suspend fun setJobAlarmEnabled(job: Job, enabled: Boolean) {
        val updated = job.copy(alarmEnabled = enabled)
        jobDao.update(updated)
        if (enabled) scheduler.schedule(updated) else scheduler.cancel(job.id)
    }

    suspend fun logHours(
        job: Job?,
        jobName: String,
        colorIndex: Int,
        date: LocalDate,
        hours: Double,
        note: String?,
        source: String
    ): Long = entryDao.insert(
        HourEntry(
            jobId = job?.id,
            jobName = jobName,
            colorIndex = colorIndex,
            dateEpochDay = date.toEpochDay(),
            hours = hours,
            note = note?.takeIf { it.isNotBlank() },
            source = source
        )
    )

    suspend fun saveEntry(entry: HourEntry): Long =
        if (entry.id == 0L) entryDao.insert(entry) else {
            entryDao.update(entry)
            entry.id
        }

    suspend fun deleteEntry(entry: HourEntry) = entryDao.delete(entry)

    suspend fun clearAllData() {
        jobDao.getAllOnce().forEach { scheduler.cancel(it.id) }
        entryDao.deleteAll()
        jobDao.deleteAll()
    }

    /** Reschedules every enabled alarm — used after boot and app updates. */
    suspend fun rescheduleAll() {
        jobDao.getAlarmEnabledJobs().forEach { scheduler.schedule(it) }
    }

    /** Writes the full hour log to a shareable CSV file in the cache dir. */
    suspend fun exportCsv(): Uri = withContext(Dispatchers.IO) {
        val entries = entryDao.getAllOnce()
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        dir.listFiles()?.forEach { old ->
            if (old.name.startsWith("timesheet-") && old.name.endsWith(".csv")) old.delete()
        }
        val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE
        val stampFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val file = File(dir, "timesheet-${LocalDate.now()}.csv")
        file.bufferedWriter().use { writer ->
            writer.write("Date,Job,Hours,Note,Logged at,Source")
            writer.newLine()
            entries.forEach { e ->
                val loggedAt = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(e.loggedAt),
                    ZoneId.systemDefault()
                ).format(stampFmt)
                writer.write(
                    listOf(
                        e.date.format(dateFmt),
                        Csv.escape(e.jobName),
                        String.format(Locale.US, "%.2f", e.hours),
                        Csv.escape(e.note.orEmpty()),
                        loggedAt,
                        e.source
                    ).joinToString(",")
                )
                writer.newLine()
            }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
