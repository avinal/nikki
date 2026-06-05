package com.avinal.memos.notifications

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.ReminderUnit
import com.avinal.memos.parser.TaskParser
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.todayIn
import kotlinx.datetime.toInstant

object CalendarReminderManager {

    private const val CALENDAR_NAME = "nikki_tasks"
    private const val ACCOUNT_NAME = "nikki"
    private const val ACCOUNT_TYPE = CalendarContract.ACCOUNT_TYPE_LOCAL

    private fun syncAdapterUri(uri: android.net.Uri): android.net.Uri =
        uri.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            .build()

    fun syncTaskReminders(context: Context, memos: List<Memo>) {
        try {
            val cr = context.contentResolver
            val calId = getOrCreateCalendar(context) ?: return
            val defaultTime = readDefaultNotifyTime(context)
            val tz = TimeZone.currentSystemDefault()
            val today = kotlin.time.Clock.System.todayIn(tz)

            val allTasks = memos.flatMap { memo ->
                TaskParser.extractTasks(memo.id, memo.content, memo.tags)
            }

            val activeTaskIds = mutableSetOf<String>()

            allTasks.forEach { task ->
                if (task.isCompleted) return@forEach
                val effectiveDate = task.dueDate ?: if (task.dueTime != null) today else return@forEach
                val effectiveTime = task.dueTime ?: defaultTime

                val startMs = effectiveDate.atTime(effectiveTime).toInstant(tz).toEpochMilliseconds()
                val taskSyncId = task.id

                activeTaskIds.add(taskSyncId)

                val eventValues = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calId)
                    put(CalendarContract.Events.TITLE, task.text)
                    put(CalendarContract.Events.DTSTART, startMs)
                    put(CalendarContract.Events.DTEND, startMs + 1800_000)
                    put(CalendarContract.Events.EVENT_TIMEZONE, tz.id)
                    put(CalendarContract.Events.SYNC_DATA1, taskSyncId)
                    task.priority?.let { put(CalendarContract.Events.SYNC_DATA2, it.toString()) }
                }

                val existingId = findEventBySyncId(context, calId, taskSyncId)
                val eventId = if (existingId != null) {
                    cr.update(
                        syncAdapterUri(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, existingId)),
                        eventValues, null, null,
                    )
                    existingId
                } else {
                    val uri = cr.insert(syncAdapterUri(CalendarContract.Events.CONTENT_URI), eventValues) ?: return@forEach
                    ContentUris.parseId(uri)
                }

                cr.delete(
                    CalendarContract.Reminders.CONTENT_URI,
                    "${CalendarContract.Reminders.EVENT_ID} = ?",
                    arrayOf(eventId.toString()),
                )

                val reminderMinutes = if (task.reminder != null) {
                    when (task.reminder.unit) {
                        ReminderUnit.MIN -> task.reminder.value
                        ReminderUnit.HR -> task.reminder.value * 60
                        ReminderUnit.DAY -> task.reminder.value * 1440
                        ReminderUnit.WEEK -> task.reminder.value * 10080
                    }
                } else 0

                cr.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
                    put(CalendarContract.Reminders.EVENT_ID, eventId)
                    put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                    put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                })
            }

            cleanupStaleEvents(context, calId, activeTaskIds)
        } catch (_: SecurityException) {
        }
    }

    private fun getOrCreateCalendar(context: Context): Long? {
        val cr = context.contentResolver

        cr.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID),
            "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND ${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
            arrayOf(ACCOUNT_NAME, ACCOUNT_TYPE),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }

        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Nikki Tasks")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0xFFEE67A4.toInt())
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 0)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.currentSystemDefault().id)
        }

        val uri = cr.insert(
            CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, ACCOUNT_NAME)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, ACCOUNT_TYPE)
                .build(),
            values,
        )
        return uri?.let { ContentUris.parseId(it) }
    }

    private fun findEventBySyncId(context: Context, calId: Long, syncId: String): Long? {
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID),
            "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.SYNC_DATA1} = ?",
            arrayOf(calId.toString(), syncId),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getLong(0)
        }
        return null
    }

    private fun cleanupStaleEvents(context: Context, calId: Long, activeIds: Set<String>) {
        val cr = context.contentResolver
        cr.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID, CalendarContract.Events.SYNC_DATA1),
            "${CalendarContract.Events.CALENDAR_ID} = ?",
            arrayOf(calId.toString()),
            null,
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
            val syncIdx = cursor.getColumnIndex(CalendarContract.Events.SYNC_DATA1)
            while (cursor.moveToNext()) {
                val syncId = cursor.getString(syncIdx) ?: continue
                if (syncId !in activeIds) {
                    cr.delete(
                        syncAdapterUri(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, cursor.getLong(idIdx))),
                        null, null,
                    )
                }
            }
        }
    }
}
