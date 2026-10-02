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

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.datetimepicker.time.RadialPickerLayout
import com.android.datetimepicker.time.TimePickerDialog
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.R
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.ui.ThemeSwitcher

/**
 * Dialog for adding a new habit to the daily routine.
 * Allows the user to pick a habit and set start/end times.
 */
class AddRoutineEntryDialog(
    private val activity: AppCompatActivity,
    private val habits: List<Habit>,
    private val themeSwitcher: ThemeSwitcher,
    private val onEntryAdded: (RoutineEntry) -> Unit
) {

    private var selectedHabit: Habit? = null
    private var startHour = 8
    private var startMinute = 0
    private var endHour = 9
    private var endMinute = 0

    fun show() {
        if (habits.isEmpty()) {
            AlertDialog.Builder(activity)
                .setTitle(R.string.add_to_routine)
                .setMessage(R.string.no_habits_to_add)
                .setPositiveButton(android.R.string.ok, null)
                .show()
            return
        }

        val dialogView = LayoutInflater.from(activity)
            .inflate(R.layout.dialog_add_routine_entry, null)

        val habitPicker = dialogView.findViewById<TextView>(R.id.habitPicker)
        val startTimePicker = dialogView.findViewById<TextView>(R.id.startTimePicker)
        val endTimePicker = dialogView.findViewById<TextView>(R.id.endTimePicker)

        // Default display
        startTimePicker.text = CalendarSyncHelper.formatTime(startHour, startMinute)
        endTimePicker.text = CalendarSyncHelper.formatTime(endHour, endMinute)

        // Habit picker
        habitPicker.setOnClickListener {
            val habitNames = habits.map { it.name }.toTypedArray()
            val adapter = ArrayAdapter(activity, android.R.layout.select_dialog_item, habitNames)
            AlertDialog.Builder(activity)
                .setTitle(R.string.select_habit)
                .setAdapter(adapter) { dialog, which ->
                    selectedHabit = habits[which]
                    habitPicker.text = habits[which].name
                    dialog.dismiss()
                }
                .show()
        }

        // Start time picker
        startTimePicker.setOnClickListener {
            showTimePicker(startHour, startMinute) { hour, minute ->
                startHour = hour
                startMinute = minute
                startTimePicker.text = CalendarSyncHelper.formatTime(hour, minute)
            }
        }

        // End time picker
        endTimePicker.setOnClickListener {
            showTimePicker(endHour, endMinute) { hour, minute ->
                endHour = hour
                endMinute = minute
                endTimePicker.text = CalendarSyncHelper.formatTime(hour, minute)
            }
        }

        AlertDialog.Builder(activity)
            .setTitle(R.string.add_to_routine)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                val habit = selectedHabit
                if (habit != null) {
                    val entry = RoutineEntry(
                        habitId = habit.id ?: -1,
                        habitName = habit.name,
                        habitColorIndex = habit.color.paletteIndex,
                        startHour = startHour,
                        startMinute = startMinute,
                        endHour = endHour,
                        endMinute = endMinute
                    )
                    onEntryAdded(entry)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showTimePicker(
        currentHour: Int,
        currentMinute: Int,
        onTimePicked: (Int, Int) -> Unit
    ) {
        val is24Hour = DateFormat.is24HourFormat(activity)
        val accentColor = themeSwitcher.currentTheme?.let {
            it.color(org.isoron.uhabits.core.models.PaletteColor(11)).toInt()
        } ?: 0xFF6200EE.toInt()

        val dialog = TimePickerDialog.newInstance(
            object : TimePickerDialog.OnTimeSetListener {
                override fun onTimeSet(view: RadialPickerLayout?, hourOfDay: Int, minute: Int) {
                    onTimePicked(hourOfDay, minute)
                }

                override fun onTimeCleared(view: RadialPickerLayout?) {
                    // Do nothing
                }
            },
            currentHour,
            currentMinute,
            is24Hour,
            accentColor
        )
        dialog.show(activity.supportFragmentManager, "routineTimePicker")
    }
}
