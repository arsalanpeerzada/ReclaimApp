package io.github.arsalanpeerzada.reclaim.ui.share

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import io.github.arsalanpeerzada.reclaim.R
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What goes on a squad card. Counts only: nothing about what was watched. */
data class SquadWeek(
    val weekStart: LocalDate,
    val daysWithinBudget: Int,
    val trackedDays: Int,
    val streak: Int,
    val averagePerDay: Int,
    val minutesWonBack: Int,
)

/**
 * Squads without a server: the week as a bright card, shared through whatever app the user picks.
 * Accountability between friends, no accounts, no leaderboard, and Reclaim still needs no internet.
 */
object SquadCard {
    private const val W = 1080
    private const val H = 1350
    private const val INK = 0xFF1D1733.toInt()
    private const val CREAM = 0xFFFFF6EA.toInt()
    private const val VIOLET = 0xFF6C4CF5.toInt()

    fun share(context: Context, week: SquadWeek) {
        val file = File(context.cacheDir, "shared").apply { mkdirs() }.resolve("reclaim-week.png")
        file.outputStream().use { render(context, week).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(
                Intent.EXTRA_TEXT,
                "My Reclaim week: ${week.daysWithinBudget} of ${week.trackedDays} days within my reel budget. Squad up? Share your week too.",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "Share your week").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun render(context: Context, week: SquadWeek): Bitmap {
        val bitmap = createBitmap(W, H)
        val canvas = Canvas(bitmap)
        canvas.drawColor(CREAM)
        val display = ResourcesCompat.getFont(context, R.font.bricolage_grotesque) ?: Typeface.DEFAULT_BOLD
        val body = ResourcesCompat.getFont(context, R.font.dm_sans) ?: Typeface.DEFAULT

        fun paint(colour: Int, size: Float, face: Typeface, weight: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colour
            textSize = size
            typeface = face
            setFontVariationSettings("'wght' $weight")
        }
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)

        // Header band with Pip.
        fill.color = VIOLET
        canvas.drawRoundRect(RectF(60f, 60f, 1020f, 470f), 64f, 64f, fill)
        canvas.drawText("MY RECLAIM WEEK", 110f, 150f, paint(0xFFFFC83D.toInt(), 40f, body, 700))
        canvas.drawText("${week.daysWithinBudget} of ${week.trackedDays} days", 110f, 285f, paint(0xFFFFFFFF.toInt(), 112f, display, 800))
        val range = "${week.weekStart.format(ShortDate)} to ${week.weekStart.plusDays(6).format(ShortDate)}"
        canvas.drawText("within budget, $range", 110f, 360f, paint(0xFFFFFFFF.toInt(), 42f, body, 500))
        ContextCompat.getDrawable(context, R.drawable.ic_launcher_foreground)?.apply {
            setBounds(740, 170, 1040, 470)
            draw(canvas)
        }

        // Four tiles.
        val tiles = listOf(
            Triple(0xFFFF7657.toInt(), "${week.streak}", "day streak"),
            Triple(0xFF57B8FF.toInt(), "${week.averagePerDay}", "reels a day"),
            Triple(0xFFFFC83D.toInt(), "${week.minutesWonBack} min", "won back"),
            Triple(0xFF34D1A0.toInt(), "${week.daysWithinBudget}/${week.trackedDays}", "good days"),
        )
        tiles.forEachIndexed { i, (colour, value, label) ->
            val left = 60f + (i % 2) * 490f
            val top = 520f + (i / 2) * 330f
            fill.color = colour
            canvas.drawRoundRect(RectF(left, top, left + 470f, top + 300f), 56f, 56f, fill)
            canvas.drawText(value, left + 44f, top + 170f, paint(INK, 104f, display, 800))
            canvas.drawText(label, left + 44f, top + 240f, paint(INK, 40f, body, 700))
        }

        canvas.drawText("Squad up: share your week too.", 60f, 1270f, paint(INK, 46f, body, 700))
        return bitmap
    }

    private val ShortDate = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
}
