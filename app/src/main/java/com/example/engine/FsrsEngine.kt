package com.example.engine

import com.example.model.FlashcardRating
import com.example.model.FsrsCardStateData
import com.example.model.FsrsPreviewOption
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * FSRS-4.5 Spaced Repetition Engine (pure offline deterministic implementation).
 * States: 0 = New, 1 = Learning, 2 = Review, 3 = Relearning
 */
object FsrsEngine {
    private val w = doubleArrayOf(
        0.40255, 1.18385, 3.173, 15.69105,
        7.1949, 0.5345, 1.4604, 0.0046,
        1.54575, 0.1192, 1.01925, 1.9395,
        0.11, 0.29605, 2.2698, 0.2315,
        2.9898, 0.51655, 0.6621
    )

    private const val DAY_MS = 86_400_000L
    private const val MIN_MS = 60_000L
    private const val FACTOR = 19.0 / 81.0
    private const val DECAY = -0.5

    fun newCardState(cardId: String, nowMs: Long = System.currentTimeMillis()): FsrsCardStateData {
        return FsrsCardStateData(
            cardId = cardId,
            dueAtMs = nowMs,
            stability = 0.0,
            difficulty = 5.0,
            lastReviewAtMs = null,
            reps = 0,
            lapses = 0,
            state = 0
        )
    }

    fun schedule(
        card: FsrsCardStateData,
        rating: FlashcardRating,
        desiredRetention: Double = 0.90,
        nowMs: Long = System.currentTimeMillis()
    ): FsrsCardStateData {
        val g = rating.value
        val r = desiredRetention.coerceIn(0.75, 0.97)
        val elapsedDays = if (card.lastReviewAtMs != null && nowMs > card.lastReviewAtMs) {
            (nowMs - card.lastReviewAtMs).toDouble() / DAY_MS
        } else 0.0

        val nextState: Int
        val nextLapses: Int
        var newD: Double
        var newS: Double
        val dueOffsetMs: Long

        if (card.state == 0 || card.reps == 0) {
            newD = initDifficulty(g)
            newS = initStability(g)
            when (rating) {
                FlashcardRating.AGAIN -> {
                    nextState = 1
                    nextLapses = card.lapses
                    dueOffsetMs = 1L * MIN_MS
                }
                FlashcardRating.HARD -> {
                    nextState = 1
                    nextLapses = card.lapses
                    val hardMinutes = max(5.0, newS * 10.0)
                    dueOffsetMs = (hardMinutes * MIN_MS).toLong()
                }
                FlashcardRating.GOOD -> {
                    nextState = 2
                    nextLapses = card.lapses
                    val days = max(1.0, nextIntervalDays(newS, r))
                    dueOffsetMs = (days * DAY_MS).toLong()
                }
                FlashcardRating.EASY -> {
                    nextState = 2
                    nextLapses = card.lapses
                    val days = max(3.0, nextIntervalDays(newS, r))
                    dueOffsetMs = (days * DAY_MS).toLong()
                }
            }
        } else {
            val retrievability = if (card.stability > 0.0) {
                (1.0 + FACTOR * (elapsedDays / card.stability)).pow(DECAY)
            } else 0.9
            newD = nextDifficulty(card.difficulty, g)
            if (rating == FlashcardRating.AGAIN) {
                nextState = if (card.state == 2 || card.state == 3) 3 else 1
                nextLapses = if (card.state == 2 || card.state == 3) card.lapses + 1 else card.lapses
                newS = nextForgetStability(newD, max(card.stability, 0.2), retrievability)
                dueOffsetMs = 5L * MIN_MS
            } else {
                nextState = 2
                nextLapses = card.lapses
                newS = nextRecallStability(newD, max(card.stability, 0.4), retrievability, g)
                val baseDays = nextIntervalDays(newS, r)
                val adjustedDays = when (rating) {
                    FlashcardRating.HARD -> max(1.0, baseDays * 0.8)
                    FlashcardRating.GOOD -> max(1.0, baseDays)
                    FlashcardRating.EASY -> max(2.0, baseDays * 1.25)
                    FlashcardRating.AGAIN -> 0.01
                }
                dueOffsetMs = (adjustedDays * DAY_MS).toLong()
            }
        }

        return card.copy(
            dueAtMs = nowMs + dueOffsetMs,
            stability = newS.coerceIn(0.1, 36500.0),
            difficulty = newD.coerceIn(1.0, 10.0),
            lastReviewAtMs = nowMs,
            reps = card.reps + 1,
            lapses = nextLapses,
            state = nextState
        )
    }

    fun previewAllRatings(
        card: FsrsCardStateData,
        desiredRetention: Double = 0.90,
        nowMs: Long = System.currentTimeMillis()
    ): Map<FlashcardRating, FsrsPreviewOption> {
        return FlashcardRating.entries.associateWith { rating ->
            val next = schedule(card, rating, desiredRetention, nowMs)
            val days = (next.dueAtMs - nowMs).toDouble() / DAY_MS
            FsrsPreviewOption(
                rating = rating,
                intervalDays = days,
                intervalLabel = formatIntervalLabel(days)
            )
        }
    }

    fun formatIntervalLabel(intervalDays: Double): String {
        val minutes = intervalDays * 1440.0
        return when {
            minutes < 1.5 -> "1 分钟"
            minutes < 60.0 -> "${minutes.roundToInt()} 分钟"
            intervalDays < 1.0 -> "${(intervalDays * 24.0).roundToInt()} 小时"
            intervalDays < 30.0 -> "${max(1, intervalDays.roundToInt())} 天"
            intervalDays < 365.0 -> "${"%.1f".format(intervalDays / 30.0)} 月"
            else -> "${"%.1f".format(intervalDays / 365.0)} 年"
        }
    }

    private fun initStability(g: Int): Double = w[(g - 1).coerceIn(0, 3)]

    private fun initDifficulty(g: Int): Double {
        return (w[4] - exp(w[5] * (g - 1)) + 1.0).coerceIn(1.0, 10.0)
    }

    private fun nextDifficulty(d: Double, g: Int): Double {
        val delta = -w[6] * (g - 3)
        val damped = d + delta * ((10.0 - d) / 9.0)
        return (w[7] * initDifficulty(4) + (1.0 - w[7]) * damped).coerceIn(1.0, 10.0)
    }

    private fun nextRecallStability(d: Double, s: Double, r: Double, g: Int): Double {
        val hardPenalty = if (g == 2) w[15] else 1.0
        val easyBonus = if (g == 4) w[16] else 1.0
        val factor = exp(w[8]) *
            (11.0 - d) *
            s.pow(-w[9]) *
            (exp((1.0 - r) * w[10]) - 1.0) *
            hardPenalty *
            easyBonus
        return s * (1.0 + factor)
    }

    private fun nextForgetStability(d: Double, s: Double, r: Double): Double {
        val raw = w[11] *
            d.pow(-w[12]) *
            ((s + 1.0).pow(w[13]) - 1.0) *
            exp((1.0 - r) * w[14])
        return min(s, max(0.2, raw))
    }

    private fun nextIntervalDays(stability: Double, retention: Double): Double {
        val raw = (stability / FACTOR) * (retention.pow(1.0 / DECAY) - 1.0)
        return max(1.0, raw)
    }
}
