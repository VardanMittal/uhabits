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

import android.content.Context
import android.widget.Toast
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages all gamification logic: XP calculation, leveling, badge unlocking, and streak tracking.
 * Acts as the business logic layer between the UI and the GamificationRepository.
 */
class GamificationManager(private val context: Context) {

    private val repository = GamificationRepository(context)

    companion object {
        const val BASE_XP_PER_COMPLETION = 10
        const val STREAK_BONUS_MULTIPLIER = 2
        const val PERFECT_DAY_BONUS = 50
        const val XP_PER_LEVEL_BASE = 100.0
        const val LEVEL_GROWTH_FACTOR = 1.5
    }

    // ── XP & Level ──────────────────────────────────────────────────────

    fun getTotalXp(): Long = repository.getTotalXp()

    fun getCurrentLevel(): Int {
        val xp = getTotalXp()
        return calculateLevel(xp)
    }

    fun getXpForLevel(level: Int): Long {
        if (level <= 1) return 0
        return (XP_PER_LEVEL_BASE * (level - 1).toDouble().pow(LEVEL_GROWTH_FACTOR)).toLong()
    }

    fun getXpForNextLevel(): Long = getXpForLevel(getCurrentLevel() + 1)

    fun getXpProgressInCurrentLevel(): Long {
        val totalXp = getTotalXp()
        val currentLevelXp = getXpForLevel(getCurrentLevel())
        return totalXp - currentLevelXp
    }

    fun getXpNeededForCurrentLevel(): Long {
        val nextLevelXp = getXpForNextLevel()
        val currentLevelXp = getXpForLevel(getCurrentLevel())
        return nextLevelXp - currentLevelXp
    }

    fun getXpProgressFraction(): Float {
        val needed = getXpNeededForCurrentLevel()
        if (needed <= 0) return 1f
        return (getXpProgressInCurrentLevel().toFloat() / needed).coerceIn(0f, 1f)
    }

    private fun calculateLevel(xp: Long): Int {
        var level = 1
        while (getXpForLevel(level + 1) <= xp) {
            level++
        }
        return level
    }

    // ── Award XP ────────────────────────────────────────────────────────

    /**
     * Awards XP for a single habit completion.
     * Returns the total XP awarded (base + streak bonus).
     */
    fun awardCompletionXp(habitId: Long): Int {
        val levelBefore = getCurrentLevel()

        val (currentStreak, bestStreak) = repository.getStreakForHabit(habitId)
        val newStreak = currentStreak + 1
        val newBest = maxOf(bestStreak, newStreak)

        // Base XP + streak bonus
        val streakBonus = (newStreak * STREAK_BONUS_MULTIPLIER).coerceAtMost(100)
        val totalXp = BASE_XP_PER_COMPLETION + streakBonus

        repository.addXpEntry(totalXp, "habit_completion:$habitId")
        repository.updateStreak(habitId, newStreak, newBest)

        // Check for level up
        val levelAfter = getCurrentLevel()
        if (levelAfter > levelBefore) {
            Toast.makeText(context, "🎉 Level Up! You're now Level $levelAfter!", Toast.LENGTH_LONG).show()
        }

        // Check for streak badges
        checkStreakBadges(newStreak)

        return totalXp
    }

    /**
     * Awards bonus XP for a perfect day (all habits completed).
     */
    fun awardPerfectDayBonus() {
        repository.addXpEntry(PERFECT_DAY_BONUS, "perfect_day")
        checkBadge(Badge.PERFECT_WEEK)
    }

    /**
     * Called when a habit completion is reverted.
     */
    fun revertCompletionStreak(habitId: Long) {
        val (_, bestStreak) = repository.getStreakForHabit(habitId)
        repository.updateStreak(habitId, 0, bestStreak)
    }

    // ── Badges ───────────────────────────────────────────────────────────

    fun getUnlockedBadges() = repository.getUnlockedBadges()

    fun getAllBadgesWithStatus(): List<BadgeStatus> {
        val unlocked = repository.getUnlockedBadges().map { it.badge }.toSet()
        return Badge.values().map { badge ->
            BadgeStatus(
                badge = badge,
                isUnlocked = badge in unlocked
            )
        }
    }

    private fun checkStreakBadges(streak: Int) {
        if (streak >= 3) checkBadge(Badge.FIRST_FLAME)
        if (streak >= 7) checkBadge(Badge.SHARPSHOOTER)
        if (streak >= 30) checkBadge(Badge.DIAMOND_STREAK)
        if (streak >= 100) checkBadge(Badge.HABIT_MASTER)
    }

    private fun checkBadge(badge: Badge) {
        if (!repository.isBadgeUnlocked(badge.badgeId)) {
            repository.unlockBadge(badge.badgeId)
            repository.addXpEntry(badge.xpReward, "badge:${badge.badgeId}")
            Toast.makeText(context, "${badge.emoji} Badge Unlocked: ${badge.badgeId.replace('_', ' ').replaceFirstChar { it.uppercase() }}!", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Check if a perfect week has been achieved (called externally when all habits done for 7 days).
     */
    fun checkPerfectWeek(consecutivePerfectDays: Int) {
        if (consecutivePerfectDays >= 7) {
            checkBadge(Badge.PERFECT_WEEK)
        }
        if (consecutivePerfectDays >= 30) {
            checkBadge(Badge.PERFECT_MONTH)
        }
    }

    // ── Streaks ──────────────────────────────────────────────────────────

    fun getCurrentStreak(): Int = repository.getCurrentStreak()
    fun getBestStreak(): Int = repository.getBestStreak()
    fun getAllStreaks() = repository.getAllStreaks()
    fun getStreakForHabit(habitId: Long) = repository.getStreakForHabit(habitId)

    // ── Stats for Review ─────────────────────────────────────────────────

    fun getXpInRange(startMs: Long, endMs: Long): Long = repository.getXpInRange(startMs, endMs)
    fun getBadgesInRange(startMs: Long, endMs: Long) = repository.getBadgesInRange(startMs, endMs)

    data class BadgeStatus(val badge: Badge, val isUnlocked: Boolean)
}
