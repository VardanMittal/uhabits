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

/**
 * Defines all achievable badges in the gamification system.
 * Each badge has a unique ID, display info, unlock condition, and XP reward.
 */
enum class Badge(
    val badgeId: String,
    val emoji: String,
    val titleResName: String,
    val descResName: String,
    val xpReward: Int
) {
    FIRST_FLAME("first_flame", "🔥", "badge_first_flame", "badge_first_flame_desc", 25),
    WEEKENDER("weekender", "⚡", "badge_weekender", "badge_weekender_desc", 30),
    SHARPSHOOTER("sharpshooter", "🎯", "badge_sharpshooter", "badge_sharpshooter_desc", 50),
    DIAMOND_STREAK("diamond_streak", "💎", "badge_diamond_streak", "badge_diamond_streak_desc", 200),
    HABIT_MASTER("habit_master", "👑", "badge_habit_master", "badge_habit_master_desc", 500),
    PERFECT_WEEK("perfect_week", "🏆", "badge_perfect_week", "badge_perfect_week_desc", 100),
    PERFECT_MONTH("perfect_month", "🌟", "badge_perfect_month", "badge_perfect_month_desc", 500),
    EARLY_BIRD("early_bird", "🐦", "badge_early_bird", "badge_early_bird_desc", 20);

    companion object {
        fun fromId(id: String): Badge? = values().find { it.badgeId == id }
    }
}
