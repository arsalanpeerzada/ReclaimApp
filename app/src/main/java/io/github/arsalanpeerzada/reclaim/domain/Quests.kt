package io.github.arsalanpeerzada.reclaim.domain

import java.time.LocalDate
import java.util.Random

/** An offline quest: something to do instead of scrolling. [icon] names a drawable; see ui/quests. */
data class Quest(val id: String, val title: String, val detail: String, val icon: String)

/**
 * Offline quests replace the habit instead of only removing it. Three a day, the same three all day,
 * done on trust: nothing is tracked. Completing one sometimes brings a surprise bloom: the slot-machine
 * mechanic, but only ever rewarding time off the phone.
 */
object Quests {
    const val PER_DAY = 3

    /** One in four completions is a surprise. */
    private const val SURPRISE_ONE_IN = 4

    val all = listOf(
        Quest("walk", "Take a 10-minute walk", "No destination needed. Just move.", "route"),
        Quest("read", "Read 10 pages", "A book, a magazine, anything printed.", "book"),
        Quest("stretch", "Stretch for 5 minutes", "Neck, shoulders, back. Slowly.", "stretch"),
        Quest("call", "Call someone you like", "Or send them a proper message.", "heart"),
        Quest("sky", "Step outside and look up", "Two minutes of sky. Clouds count.", "sun"),
        Quest("write", "Write three lines about today", "Paper or notes app, then put it down.", "pen"),
        Quest("drink", "Make yourself a drink", "Tea, coffee, juice. Drink it without a screen.", "cup"),
        Quest("song", "Listen to one song, eyes closed", "Just the song. Nothing else.", "music"),
        Quest("breathe", "One minute of slow breathing", "In for four, out for six.", "wind"),
        Quest("tidy", "Tidy one small space", "A desk corner, a shelf, a bag.", "tidy"),
    )

    private val byId = all.associateBy { it.id }

    fun byId(id: String): Quest? = byId[id]

    /** Today's three, the same all day and different tomorrow. */
    fun forDay(date: LocalDate): List<Quest> = all.shuffled(Random(date.toEpochDay() * 7919)).take(PER_DAY)

    /** Deterministic, so the garden can be rebuilt from history and the surprise never changes. */
    fun isSurprise(date: LocalDate, questId: String): Boolean {
        val hash = (date.toEpochDay() * 31 + questId.hashCode()).let { it xor (it ushr 16) }
        return Math.floorMod(hash, SURPRISE_ONE_IN.toLong()) == 0L
    }
}
