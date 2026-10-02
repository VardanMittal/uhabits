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

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import org.isoron.platform.gui.toInt
import org.isoron.uhabits.HabitsApplication
import org.isoron.uhabits.R
import org.isoron.uhabits.activities.AndroidThemeSwitcher
import org.isoron.uhabits.core.models.Habit
import org.isoron.uhabits.core.models.PaletteColor
import org.isoron.uhabits.core.preferences.Preferences
import org.isoron.uhabits.databinding.ActivityDailyRoutineBinding
import org.isoron.uhabits.utils.applyBottomInset
import org.isoron.uhabits.utils.applyRootViewInsets
import org.isoron.uhabits.utils.applyToolbarInsets
import org.isoron.uhabits.utils.currentTheme

class DailyRoutineActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDailyRoutineBinding
    private lateinit var repository: RoutineRepository
    private lateinit var adapter: RoutineAdapter
    private lateinit var calendarSyncHelper: CalendarSyncHelper
    private lateinit var themeSwitcher: AndroidThemeSwitcher
    private var allHabits = listOf<Habit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val appComponent = (application as HabitsApplication).component
        themeSwitcher = AndroidThemeSwitcher(this, appComponent.preferences)
        themeSwitcher.apply()

        binding = ActivityDailyRoutineBinding.inflate(layoutInflater)
        binding.root.applyRootViewInsets()
        binding.root.applyBottomInset()
        binding.toolbar.applyToolbarInsets()
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Theme the toolbar correctly if not night mode
        if (!themeSwitcher.isNightMode) {
            val color = themeSwitcher.currentTheme.color(PaletteColor(17)).toInt()
            window.statusBarColor = color
            binding.toolbar.setBackgroundColor(color)
        }

        repository = RoutineRepository(this)
        calendarSyncHelper = CalendarSyncHelper(this)

        allHabits = appComponent.habitList.toList()
        
        setupRecyclerView()
        setupListeners()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = RoutineAdapter(
            onDelete = { entry -> deleteEntry(entry) },
            onSyncEntry = { entry -> syncSingleEntry(entry) },
            getColor = { paletteIndex -> 
                themeSwitcher.currentTheme.color(PaletteColor(paletteIndex)).toInt() 
            }
        )
        binding.routineList.layoutManager = LinearLayoutManager(this)
        binding.routineList.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAddRoutine.setOnClickListener {
            showAddEntryDialog()
        }
        
        binding.btnSyncAll.setOnClickListener {
            syncAllEntries()
        }
    }

    private fun loadData() {
        val entries = repository.getAll()
        adapter.submitList(entries)
        updateUIState(entries)
    }

    private fun updateUIState(entries: List<RoutineEntry>) {
        if (entries.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.routineList.visibility = View.GONE
            binding.routineSummary.text = getString(R.string.routine_summary_empty)
            binding.btnSyncAll.isEnabled = false
        } else {
            binding.emptyState.visibility = View.GONE
            binding.routineList.visibility = View.VISIBLE
            binding.routineSummary.text = resources.getQuantityString(
                R.plurals.routine_summary_items, entries.size, entries.size
            )
            binding.btnSyncAll.isEnabled = true
        }
    }

    private fun showAddEntryDialog() {
        val dialog = AddRoutineEntryDialog(
            activity = this,
            habits = allHabits,
            themeSwitcher = themeSwitcher,
            onEntryAdded = { entry ->
                repository.insert(entry)
                loadData()
            }
        )
        dialog.show()
    }

    private fun deleteEntry(entry: RoutineEntry) {
        repository.delete(entry.id)
        loadData()
    }

    private fun syncSingleEntry(entry: RoutineEntry) {
        calendarSyncHelper.createRecurringEventForEntry(entry)
    }

    private fun syncAllEntries() {
        val entries = adapter.getEntries()
        if (entries.isNotEmpty()) {
            calendarSyncHelper.createAllEvents(entries)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
