package com.moneyfirst.pourperfect.state;

/** Result of recording a day's play with {@link StreakTracker#recordPlay}. */
public record StreakResult(int currentStreak, int bestStreak) {
}
