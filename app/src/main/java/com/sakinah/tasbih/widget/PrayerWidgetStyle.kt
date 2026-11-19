package com.sakinah.tasbih.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** What one side of a line above the day's prayers shows. */
enum class LineItem { NONE, HIJRI_DATE, GREGORIAN_DATE, WEEKDAY, PLACE, NEXT_PRAYER, COUNTDOWN, DHIKR }

/** A line above the prayers: [right] leads in Arabic; when one side is empty the other is centred. */
data class WidgetLine(val right: LineItem, val left: LineItem = LineItem.NONE) {
    val isEmpty get() = right == LineItem.NONE && left == LineItem.NONE
}

/** The widget's paper. [AUTO] is Anaa's own, ivory by day and deep green by night. */
enum class WidgetSurface { AUTO, IVORY, SAND, PINE, NIGHT }

/** The ink of the words. [AUTO] follows the paper. */
enum class WidgetInk { AUTO, DARK, LIGHT }

/** The colour that marks the next prayer and the ornament. */
enum class WidgetAccent { GREEN, COPPER, GOLD, BLUE, RED, PURPLE }

/** How strongly the accent is laid on. */
enum class WidgetIntensity { SOFT, NORMAL, STRONG }

/** How the next prayer stands out: a filled tile, a faint one, or a ruled outline. */
enum class NextMark { FILLED, FAINT, OUTLINE }

enum class WidgetWeight { REGULAR, BOLD }

enum class WidgetScale { SMALL, MEDIUM, LARGE }

/** The Islamic touch: none, a ruled star between the lines and the prayers, or that and a frame with stars at its corners. */
enum class WidgetOrnament { NONE, DIVIDER, FRAME }

data class PrayerWidgetStyle(
    val lines: List<WidgetLine> = listOf(WidgetLine(LineItem.PLACE, LineItem.HIJRI_DATE)),
    val surface: WidgetSurface = WidgetSurface.AUTO,
    /** From 0, fully clear, to 100, the paper solid. */
    val opacity: Int = 95,
    val ink: WidgetInk = WidgetInk.AUTO,
    val accent: WidgetAccent = WidgetAccent.GREEN,
    val intensity: WidgetIntensity = WidgetIntensity.NORMAL,
    val nextMark: NextMark = NextMark.FILLED,
    val weight: WidgetWeight = WidgetWeight.REGULAR,
    val scale: WidgetScale = WidgetScale.MEDIUM,
    val ornament: WidgetOrnament = WidgetOrnament.DIVIDER,
    /** The Hijri date written with its month's number, «جمادى الآخرة (٦)». */
    val hijriMonthNumber: Boolean = false,
    /** The Gregorian date written with its month's number, «نوفمبر (١١)». */
    val gregorianMonthNumber: Boolean = false,
    /** The lines under the next prayer's name and time, in its own small widget. */
    val nextLines: List<WidgetLine> = listOf(WidgetLine(LineItem.PLACE)),
    /** A bar of the time passed since the last prayer, under the prayers. */
    val progress: Boolean = true,
) {
    companion object {
        const val MAX_LINES = 3
        const val MAX_NEXT_LINES = 2
    }
}

/**
 * How far from the last prayer to the next [now] is, from 0 to [max]. Clamped, so a clock a
 * moment late or early never draws the bar past its ends.
 */
fun passedShare(previous: java.time.Instant, next: java.time.Instant, now: java.time.Instant, max: Int = 1000): Int {
    val span = java.time.Duration.between(previous, next).toMillis()
    if (span <= 0) return max
    val passed = java.time.Duration.between(previous, now).toMillis()
    return (passed * max / span).toInt().coerceIn(0, max)
}

/** Lines are kept as «RIGHT|LEFT;RIGHT|LEFT»; anything unreadable is dropped, never fatal. */
internal object WidgetLines {
    fun encode(lines: List<WidgetLine>): String = lines.joinToString(";") { "${it.right.name}|${it.left.name}" }

    fun decode(saved: String?): List<WidgetLine>? = saved?.let { text ->
        if (text.isEmpty()) return emptyList()
        text.split(';').mapNotNull { line ->
            val parts = line.split('|')
            val right = LineItem.entries.firstOrNull { it.name == parts.getOrNull(0) } ?: return@mapNotNull null
            val left = LineItem.entries.firstOrNull { it.name == parts.getOrNull(1) } ?: LineItem.NONE
            WidgetLine(right, left)
        }.take(PrayerWidgetStyle.MAX_LINES)
    }
}

// A file of its own, so the widget's look can never damage prayer or tasbih settings.
private val Context.widgetDataStore by preferencesDataStore(name = "widget_preferences")

class PrayerWidgetStyleRepository internal constructor(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.widgetDataStore)

    val style: Flow<PrayerWidgetStyle> = dataStore.data.map(::read)

    suspend fun current(): PrayerWidgetStyle = style.first()

    suspend fun save(style: PrayerWidgetStyle) = dataStore.edit {
        it[Keys.lines] = WidgetLines.encode(style.lines)
        it[Keys.surface] = style.surface.name
        it[Keys.opacity] = style.opacity.coerceIn(0, 100)
        it[Keys.ink] = style.ink.name
        it[Keys.accent] = style.accent.name
        it[Keys.intensity] = style.intensity.name
        it[Keys.nextMark] = style.nextMark.name
        it[Keys.weight] = style.weight.name
        it[Keys.scale] = style.scale.name
        it[Keys.ornament] = style.ornament.name
        it[Keys.hijriMonthNumber] = style.hijriMonthNumber
        it[Keys.gregorianMonthNumber] = style.gregorianMonthNumber
        it[Keys.nextLines] = WidgetLines.encode(style.nextLines)
        it[Keys.progress] = style.progress
    }

    private fun read(p: Preferences): PrayerWidgetStyle {
        val default = PrayerWidgetStyle()
        return PrayerWidgetStyle(
            lines = WidgetLines.decode(p[Keys.lines]) ?: default.lines,
            surface = enumOr(p[Keys.surface], default.surface),
            opacity = (p[Keys.opacity] ?: default.opacity).coerceIn(0, 100),
            ink = enumOr(p[Keys.ink], default.ink),
            accent = enumOr(p[Keys.accent], default.accent),
            intensity = enumOr(p[Keys.intensity], default.intensity),
            nextMark = enumOr(p[Keys.nextMark], default.nextMark),
            weight = enumOr(p[Keys.weight], default.weight),
            scale = enumOr(p[Keys.scale], default.scale),
            ornament = enumOr(p[Keys.ornament], default.ornament),
            hijriMonthNumber = p[Keys.hijriMonthNumber] ?: default.hijriMonthNumber,
            gregorianMonthNumber = p[Keys.gregorianMonthNumber] ?: default.gregorianMonthNumber,
            nextLines = WidgetLines.decode(p[Keys.nextLines])?.take(PrayerWidgetStyle.MAX_NEXT_LINES) ?: default.nextLines,
            progress = p[Keys.progress] ?: default.progress,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    private object Keys {
        val lines = stringPreferencesKey("day_lines")
        val surface = stringPreferencesKey("day_surface")
        val opacity = intPreferencesKey("day_opacity")
        val ink = stringPreferencesKey("day_ink")
        val accent = stringPreferencesKey("day_accent")
        val intensity = stringPreferencesKey("day_intensity")
        val nextMark = stringPreferencesKey("day_next_mark")
        val weight = stringPreferencesKey("day_weight")
        val scale = stringPreferencesKey("day_scale")
        val ornament = stringPreferencesKey("day_ornament")
        val hijriMonthNumber = booleanPreferencesKey("day_hijri_month_number")
        val gregorianMonthNumber = booleanPreferencesKey("day_gregorian_month_number")
        val nextLines = stringPreferencesKey("next_lines")
        val progress = booleanPreferencesKey("progress")
    }
}

/**
 * The colours of a style as plain ARGB, shared by the widget and the preview in the app so the
 * two cannot drift apart. [night] decides the automatic paper and ink.
 */
data class WidgetPalette(
    val surface: Int,
    val text: Int,
    val textSoft: Int,
    val accent: Int,
    /** The words on a filled next-prayer tile. */
    val onAccent: Int,
    /** The tile's own alpha, from the intensity. */
    val markAlpha: Int,
) {
    companion object {
        fun of(style: PrayerWidgetStyle, night: Boolean): WidgetPalette {
            val surface = when (style.surface) {
                WidgetSurface.AUTO -> if (night) 0xFF10251F else 0xFFFAF8F2
                WidgetSurface.IVORY -> 0xFFFAF8F2
                WidgetSurface.SAND -> 0xFFEFE3CF
                WidgetSurface.PINE -> 0xFF185348
                WidgetSurface.NIGHT -> 0xFF141C1A
            }.toInt()
            val darkPaper = luminance(surface) < 0.45
            val lightInk = when (style.ink) {
                WidgetInk.AUTO -> darkPaper
                WidgetInk.DARK -> false
                WidgetInk.LIGHT -> true
            }
            val text = if (lightInk) 0xFFF1EEE6.toInt() else 0xFF17231F.toInt()
            val textSoft = if (lightInk) 0xFFB9C6C0.toInt() else 0xFF56645F.toInt()
            // On dark paper the accent is lifted so it keeps its contrast.
            val accent = when (style.accent) {
                WidgetAccent.GREEN -> if (darkPaper) 0xFF8ED8C0 else 0xFF185348
                WidgetAccent.COPPER -> if (darkPaper) 0xFFE3AC7C else 0xFF94562A
                WidgetAccent.GOLD -> if (darkPaper) 0xFFE6C77A else 0xFFA0761E
                WidgetAccent.BLUE -> if (darkPaper) 0xFF94BDEB else 0xFF235A92
                WidgetAccent.RED -> if (darkPaper) 0xFFF0A0A0 else 0xFFA23636
                WidgetAccent.PURPLE -> if (darkPaper) 0xFFC6A9E8 else 0xFF684592
            }.toInt()
            val onAccent = if (luminance(accent) > 0.5) 0xFF0E1A16.toInt() else 0xFFFFFBF3.toInt()
            val markAlpha = when (style.nextMark) {
                NextMark.FILLED -> when (style.intensity) {
                    WidgetIntensity.SOFT -> 170
                    WidgetIntensity.NORMAL -> 225
                    WidgetIntensity.STRONG -> 255
                }
                NextMark.FAINT -> when (style.intensity) {
                    WidgetIntensity.SOFT -> 28
                    WidgetIntensity.NORMAL -> 50
                    WidgetIntensity.STRONG -> 80
                }
                NextMark.OUTLINE -> when (style.intensity) {
                    WidgetIntensity.SOFT -> 120
                    WidgetIntensity.NORMAL -> 190
                    WidgetIntensity.STRONG -> 255
                }
            }
            return WidgetPalette(surface, text, textSoft, accent, onAccent, markAlpha)
        }

        private fun luminance(color: Int): Double {
            fun channel(shift: Int): Double {
                val c = ((color shr shift) and 0xFF) / 255.0
                return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channel(16) + 0.7152 * channel(8) + 0.0722 * channel(0)
        }
    }

    /** The words of the next prayer, on its tile. */
    fun nextText(mark: NextMark): Int = if (mark == NextMark.FILLED) onAccent else accent
}

/** Short remembrances for the line of the day, one each day in turn. */
internal val WidgetDhikr = listOf(
    "سبحان الله وبحمده",
    "لا إله إلا الله",
    "الحمد لله",
    "الله أكبر",
    "أستغفر الله",
    "اللهم صلِّ على محمد",
    "لا حول ولا قوة إلا بالله",
)
