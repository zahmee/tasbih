package com.sakinah.tasbih.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** What one side of a line above the day's prayers shows. */
enum class LineItem { NONE, HIJRI_DATE, GREGORIAN_DATE, WEEKDAY, PLACE, NEXT_PRAYER, COUNTDOWN, DHIKR }

/**
 * A colour for one line of words. [AUTO] keeps the widget's own choice: the accent for the day
 * widget's first line, the soft ink for the rest. The named colours are deep on light paper and
 * lifted on dark paper, as the accent is.
 */
enum class LineColor { AUTO, INK, SOFT, ACCENT, GREEN, COPPER, GOLD, BLUE, RED, PURPLE, WHITE, BLACK }

/** A line above the prayers: [right] leads in Arabic; when one side is empty the other is centred. */
data class WidgetLine(val right: LineItem, val left: LineItem = LineItem.NONE, val color: LineColor = LineColor.AUTO) {
    val isEmpty get() = right == LineItem.NONE && left == LineItem.NONE
}

/** The widget's paper. [AUTO] is Anaa's own, ivory by day and deep green by night. */
enum class WidgetSurface { AUTO, IVORY, WHITE, SAND, STONE, SKY, ROSE, PINE, NAVY, MAROON, COFFEE, NIGHT, BLACK }

/** The ink of the words. [AUTO] follows the paper. */
enum class WidgetInk { AUTO, DARK, LIGHT }

/** The colour that marks the next prayer and the ornament. */
enum class WidgetAccent { GREEN, COPPER, GOLD, BLUE, RED, PURPLE }

/** How strongly the accent is laid on. */
enum class WidgetIntensity { SOFT, NORMAL, STRONG }

/** How the next prayer stands out: a filled tile, a faint one, or a ruled outline. */
enum class NextMark { FILLED, FAINT, OUTLINE }

/**
 * The weight of the letters. Only two: a widget is drawn in Android's own Arabic font, which has
 * a regular and a bold, and the launcher loads neither the app's fonts nor a finer weight.
 */
enum class WidgetWeight { REGULAR, BOLD }

enum class WidgetScale { SMALL, MEDIUM, LARGE }

/**
 * The Islamic touch: none, a ruled star between the lines and the prayers, or that with a light
 * mihrab drawn round the paper. [PATTERN] keeps its stored name from when it was a pattern of
 * interlaced stars, which the user found too busy on November 23, 2026. [FRAME] was a ruled frame
 * with stars at its corners, found too slight; a saved choice of it reads as [PATTERN].
 */
/**
 * The widget's ornament. FRAME is kept only to read old choices, which now show as the mihrab.
 * The six after it came on November 28, 2026: see [IslamicFrames].
 */
enum class WidgetOrnament {
    NONE, DIVIDER, PATTERN, FRAME,
    KUFIC_CORNERS, ARABESQUE_CORNERS, CROWNED_FRAME, WINGS, FLOURISH, STEPPED_CORNERS,
}

data class PrayerWidgetStyle(
    val lines: List<WidgetLine> = listOf(WidgetLine(LineItem.PLACE, LineItem.HIJRI_DATE)),
    val surface: WidgetSurface = WidgetSurface.AUTO,
    /** From 0, fully clear, to 100, the paper solid. */
    val opacity: Int = 95,
    val ink: WidgetInk = WidgetInk.AUTO,
    val accent: WidgetAccent = WidgetAccent.GREEN,
    val intensity: WidgetIntensity = WidgetIntensity.NORMAL,
    val nextMark: NextMark = NextMark.FILLED,
    /** The weight of the lines and the prayers' names. */
    val weight: WidgetWeight = WidgetWeight.REGULAR,
    /** The weight of the prayers' times, bold as they always were. */
    val timeWeight: WidgetWeight = WidgetWeight.BOLD,
    /** The prayers' names and their times, each in a colour of the user's or the widget's own. */
    val nameColor: LineColor = LineColor.AUTO,
    val timeColor: LineColor = LineColor.AUTO,
    val scale: WidgetScale = WidgetScale.MEDIUM,
    val ornament: WidgetOrnament = WidgetOrnament.DIVIDER,
    /** How strongly the pattern is laid on the paper. */
    val patternIntensity: WidgetIntensity = WidgetIntensity.NORMAL,
    /** The Hijri date written with its month's number, «جمادى الآخرة (٦)». */
    val hijriMonthNumber: Boolean = false,
    /** The Gregorian date written with its month's number, «نوفمبر (١١)». */
    val gregorianMonthNumber: Boolean = false,
    /** The lines under the next prayer's name and time, in its own small widget. */
    val nextLines: List<WidgetLine> = listOf(WidgetLine(LineItem.PLACE)),
    /** A bar of the time passed since the last prayer, under the prayers. */
    val progress: Boolean = true,
    /**
     * The day widget's prayers together in one box, apart from the lines above them, as the user
     * asked on November 23, 2026: its own fill, of [boxSurface] at [boxOpacity], and a fine line.
     */
    val prayersBox: Boolean = false,
    val boxSurface: WidgetSurface = WidgetSurface.BLACK,
    /** From 0, no fill and the line alone, to 100, the fill solid. */
    val boxOpacity: Int = 25,
    val boxLine: Boolean = true,
    /** The colour that marks the next prayer; [LineColor.AUTO] is the accent. */
    val markColor: LineColor = LineColor.AUTO,
    /** The prayer whose time it is now is marked, rather than the one to come. */
    val markCurrent: Boolean = false,
    /** The bar's own colour, apart from the widget's; [LineColor.AUTO] is the accent. */
    val progressColor: LineColor = LineColor.AUTO,
    /** The bar goes from blue while the prayer is far to red as its adhan nears, in place of one colour. */
    val progressGradient: Boolean = false,
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

/**
 * Lines are kept as «RIGHT|LEFT|COLOUR;…»; lines saved before colours had two parts and read as
 * [LineColor.AUTO]. Anything unreadable is dropped, never fatal.
 */
internal object WidgetLines {
    fun encode(lines: List<WidgetLine>): String = lines.joinToString(";") { "${it.right.name}|${it.left.name}|${it.color.name}" }

    fun decode(saved: String?): List<WidgetLine>? = saved?.let { text ->
        if (text.isEmpty()) return emptyList()
        text.split(';').mapNotNull { line ->
            val parts = line.split('|')
            val right = LineItem.entries.firstOrNull { it.name == parts.getOrNull(0) } ?: return@mapNotNull null
            val left = LineItem.entries.firstOrNull { it.name == parts.getOrNull(1) } ?: LineItem.NONE
            val color = LineColor.entries.firstOrNull { it.name == parts.getOrNull(2) } ?: LineColor.AUTO
            WidgetLine(right, left, color)
        }.take(PrayerWidgetStyle.MAX_LINES)
    }
}

// A file of its own, so the widget's look can never damage prayer or tasbih settings.
private val Context.widgetDataStore by preferencesDataStore(name = "widget_preferences")

/**
 * The look of the prayer widgets, one for each widget on the home screen, as the user asked on
 * November 28, 2026, so two widgets of a kind can look apart. Each is kept under its widget's id,
 * «w12_day_surface»; the keys without an id are the default, the one style there was until then.
 * A widget with no style of its own takes the default, and [stylesFor] gives it that default for
 * its own the first time it is drawn, so a later change to the default leaves it as it is. Saving
 * a widget's style makes it the default too, so a widget placed next starts from the last look.
 */
class PrayerWidgetStyleRepository internal constructor(private val dataStore: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.widgetDataStore)

    /** The default, for a widget placed later. */
    val style: Flow<PrayerWidgetStyle> = dataStore.data.map { read(it, Keys.DEFAULT) }

    /** The style of the widget [appWidgetId], or the default where it is null. */
    fun style(appWidgetId: Int?): Flow<PrayerWidgetStyle> = dataStore.data.map { styleIn(it, appWidgetId) }

    suspend fun current(): PrayerWidgetStyle = style.first()

    suspend fun current(appWidgetId: Int?): PrayerWidgetStyle = style(appWidgetId).first()

    /** The ids that have a style of their own. */
    suspend fun styledIds(): Set<Int> = ids(dataStore.data.first())

    /**
     * The style of each of [appWidgetIds]. One that has none of its own is given the default
     * for its own now: this is how the widgets placed before styles were apart keep their look.
     */
    suspend fun stylesFor(appWidgetIds: Collection<Int>): Map<Int, PrayerWidgetStyle> {
        val known = ids(dataStore.data.first())
        if (appWidgetIds.any { it !in known }) {
            dataStore.edit { p ->
                val styled = ids(p)
                val missing = appWidgetIds.filterNot { it in styled }
                val default = read(p, Keys.DEFAULT)
                missing.forEach { write(p, Keys.of(it), default) }
                p[Keys.styled] = names(styled + missing)
            }
        }
        val prefs = dataStore.data.first()
        return appWidgetIds.associateWith { styleIn(prefs, it) }
    }

    /** Saves the default. */
    suspend fun save(style: PrayerWidgetStyle) = save(null, style)

    /** Saves [style] for [appWidgetId], and as the default for a widget placed next. */
    suspend fun save(appWidgetId: Int?, style: PrayerWidgetStyle) = saveAll(listOfNotNull(appWidgetId), style)

    /** «تطبيق على كل الأدوات»: [style] for each of [appWidgetIds], and as the default. */
    suspend fun saveAll(appWidgetIds: Collection<Int>, style: PrayerWidgetStyle) {
        dataStore.edit { p ->
            write(p, Keys.DEFAULT, style)
            appWidgetIds.forEach { write(p, Keys.of(it), style) }
            if (appWidgetIds.isNotEmpty()) p[Keys.styled] = names(ids(p) + appWidgetIds)
        }
    }

    /** Forgets the styles of widgets taken off the home screen. */
    suspend fun remove(appWidgetIds: Collection<Int>) {
        dataStore.edit { p ->
            appWidgetIds.forEach { id -> Keys.of(id).all.forEach { p.remove(it) } }
            p[Keys.styled] = names(ids(p) - appWidgetIds.toSet())
        }
    }

    /**
     * After a backup is restored the launcher gives its widgets new ids: each style moves to its
     * widget's new id. A widget whose old id had no style of its own keeps what it has.
     */
    suspend fun remap(oldIds: IntArray, newIds: IntArray) {
        dataStore.edit { p ->
            val styled = ids(p)
            val moved = oldIds.indices
                .filter { it < newIds.size && oldIds[it] in styled }
                .associate { newIds[it] to read(p, Keys.of(oldIds[it])) }
            // The old ones all go first, so an old id that is also another widget's new one is not lost.
            oldIds.forEach { id -> Keys.of(id).all.forEach { p.remove(it) } }
            moved.forEach { (id, style) -> write(p, Keys.of(id), style) }
            p[Keys.styled] = names(styled - oldIds.toSet() + moved.keys)
        }
    }

    private fun ids(p: Preferences): Set<Int> = p[Keys.styled].orEmpty().mapNotNull(String::toIntOrNull).toSet()

    private fun names(ids: Collection<Int>): Set<String> = ids.map(Int::toString).toSet()

    private fun styleIn(p: Preferences, appWidgetId: Int?): PrayerWidgetStyle =
        if (appWidgetId != null && appWidgetId in ids(p)) read(p, Keys.of(appWidgetId)) else read(p, Keys.DEFAULT)

    private fun write(it: MutablePreferences, keys: Keys, style: PrayerWidgetStyle) {
        it[keys.lines] = WidgetLines.encode(style.lines)
        it[keys.surface] = style.surface.name
        it[keys.opacity] = style.opacity.coerceIn(0, 100)
        it[keys.ink] = style.ink.name
        it[keys.accent] = style.accent.name
        it[keys.intensity] = style.intensity.name
        it[keys.nextMark] = style.nextMark.name
        it[keys.weight] = style.weight.name
        it[keys.timeWeight] = style.timeWeight.name
        it[keys.nameColor] = style.nameColor.name
        it[keys.timeColor] = style.timeColor.name
        it[keys.scale] = style.scale.name
        it[keys.ornament] = style.ornament.name
        it[keys.patternIntensity] = style.patternIntensity.name
        it[keys.hijriMonthNumber] = style.hijriMonthNumber
        it[keys.gregorianMonthNumber] = style.gregorianMonthNumber
        it[keys.nextLines] = WidgetLines.encode(style.nextLines)
        it[keys.progress] = style.progress
        it[keys.prayersBox] = style.prayersBox
        it[keys.boxSurface] = style.boxSurface.name
        it[keys.boxOpacity] = style.boxOpacity.coerceIn(0, 100)
        it[keys.boxLine] = style.boxLine
        it[keys.markColor] = style.markColor.name
        it[keys.markCurrent] = style.markCurrent
        it[keys.progressColor] = style.progressColor.name
        it[keys.progressGradient] = style.progressGradient
    }

    private fun read(p: Preferences, keys: Keys): PrayerWidgetStyle {
        val default = PrayerWidgetStyle()
        return PrayerWidgetStyle(
            lines = WidgetLines.decode(p[keys.lines]) ?: default.lines,
            surface = enumOr(p[keys.surface], default.surface),
            opacity = (p[keys.opacity] ?: default.opacity).coerceIn(0, 100),
            ink = enumOr(p[keys.ink], default.ink),
            accent = enumOr(p[keys.accent], default.accent),
            intensity = enumOr(p[keys.intensity], default.intensity),
            nextMark = enumOr(p[keys.nextMark], default.nextMark),
            weight = enumOr(p[keys.weight], default.weight),
            timeWeight = enumOr(p[keys.timeWeight], default.timeWeight),
            nameColor = enumOr(p[keys.nameColor], default.nameColor),
            timeColor = enumOr(p[keys.timeColor], default.timeColor),
            scale = enumOr(p[keys.scale], default.scale),
            ornament = enumOr(p[keys.ornament], default.ornament).let { if (it == WidgetOrnament.FRAME) WidgetOrnament.PATTERN else it },
            patternIntensity = enumOr(p[keys.patternIntensity], default.patternIntensity),
            hijriMonthNumber = p[keys.hijriMonthNumber] ?: default.hijriMonthNumber,
            gregorianMonthNumber = p[keys.gregorianMonthNumber] ?: default.gregorianMonthNumber,
            nextLines = WidgetLines.decode(p[keys.nextLines])?.take(PrayerWidgetStyle.MAX_NEXT_LINES) ?: default.nextLines,
            progress = p[keys.progress] ?: default.progress,
            prayersBox = p[keys.prayersBox] ?: default.prayersBox,
            boxSurface = enumOr(p[keys.boxSurface], default.boxSurface),
            boxOpacity = (p[keys.boxOpacity] ?: default.boxOpacity).coerceIn(0, 100),
            boxLine = p[keys.boxLine] ?: default.boxLine,
            markColor = enumOr(p[keys.markColor], default.markColor),
            markCurrent = p[keys.markCurrent] ?: default.markCurrent,
            progressColor = enumOr(p[keys.progressColor], default.progressColor),
            progressGradient = p[keys.progressGradient] ?: default.progressGradient,
        )
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default

    /** The keys of one style; the default's are the names kept since the first version. */
    private class Keys(prefix: String) {
        val lines = stringPreferencesKey("${prefix}day_lines")
        val surface = stringPreferencesKey("${prefix}day_surface")
        val opacity = intPreferencesKey("${prefix}day_opacity")
        val ink = stringPreferencesKey("${prefix}day_ink")
        val accent = stringPreferencesKey("${prefix}day_accent")
        val intensity = stringPreferencesKey("${prefix}day_intensity")
        val nextMark = stringPreferencesKey("${prefix}day_next_mark")
        val weight = stringPreferencesKey("${prefix}day_weight")
        val timeWeight = stringPreferencesKey("${prefix}day_time_weight")
        val nameColor = stringPreferencesKey("${prefix}day_name_color")
        val timeColor = stringPreferencesKey("${prefix}day_time_color")
        val scale = stringPreferencesKey("${prefix}day_scale")
        val ornament = stringPreferencesKey("${prefix}day_ornament")
        val patternIntensity = stringPreferencesKey("${prefix}day_pattern_intensity")
        val hijriMonthNumber = booleanPreferencesKey("${prefix}day_hijri_month_number")
        val gregorianMonthNumber = booleanPreferencesKey("${prefix}day_gregorian_month_number")
        val nextLines = stringPreferencesKey("${prefix}next_lines")
        val progress = booleanPreferencesKey("${prefix}progress")
        val prayersBox = booleanPreferencesKey("${prefix}day_prayers_box")
        val boxSurface = stringPreferencesKey("${prefix}day_box_surface")
        val boxOpacity = intPreferencesKey("${prefix}day_box_opacity")
        val boxLine = booleanPreferencesKey("${prefix}day_box_line")
        val markColor = stringPreferencesKey("${prefix}mark_color")
        val markCurrent = booleanPreferencesKey("${prefix}mark_current")
        val progressColor = stringPreferencesKey("${prefix}progress_color")
        val progressGradient = booleanPreferencesKey("${prefix}progress_gradient")

        val all: List<Preferences.Key<*>> = listOf(
            lines, surface, opacity, ink, accent, intensity, nextMark, weight, timeWeight, nameColor, timeColor, scale,
            ornament, patternIntensity, hijriMonthNumber, gregorianMonthNumber, nextLines, progress, prayersBox,
            boxSurface, boxOpacity, boxLine, markColor, markCurrent, progressColor, progressGradient,
        )

        companion object {
            val DEFAULT = Keys("")

            fun of(appWidgetId: Int) = Keys("w${appWidgetId}_")

            /** The widgets that have a style of their own. */
            val styled = stringSetPreferencesKey("styled_widgets")
        }
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
    /** Whether the paper is dark, so named colours are lifted to keep their contrast. */
    val darkPaper: Boolean,
    /** The pattern's alpha over the paper, from its own intensity. */
    val patternAlpha: Int,
    /** The strength of the frames and corners, which stand out more than the mihrab. */
    val frameAlpha: Int = 150,
) {
    /** A line's colour, or null for [LineColor.AUTO], where the widget keeps its own. */
    fun line(color: LineColor): Int? = when (color) {
        LineColor.AUTO -> null
        LineColor.INK -> text
        LineColor.SOFT -> textSoft
        LineColor.ACCENT -> accent
        LineColor.GREEN -> named(WidgetAccent.GREEN, darkPaper)
        LineColor.COPPER -> named(WidgetAccent.COPPER, darkPaper)
        LineColor.GOLD -> named(WidgetAccent.GOLD, darkPaper)
        LineColor.BLUE -> named(WidgetAccent.BLUE, darkPaper)
        LineColor.RED -> named(WidgetAccent.RED, darkPaper)
        LineColor.PURPLE -> named(WidgetAccent.PURPLE, darkPaper)
        LineColor.WHITE -> 0xFFFFFFFF.toInt()
        LineColor.BLACK -> 0xFF000000.toInt()
    }

    companion object {
        /**
         * The colours inside the prayers' box. Once its fill is strong enough to be what the
         * words stand on, the ink and the accent follow it, so a dark box takes light words;
         * a fainter box leaves them as on the paper. The next prayer's own colour, if the user
         * chose one, is kept either way.
         */
        fun inner(style: PrayerWidgetStyle, night: Boolean): WidgetPalette {
            val paper = of(style, night)
            val onBox = style.prayersBox && style.boxOpacity >= BOX_SETS_INK
            val base = if (onBox) of(style.copy(surface = style.boxSurface, ink = WidgetInk.AUTO), night) else paper
            val mark = base.line(style.markColor) ?: base.accent
            return base.copy(accent = mark, onAccent = if (luminance(mark) > 0.5) 0xFF0E1A16.toInt() else 0xFFFFFBF3.toInt())
        }

        /** The box's fill at this opacity or more decides the colour of the words on it. */
        const val BOX_SETS_INK = 50

        /** The box's own colour, solid; its strength is set apart, on the picture. */
        fun boxColor(style: PrayerWidgetStyle): Int = of(style.copy(surface = style.boxSurface), false).surface

        fun of(style: PrayerWidgetStyle, night: Boolean): WidgetPalette {
            val surface = when (style.surface) {
                WidgetSurface.AUTO -> if (night) 0xFF10251F else 0xFFFAF8F2
                WidgetSurface.IVORY -> 0xFFFAF8F2
                WidgetSurface.WHITE -> 0xFFFFFFFF
                WidgetSurface.SAND -> 0xFFEFE3CF
                WidgetSurface.STONE -> 0xFFE4E6E3
                WidgetSurface.SKY -> 0xFFDCEBF5
                WidgetSurface.ROSE -> 0xFFF6E3E0
                WidgetSurface.PINE -> 0xFF185348
                WidgetSurface.NAVY -> 0xFF1C2E4A
                WidgetSurface.MAROON -> 0xFF5A2328
                WidgetSurface.COFFEE -> 0xFF4A3528
                WidgetSurface.NIGHT -> 0xFF141C1A
                WidgetSurface.BLACK -> 0xFF000000
            }.toInt()
            val darkPaper = luminance(surface) < 0.45
            val lightInk = when (style.ink) {
                WidgetInk.AUTO -> darkPaper
                WidgetInk.DARK -> false
                WidgetInk.LIGHT -> true
            }
            val text = if (lightInk) 0xFFF1EEE6.toInt() else 0xFF17231F.toInt()
            val textSoft = if (lightInk) 0xFFB9C6C0.toInt() else 0xFF56645F.toInt()
            val accent = named(style.accent, darkPaper)
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
            // Faint enough that the words over it read as clearly as on plain paper.
            val patternAlpha = when (style.patternIntensity) {
                WidgetIntensity.SOFT -> 38
                WidgetIntensity.NORMAL -> 64
                WidgetIntensity.STRONG -> 100
            }
            // The frames and corners are the ornament itself, not a watermark, so they show more.
            val frameAlpha = when (style.patternIntensity) {
                WidgetIntensity.SOFT -> 90
                WidgetIntensity.NORMAL -> 150
                WidgetIntensity.STRONG -> 225
            }
            return WidgetPalette(surface, text, textSoft, accent, onAccent, markAlpha, darkPaper, patternAlpha, frameAlpha)
        }

        /** On dark paper a colour is lifted so it keeps its contrast. */
        internal fun named(accent: WidgetAccent, darkPaper: Boolean): Int = when (accent) {
            WidgetAccent.GREEN -> if (darkPaper) 0xFF8ED8C0 else 0xFF185348
            WidgetAccent.COPPER -> if (darkPaper) 0xFFE3AC7C else 0xFF94562A
            WidgetAccent.GOLD -> if (darkPaper) 0xFFE6C77A else 0xFFA0761E
            WidgetAccent.BLUE -> if (darkPaper) 0xFF94BDEB else 0xFF235A92
            WidgetAccent.RED -> if (darkPaper) 0xFFF0A0A0 else 0xFFA23636
            WidgetAccent.PURPLE -> if (darkPaper) 0xFFC6A9E8 else 0xFF684592
        }.toInt()

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


    /**
     * The bar's colour with [passed] of the time between the prayers gone, from 0 to 1: its own
     * colour, or on the way from blue to red, the red reached at the adhan.
     */
    fun progress(style: PrayerWidgetStyle, passed: Float): Int = when {
        style.progressGradient -> blend(named(WidgetAccent.BLUE, darkPaper), named(WidgetAccent.RED, darkPaper), passed)
        else -> line(style.progressColor) ?: accent
    }
}

/** [from] turned towards [to] by [share], from 0 to 1, channel by channel. */
internal fun blend(from: Int, to: Int, share: Float): Int {
    val t = share.coerceIn(0f, 1f)
    fun channel(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).toInt().coerceIn(0, 255) shl shift
    }
    return channel(24) or channel(16) or channel(8) or channel(0)
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
