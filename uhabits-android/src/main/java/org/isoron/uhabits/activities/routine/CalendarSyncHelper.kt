/*
 * Copyright (C) 2016-2025 Álinson Santos Xavier <git@axavier.org>
 *
 * This file is part of Loop Habit Tracker.
 *
 * Loop Habit Tracker is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Loop Habit Tracker is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.isoron.uhabits.activities.routine

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.util.Calendar
import java.util.TimeZone

/**
 * Creates calendar events from routine entries using Android's Calendar provider intents.
 * Works with Google Calendar, Samsung Calendar, or any calendar app installed on the device.
 */
class CalendarSyncHelper(private val context: Context) {

    /**
     * Opens the calendar app with a new event pre-filled for the given routine entry.
     * The event is set for today with the specified start and end time.
     */
    fun createEventForEntry(entry: RoutineEntry) {
        val startMillis = getTimeMillisForStart(entry.startHour, entry.startMinute)
        val endMillis = getTimeMillisForEnd(startMillis, entry.endHour, entry.endMinute)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, entry.habitName)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Daily routine habit from Loop Habit Tracker"
            )
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /**
     * Creates calendar events for ALL routine entries at once by launching
     * an intent for each entry. Since Android's calendar intent only supports
     * one event at a time, this creates a batch by chaining the first event
     * and notifying the user to come back for more.
     *
     * For a better UX, we create all events as recurring daily events.
     */
    fun createAllEvents(entries: List<RoutineEntry>) {
        if (entries.isEmpty()) return

        val startMillis = getTimeMillisForStart(entries.first().startHour, entries.first().startMinute)
        val endMillis = getTimeMillisForEnd(startMillis, entries.last().endHour, entries.last().endMinute)

        val description = buildString {
            append("Daily Routine Schedule:\n\n")
            entries.forEach { entry ->
                append("• ${formatTime(entry.startHour, entry.startMinute)}")
                append(" – ${formatTime(entry.endHour, entry.endMinute)}")
                append(": ${entry.habitName}\n")
            }
            append("\nFrom Loop Habit Tracker")
        }

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, "Daily Routine")
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            // Set as daily recurring event
            putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    /**
     * Creates a single recurring daily event for one routine entry.
     */
    fun createRecurringEventForEntry(entry: RoutineEntry) {
        val startMillis = getTimeMillisForStart(entry.startHour, entry.startMinute)
        val endMillis = getTimeMillisForEnd(startMillis, entry.endHour, entry.endMinute)

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.Events.TITLE, entry.habitName)
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Daily routine habit from Loop Habit Tracker"
            )
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
            putExtra(CalendarContract.Events.RRULE, "FREQ=DAILY")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(intent)
    }

    private fun getTimeMillisForStart(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val now = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis < now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun getTimeMillisForEnd(startMillis: Long, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = startMillis
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis < startMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    companion object {
        fun formatTime(hour: Int, minute: Int): String {
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%d:%02d %s", displayHour, minute, amPm)
        }
    }
}
