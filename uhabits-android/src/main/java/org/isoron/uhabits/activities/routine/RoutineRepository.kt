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

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Manages persistence of daily routine schedule entries in a dedicated SQLite database.
 * Uses its own database file (separate from the main habits DB) to avoid schema migration
 * conflicts with the existing habit tracker database.
 */
class RoutineRepository(context: Context) {

    private val dbHelper = RoutineDbHelper(context)

    fun getAll(): List<RoutineEntry> {
        val db = dbHelper.readableDatabase
        val entries = mutableListOf<RoutineEntry>()
        val cursor = db.query(
            TABLE_NAME, null, null, null, null, null,
            "$COL_START_HOUR ASC, $COL_START_MINUTE ASC"
        )
        cursor.use {
            while (it.moveToNext()) {
                entries.add(
                    RoutineEntry(
                        id = it.getLong(it.getColumnIndexOrThrow(COL_ID)),
                        habitId = it.getLong(it.getColumnIndexOrThrow(COL_HABIT_ID)),
                        habitName = it.getString(it.getColumnIndexOrThrow(COL_HABIT_NAME)),
                        habitColorIndex = it.getInt(it.getColumnIndexOrThrow(COL_HABIT_COLOR)),
                        startHour = it.getInt(it.getColumnIndexOrThrow(COL_START_HOUR)),
                        startMinute = it.getInt(it.getColumnIndexOrThrow(COL_START_MINUTE)),
                        endHour = it.getInt(it.getColumnIndexOrThrow(COL_END_HOUR)),
                        endMinute = it.getInt(it.getColumnIndexOrThrow(COL_END_MINUTE)),
                        order = it.getInt(it.getColumnIndexOrThrow(COL_ORDER))
                    )
                )
            }
        }
        return entries
    }

    fun insert(entry: RoutineEntry): Long {
        val db = dbHelper.writableDatabase
        val values = toContentValues(entry)
        return db.insert(TABLE_NAME, null, values)
    }

    fun update(entry: RoutineEntry) {
        val db = dbHelper.writableDatabase
        val values = toContentValues(entry)
        db.update(TABLE_NAME, values, "$COL_ID = ?", arrayOf(entry.id.toString()))
    }

    fun delete(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(TABLE_NAME, "$COL_ID = ?", arrayOf(id.toString()))
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete(TABLE_NAME, null, null)
    }

    private fun toContentValues(entry: RoutineEntry) = ContentValues().apply {
        put(COL_HABIT_ID, entry.habitId)
        put(COL_HABIT_NAME, entry.habitName)
        put(COL_HABIT_COLOR, entry.habitColorIndex)
        put(COL_START_HOUR, entry.startHour)
        put(COL_START_MINUTE, entry.startMinute)
        put(COL_END_HOUR, entry.endHour)
        put(COL_END_MINUTE, entry.endMinute)
        put(COL_ORDER, entry.order)
    }

    companion object {
        private const val DB_NAME = "routine.db"
        private const val DB_VERSION = 1
        private const val TABLE_NAME = "routine_entries"
        private const val COL_ID = "_id"
        private const val COL_HABIT_ID = "habit_id"
        private const val COL_HABIT_NAME = "habit_name"
        private const val COL_HABIT_COLOR = "habit_color"
        private const val COL_START_HOUR = "start_hour"
        private const val COL_START_MINUTE = "start_minute"
        private const val COL_END_HOUR = "end_hour"
        private const val COL_END_MINUTE = "end_minute"
        private const val COL_ORDER = "sort_order"
    }

    private class RoutineDbHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE $TABLE_NAME (
                    $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_HABIT_ID INTEGER NOT NULL,
                    $COL_HABIT_NAME TEXT NOT NULL DEFAULT '',
                    $COL_HABIT_COLOR INTEGER NOT NULL DEFAULT 0,
                    $COL_START_HOUR INTEGER NOT NULL DEFAULT 8,
                    $COL_START_MINUTE INTEGER NOT NULL DEFAULT 0,
                    $COL_END_HOUR INTEGER NOT NULL DEFAULT 9,
                    $COL_END_MINUTE INTEGER NOT NULL DEFAULT 0,
                    $COL_ORDER INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Future migrations go here
        }
    }
}
