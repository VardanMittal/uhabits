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

/**
 * Represents a single time slot in the user's daily routine schedule.
 * Each entry maps a habit to a specific time window during the day.
 */
data class RoutineEntry(
    var id: Long = -1,
    var habitId: Long = -1,
    var habitName: String = "",
    var habitColorIndex: Int = 0,
    var startHour: Int = 8,
    var startMinute: Int = 0,
    var endHour: Int = 9,
    var endMinute: Int = 0,
    var order: Int = 0
) {
    /**
     * Returns the total duration in minutes.
     */
    fun durationMinutes(): Int {
        val startTotal = startHour * 60 + startMinute
        val endTotal = endHour * 60 + endMinute
        return if (endTotal > startTotal) endTotal - startTotal else 0
    }

    /**
     * Returns the start time as total minutes from midnight.
     */
    fun startTimeMinutes(): Int = startHour * 60 + startMinute

    /**
     * Returns the end time as total minutes from midnight.
     */
    fun endTimeMinutes(): Int = endHour * 60 + endMinute
}
