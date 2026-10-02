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

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.isoron.uhabits.R
import org.isoron.uhabits.databinding.ItemRoutineEntryBinding

class RoutineAdapter(
    private val onDelete: (RoutineEntry) -> Unit,
    private val onSyncEntry: (RoutineEntry) -> Unit,
    private val getColor: (Int) -> Int
) : RecyclerView.Adapter<RoutineAdapter.RoutineViewHolder>() {

    private val entries = mutableListOf<RoutineEntry>()

    fun submitList(newEntries: List<RoutineEntry>) {
        entries.clear()
        entries.addAll(newEntries)
        notifyDataSetChanged()
    }

    fun getEntries(): List<RoutineEntry> = entries.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoutineViewHolder {
        val binding = ItemRoutineEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return RoutineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoutineViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size

    inner class RoutineViewHolder(
        private val binding: ItemRoutineEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: RoutineEntry) {
            val startTime = CalendarSyncHelper.formatTime(entry.startHour, entry.startMinute)
            val endTime = CalendarSyncHelper.formatTime(entry.endHour, entry.endMinute)
            val duration = entry.durationMinutes()

            binding.tvStartTime.text = startTime
            binding.tvEndTime.text = endTime
            binding.tvHabitName.text = entry.habitName

            val durationText = when {
                duration >= 60 && duration % 60 == 0 -> {
                    val hours = duration / 60
                    binding.root.context.resources.getQuantityString(
                        R.plurals.duration_hours, hours, hours
                    )
                }
                duration >= 60 -> {
                    val hours = duration / 60
                    val mins = duration % 60
                    "${hours}h ${mins}m"
                }
                else -> "$duration min"
            }
            binding.tvDuration.text = durationText

            // Set the color accent
            val color = getColor(entry.habitColorIndex)
            binding.colorBar.backgroundTintList = ColorStateList.valueOf(color)
            binding.timelineDot.backgroundTintList = ColorStateList.valueOf(color)

            binding.btnDeleteEntry.setOnClickListener { onDelete(entry) }
            binding.btnSyncEntry.setOnClickListener { onSyncEntry(entry) }
        }
    }
}
