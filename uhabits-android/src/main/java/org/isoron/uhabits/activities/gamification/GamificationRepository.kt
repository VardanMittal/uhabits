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

package org.isoron.uhabits.activities.gamification

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Persists gamification state (XP, level, badges, streaks) in a dedicated SQLite database.
 * Uses its own DB file to avoid migrations on the main habit tracker database.
 */
class GamificationRepository(context: Context) {

    private val dbHelper = GamificationDbHelper(context)

    // ── XP & Level ──────────────────────────────────────────────────────

    fun getTotalXp(): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COALESCE(SUM($COL_XP_AMOUNT), 0) FROM $TABLE_XP_LOG", null)
        cursor.use {
            it.moveToFirst()
            return it.getLong(0)
        }
    }

    fun addXpEntry(amount: Int, reason: String, timestamp: Long = System.currentTimeMillis()) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COL_XP_AMOUNT, amount)
            put(COL_XP_REASON, reason)
            put(COL_XP_TIMESTAMP, timestamp)
        }
        db.insert(TABLE_XP_LOG, null, values)
    }

    fun getXpInRange(startMs: Long, endMs: Long): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COALESCE(SUM($COL_XP_AMOUNT), 0) FROM $TABLE_XP_LOG WHERE $COL_XP_TIMESTAMP >= ? AND $COL_XP_TIMESTAMP <= ?",
            arrayOf(startMs.toString(), endMs.toString())
        )
        cursor.use {
            it.moveToFirst()
            return it.getLong(0)
        }
    }

    // ── Badges ───────────────────────────────────────────────────────────

    fun getUnlockedBadges(): List<UnlockedBadge> {
        val db = dbHelper.readableDatabase
        val badges = mutableListOf<UnlockedBadge>()
        val cursor = db.query(TABLE_BADGES, null, null, null, null, null, "$COL_BADGE_UNLOCKED_AT DESC")
        cursor.use {
            while (it.moveToNext()) {
                val badgeId = it.getString(it.getColumnIndexOrThrow(COL_BADGE_ID))
                val badge = Badge.fromId(badgeId) ?: continue
                badges.add(
                    UnlockedBadge(
                        badge = badge,
                        unlockedAt = it.getLong(it.getColumnIndexOrThrow(COL_BADGE_UNLOCKED_AT))
                    )
                )
            }
        }
        return badges
    }

    fun isBadgeUnlocked(badgeId: String): Boolean {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_BADGES, arrayOf(COL_BADGE_ID),
            "$COL_BADGE_ID = ?", arrayOf(badgeId),
            null, null, null
        )
        val unlocked = cursor.count > 0
        cursor.close()
        return unlocked
    }

    fun unlockBadge(badgeId: String, timestamp: Long = System.currentTimeMillis()) {
        if (isBadgeUnlocked(badgeId)) return
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COL_BADGE_ID, badgeId)
            put(COL_BADGE_UNLOCKED_AT, timestamp)
        }
        db.insert(TABLE_BADGES, null, values)
    }

    fun getBadgesInRange(startMs: Long, endMs: Long): List<UnlockedBadge> {
        val db = dbHelper.readableDatabase
        val badges = mutableListOf<UnlockedBadge>()
        val cursor = db.query(
            TABLE_BADGES, null,
            "$COL_BADGE_UNLOCKED_AT >= ? AND $COL_BADGE_UNLOCKED_AT <= ?",
            arrayOf(startMs.toString(), endMs.toString()),
            null, null, "$COL_BADGE_UNLOCKED_AT DESC"
        )
        cursor.use {
            while (it.moveToNext()) {
                val badgeId = it.getString(it.getColumnIndexOrThrow(COL_BADGE_ID))
                val badge = Badge.fromId(badgeId) ?: continue
                badges.add(
                    UnlockedBadge(
                        badge = badge,
                        unlockedAt = it.getLong(it.getColumnIndexOrThrow(COL_BADGE_UNLOCKED_AT))
                    )
                )
            }
        }
        return badges
    }

    // ── Streaks ──────────────────────────────────────────────────────────

    fun getCurrentStreak(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COALESCE(MAX($COL_STREAK_CURRENT), 0) FROM $TABLE_STREAKS", null
        )
        cursor.use {
            it.moveToFirst()
            return it.getInt(0)
        }
    }

    fun getBestStreak(): Int {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COALESCE(MAX($COL_STREAK_BEST), 0) FROM $TABLE_STREAKS", null
        )
        cursor.use {
            it.moveToFirst()
            return it.getInt(0)
        }
    }

    fun updateStreak(habitId: Long, current: Int, best: Int) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(COL_STREAK_HABIT_ID, habitId)
            put(COL_STREAK_CURRENT, current)
            put(COL_STREAK_BEST, best)
            put(COL_STREAK_UPDATED, System.currentTimeMillis())
        }
        // Try update first
        val updated = db.update(TABLE_STREAKS, values,
            "$COL_STREAK_HABIT_ID = ?", arrayOf(habitId.toString()))
        if (updated == 0) {
            db.insert(TABLE_STREAKS, null, values)
        }
    }

    fun getStreakForHabit(habitId: Long): Pair<Int, Int> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            TABLE_STREAKS, arrayOf(COL_STREAK_CURRENT, COL_STREAK_BEST),
            "$COL_STREAK_HABIT_ID = ?", arrayOf(habitId.toString()),
            null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return Pair(
                    it.getInt(it.getColumnIndexOrThrow(COL_STREAK_CURRENT)),
                    it.getInt(it.getColumnIndexOrThrow(COL_STREAK_BEST))
                )
            }
        }
        return Pair(0, 0)
    }

    fun getAllStreaks(): List<HabitStreak> {
        val db = dbHelper.readableDatabase
        val streaks = mutableListOf<HabitStreak>()
        val cursor = db.query(TABLE_STREAKS, null, null, null, null, null, "$COL_STREAK_CURRENT DESC")
        cursor.use {
            while (it.moveToNext()) {
                streaks.add(
                    HabitStreak(
                        habitId = it.getLong(it.getColumnIndexOrThrow(COL_STREAK_HABIT_ID)),
                        current = it.getInt(it.getColumnIndexOrThrow(COL_STREAK_CURRENT)),
                        best = it.getInt(it.getColumnIndexOrThrow(COL_STREAK_BEST))
                    )
                )
            }
        }
        return streaks
    }

    data class UnlockedBadge(val badge: Badge, val unlockedAt: Long)
    data class HabitStreak(val habitId: Long, val current: Int, val best: Int)

    companion object {
        private const val DB_NAME = "gamification.db"
        private const val DB_VERSION = 1

        private const val TABLE_XP_LOG = "xp_log"
        private const val COL_XP_ID = "_id"
        private const val COL_XP_AMOUNT = "amount"
        private const val COL_XP_REASON = "reason"
        private const val COL_XP_TIMESTAMP = "timestamp"

        private const val TABLE_BADGES = "badges"
        private const val COL_BADGE_ID = "badge_id"
        private const val COL_BADGE_UNLOCKED_AT = "unlocked_at"

        private const val TABLE_STREAKS = "streaks"
        private const val COL_STREAK_HABIT_ID = "habit_id"
        private const val COL_STREAK_CURRENT = "current_streak"
        private const val COL_STREAK_BEST = "best_streak"
        private const val COL_STREAK_UPDATED = "updated_at"
    }

    private class GamificationDbHelper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE $TABLE_XP_LOG (
                    $COL_XP_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COL_XP_AMOUNT INTEGER NOT NULL DEFAULT 0,
                    $COL_XP_REASON TEXT NOT NULL DEFAULT '',
                    $COL_XP_TIMESTAMP INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE $TABLE_BADGES (
                    $COL_BADGE_ID TEXT PRIMARY KEY,
                    $COL_BADGE_UNLOCKED_AT INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE $TABLE_STREAKS (
                    $COL_STREAK_HABIT_ID INTEGER PRIMARY KEY,
                    $COL_STREAK_CURRENT INTEGER NOT NULL DEFAULT 0,
                    $COL_STREAK_BEST INTEGER NOT NULL DEFAULT 0,
                    $COL_STREAK_UPDATED INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // Future migrations
        }
    }
}
