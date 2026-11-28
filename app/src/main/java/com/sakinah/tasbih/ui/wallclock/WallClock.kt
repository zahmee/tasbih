package com.sakinah.tasbih.ui.wallclock

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.sakinah.tasbih.R
import com.sakinah.tasbih.calendar.HijriCalendar
import com.sakinah.tasbih.calendar.hijriDayFor
import com.sakinah.tasbih.data.ArabicFontStyle
import com.sakinah.tasbih.data.CalendarPreferencesRepository
import com.sakinah.tasbih.data.PrayerPreferencesRepository
import com.sakinah.tasbih.data.ThemeMode
import com.sakinah.tasbih.data.arabicNumber
import com.sakinah.tasbih.data.twoDigits
import com.sakinah.tasbih.prayer.ObligatoryPrayers
import com.sakinah.tasbih.prayer.Prayer
import com.sakinah.tasbih.prayer.PrayerPhase
import com.sakinah.tasbih.prayer.PrayerSchedule
import com.sakinah.tasbih.ui.SettingToggleRow
import com.sakinah.tasbih.ui.calendar.CalendarText
import com.sakinah.tasbih.ui.prayer.PrayerText
import com.sakinah.tasbih.ui.theme.LocalSakinahBrandColors
import com.sakinah.tasbih.ui.theme.SakinahTheme
import com.sakinah.tasbih.ui.theme.dhikrFontFor
import com.sakinah.tasbih.widget.WidgetDhikr
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime

/** What the wall clock may be told: whether it keeps the screen on, dims at night, and shows seconds. */
data class WallClockOptions(
    val keepScreenOn: Boolean = true,
    val dimAtNight: Boolean = true,
    val showSeconds: Boolean = true,
)

/**
 * Kept beside the digits' choice rather than in the app's store, so the system's screen saver,
 * which runs without the app's screens, reads the same choices at once.
 */
internal object WallClockStore {
    private const val STORE = "wall_clock"

    fun read(context: Context): WallClockOptions = context.getSharedPreferences(STORE, Context.MODE_PRIVATE).let {
        WallClockOptions(
            keepScreenOn = it.getBoolean("keep_screen_on", true),
            dimAtNight = it.getBoolean("dim_at_night", true),
            showSeconds = it.getBoolean("show_seconds", true),
        )
    }

    fun save(context: Context, options: WallClockOptions) {
        context.getSharedPreferences(STORE, Context.MODE_PRIVATE).edit()
            .putBoolean("keep_screen_on", options.keepScreenOn)
            .putBoolean("dim_at_night", options.dimAtNight)
            .putBoolean("show_seconds", options.showSeconds)
            .apply()
    }
}

/** The day's times and the calendar, read as the widgets read them, so the screen saver needs no app screen. */
private class WallClockData(val schedule: PrayerSchedule?, val calendarText: CalendarText)

/**
 * The wall clock: the time large, the day and both dates, the six prayers with their adhan and
 * iqama, and how long to the next. For a tablet left on a wall, as the user asked on November 28,
 * 2026. The same face serves the screen in the app, where [onExit] leaves it and a tap shows its
 * choices, and the system's screen saver, which has no [onExit] and ends at a touch.
 *
 * It is always dark, as it stays lit through the night, and at night it dims [window] and itself.
 * Every few minutes it moves a few points, so nothing stays long enough to mark the screen.
 */
@Composable
fun WallClock(window: Window?, arabicFontStyle: ArabicFontStyle, onExit: (() -> Unit)?) {
    val context = LocalContext.current
    val app = context.applicationContext
    var options by remember { mutableStateOf(WallClockStore.read(app)) }
    val data by produceState<WallClockData?>(null, app) {
        combine(PrayerPreferencesRepository(app).settings, CalendarPreferencesRepository(app).settings) { prayer, calendar ->
            WallClockData(prayer.place?.let { PrayerSchedule(it, prayer, HijriCalendar.from(calendar)) }, CalendarText(app, calendar))
        }.collect { value = it }
    }
    val now = rememberSecondTick()
    val night = options.dimAtNight && isNight(data?.schedule, now)

    // The screen stays on only while this clock is shown, never for the rest of the app.
    val view = LocalView.current
    DisposableEffect(view, options.keepScreenOn) {
        view.keepScreenOn = options.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }
    DisposableEffect(window, night) {
        window?.setBrightness(if (night) NIGHT_BRIGHTNESS else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        onDispose { window?.setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE) }
    }
    // In the app the bars go, as a clock on a wall has none; a swipe from the edge shows them a while.
    if (onExit != null && window != null) {
        DisposableEffect(window, view) {
            val controller = WindowCompat.getInsetsController(window, view)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        SakinahTheme(dynamicColor = false, themeMode = ThemeMode.Dark, arabicFontStyle = arabicFontStyle) {
            var controls by rememberSaveable { mutableStateOf(false) }
            var touched by remember { mutableIntStateOf(0) }
            // The choices leave by themselves, so the clock is never left covered.
            LaunchedEffect(controls, touched) {
                if (controls) {
                    delay(CONTROLS_SHOWN_MS)
                    controls = false
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .testTag("wall_clock")
                    .then(
                        if (onExit != null) {
                            Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                controls = !controls
                                touched++
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                WallClockFace(
                    data = data,
                    now = now,
                    options = options,
                    arabicFontStyle = arabicFontStyle,
                    modifier = Modifier.alpha(if (night) NIGHT_ALPHA else 1f),
                )
                if (onExit != null) {
                    AnimatedVisibility(
                        visible = controls,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.BottomCenter),
                    ) {
                        WallClockControls(
                            options = options,
                            onChange = {
                                options = it
                                WallClockStore.save(app, it)
                                touched++
                            },
                            onExit = onExit,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WallClockFace(
    data: WallClockData?,
    now: ZonedDateTime,
    options: WallClockOptions,
    arabicFontStyle: ArabicFontStyle,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val text = remember(context) { PrayerText(context) }
    val schedule = data?.schedule
    val calendarText = data?.calendarText
    val instant = now.toInstant()
    val today = schedule?.today(instant) ?: now.toLocalDate()
    // Each second, so the count turns from the adhan to the iqama on the very second.
    val phase = schedule?.phase(instant)
    // After the night's last prayer the table turns to the next day, whose dawn comes next.
    val tableDate = if (phase is PrayerPhase.ToAdhan && phase.next.tomorrow) today.plusDays(1) else today
    val day = remember(schedule, tableDate) { schedule?.day(tableDate) }
    val hijri = calendarText?.let { cal ->
        val maghrib = schedule?.day(today)?.get(Prayer.MAGHRIB)?.toInstant()
        cal.hijriDate(hijriDayFor(today, instant, maghrib, cal.settings.hijriDayFromMaghrib), numbered = false)
    }.orEmpty()
    val gregorian = calendarText?.gregorianDate(today, numbered = false).orEmpty()
    val weekday = calendarText?.weekday(today.dayOfWeek).orEmpty()
    val gold = LocalSakinahBrandColors.current.antiqueGold
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    // A slow walk of a few points, a step every three minutes, against marks on the screen.
    val step = ((now.toEpochSecond() / SHIFT_SECONDS) % SHIFTS.size).toInt()
    val shift = SHIFTS[step]

    BoxWithConstraints(modifier.fillMaxSize().offset(shift.first.dp, shift.second.dp)) {
        val landscape = maxWidth > maxHeight
        // One measure for every size, from the screen and not the font setting, so the face fills a
        // tablet as it fills a phone, and a large font cannot push it off the screen.
        val unit: Dp = if (landscape) minOf(maxWidth / 100, maxHeight / 62) else minOf(maxWidth / 62, maxHeight / 100)
        val density = LocalDensity.current
        fun size(units: Float): TextUnit = with(density) { (unit * units).toSp() }
        val pad = unit * 4

        val clockSide: @Composable (Modifier) -> Unit = { sideModifier ->
            Column(sideModifier, verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (schedule != null) {
                        Icon(Icons.Outlined.LocationOn, null, tint = muted, modifier = Modifier.size(unit * 2.4f))
                        Spacer(Modifier.width(unit * 0.6f))
                        Text(schedule.place.name, color = muted, fontSize = size(2f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Text(stringResource(R.string.app_name), color = gold, fontSize = size(2f))
                }
                Column {
                    val clock = text.clock(now)
                    val half = text.time(now).removePrefix(clock).trim()
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            clock,
                            color = ink,
                            fontSize = size(16f),
                            fontWeight = FontWeight.Medium,
                            lineHeight = size(17f),
                            modifier = Modifier.testTag("wall_time"),
                        )
                        Column(Modifier.padding(start = unit * 1.2f, bottom = unit * 2f)) {
                            if (options.showSeconds) Text(twoDigits(now.second), color = muted, fontSize = size(4.5f), modifier = Modifier.testTag("wall_seconds"))
                            if (half.isNotEmpty()) Text(half, color = muted, fontSize = size(3f))
                        }
                    }
                    Spacer(Modifier.height(unit * 1.5f))
                    Text(weekday, color = ink, fontSize = size(4.2f), fontWeight = FontWeight.Medium)
                    Text(hijri, color = gold, fontSize = size(3.2f), modifier = Modifier.padding(top = unit * 0.4f))
                    Text(gregorian, color = muted, fontSize = size(2.6f))
                }
                Column {
                    HorizontalDivider(color = muted.copy(alpha = 0.25f))
                    Text(
                        WidgetDhikr[now.dayOfYear % WidgetDhikr.size],
                        color = ink.copy(alpha = 0.85f),
                        fontFamily = dhikrFontFor(arabicFontStyle),
                        fontSize = size(2.8f),
                        modifier = Modifier.padding(top = unit * 1f),
                    )
                }
            }
        }

        val prayerSide: @Composable (Modifier) -> Unit = { sideModifier ->
            Column(sideModifier, verticalArrangement = Arrangement.Center) {
                if (schedule == null || day == null) {
                    Text(stringResource(R.string.wall_no_place), color = muted, fontSize = size(2.6f), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    return@Column
                }
                val marked = when (phase) {
                    is PrayerPhase.ToAdhan -> phase.next.prayer
                    is PrayerPhase.ToIqama -> phase.prayer.prayer
                    is PrayerPhase.SinceIqama -> phase.prayer.prayer
                    null -> null
                }
                val iqamaShown = ObligatoryPrayers.any { schedule.iqamaMinutes(it, tableDate) > 0 }
                PrayerRow(
                    name = "", adhan = stringResource(R.string.wall_adhan), iqama = stringResource(R.string.wall_iqama).takeIf { iqamaShown },
                    unit = unit, fontSize = size(1.9f), color = muted, highlight = null,
                )
                Prayer.entries.forEach { prayer ->
                    val time = day[prayer]
                    val iqama = if (prayer in ObligatoryPrayers) text.clock(time.plusMinutes(schedule.iqamaMinutes(prayer, tableDate).toLong())) else "—"
                    PrayerRow(
                        name = text.name(prayer, time),
                        adhan = text.clock(time),
                        iqama = iqama.takeIf { iqamaShown },
                        unit = unit,
                        fontSize = size(2.9f),
                        color = if (prayer == Prayer.SUNRISE) muted else ink,
                        highlight = gold.takeIf { prayer == marked },
                        onHighlight = MaterialTheme.colorScheme.background,
                    )
                }
                phase?.let { Countdown(it, now, text, unit, ::size, gold, ink) }
            }
        }

        if (landscape) {
            Row(Modifier.fillMaxSize().padding(horizontal = pad * 1.4f, vertical = pad)) {
                clockSide(Modifier.weight(1.1f).fillMaxSize())
                Spacer(Modifier.width(pad * 1.4f))
                prayerSide(Modifier.weight(1f).fillMaxSize())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = pad * 1.4f)) {
                clockSide(Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(pad))
                prayerSide(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PrayerRow(
    name: String,
    adhan: String,
    iqama: String?,
    unit: Dp,
    fontSize: TextUnit,
    color: Color,
    highlight: Color?,
    onHighlight: Color = color,
) {
    val ink = if (highlight != null) onHighlight else color
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (highlight != null) Modifier.background(highlight, RoundedCornerShape(unit * 1.4f)) else Modifier)
            .padding(horizontal = unit * 1.6f, vertical = unit * 0.9f),
    ) {
        Text(name, color = ink, fontSize = fontSize, fontWeight = if (highlight != null) FontWeight.Medium else null, modifier = Modifier.weight(1f))
        Text(adhan, color = ink, fontSize = fontSize, textAlign = TextAlign.Center, modifier = Modifier.width(unit * 9))
        if (iqama != null) Text(iqama, color = ink, fontSize = fontSize, textAlign = TextAlign.Center, modifier = Modifier.width(unit * 9))
    }
}

/**
 * How long to the next adhan, then to its iqama, then since it, in hours, minutes and seconds.
 * Between the adhan and the iqama it fills with gold, the moment a wall clock is looked at most.
 */
@Composable
private fun Countdown(
    phase: PrayerPhase,
    now: ZonedDateTime,
    text: PrayerText,
    unit: Dp,
    size: (Float) -> TextUnit,
    gold: Color,
    ink: Color,
) {
    val instant = now.toInstant()
    val (label, span) = when (phase) {
        is PrayerPhase.ToAdhan -> stringResource(R.string.wall_next_in, text.name(phase.next.prayer, phase.next.time)) to
            Duration.between(instant, phase.next.time.toInstant())
        is PrayerPhase.ToIqama -> stringResource(R.string.wall_iqama_in, text.name(phase.prayer.prayer, phase.prayer.time)) to
            Duration.between(instant, phase.iqama.toInstant())
        is PrayerPhase.SinceIqama -> stringResource(R.string.wall_iqama_since, text.name(phase.prayer.prayer, phase.prayer.time)) to
            Duration.between(phase.iqama.toInstant(), instant)
    }
    val filled = phase is PrayerPhase.ToIqama
    val onFill = MaterialTheme.colorScheme.background
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = unit * 2f)
            .fillMaxWidth()
            .then(
                if (filled) {
                    Modifier.background(gold, RoundedCornerShape(unit * 1.6f))
                } else {
                    Modifier.border(1.dp, gold, RoundedCornerShape(unit * 1.6f))
                },
            )
            .padding(horizontal = unit * 2f, vertical = unit * 1.2f)
            .testTag("wall_countdown"),
    ) {
        Text(label, color = if (filled) onFill else ink, fontSize = size(2.4f), modifier = Modifier.weight(1f))
        Text(clockSpan(span), color = if (filled) onFill else gold, fontSize = size(4.4f), fontWeight = FontWeight.Medium)
    }
}

/** «١:٥٤:٢٨», or «٥٤:٢٨» within the hour, in the digits the user chose. */
private fun clockSpan(duration: Duration): String {
    val seconds = duration.seconds.coerceAtLeast(0)
    val hours = (seconds / 3600).toInt()
    val minutes = ((seconds % 3600) / 60).toInt()
    val rest = (seconds % 60).toInt()
    return if (hours > 0) "${arabicNumber(hours)}:${twoDigits(minutes)}:${twoDigits(rest)}" else "${arabicNumber(minutes)}:${twoDigits(rest)}"
}

/** The choices, over the foot of the clock after a tap: they are few, and on the clock itself. */
@Composable
private fun WallClockControls(options: WallClockOptions, onChange: (WallClockOptions) -> Unit, onExit: () -> Unit) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .heightIn(max = 520.dp)
            .testTag("wall_clock_controls")
            // Taps on the panel stay on it, and do not hide it.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.NightsStay, null, tint = LocalSakinahBrandColors.current.antiqueGold)
                Text(
                    stringResource(R.string.wall_clock_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                IconButton(onClick = onExit, modifier = Modifier.testTag("wall_clock_exit")) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.wall_exit))
                }
            }
            SettingToggleRow(stringResource(R.string.wall_keep_on), stringResource(R.string.wall_keep_on_hint), options.keepScreenOn, { onChange(options.copy(keepScreenOn = it)) }, "wall_keep_on")
            SettingToggleRow(stringResource(R.string.wall_dim), stringResource(R.string.wall_dim_hint), options.dimAtNight, { onChange(options.copy(dimAtNight = it)) }, "wall_dim")
            SettingToggleRow(stringResource(R.string.wall_seconds), stringResource(R.string.wall_seconds_hint), options.showSeconds, { onChange(options.copy(showSeconds = it)) }, "wall_seconds_switch")
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(stringResource(R.string.wall_screen_saver_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(
                onClick = { openScreenSaverSettings(context) },
                modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth().heightIn(min = 48.dp).testTag("wall_screen_saver"),
            ) { Text(stringResource(R.string.wall_screen_saver)) }
        }
    }
}

/** The system's own screen saver page; some makers hide it, and then the app's screen is the way. */
private fun openScreenSaverSettings(context: Context) {
    try {
        context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.wall_screen_saver_missing, Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        Toast.makeText(context, R.string.wall_screen_saver_missing, Toast.LENGTH_LONG).show()
    }
}

/** Now, a fresh value on each second's turn, so the seconds and the count move together. */
@Composable
private fun rememberSecondTick(): ZonedDateTime {
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = ZonedDateTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    return now
}

/**
 * From a while after the Isha to a while before the Fajr, when a lit wall should be quiet. Without
 * a place, a plain night of eleven to four.
 */
internal fun isNight(schedule: PrayerSchedule?, now: ZonedDateTime): Boolean {
    if (schedule == null) return now.hour >= 23 || now.hour < 4
    val instant: Instant = now.toInstant()
    val day = schedule.day(schedule.today(instant))
    val start = day[Prayer.ISHA].plusMinutes(NIGHT_AFTER_ISHA_MINUTES).toInstant()
    val end = day[Prayer.FAJR].minusMinutes(NIGHT_BEFORE_FAJR_MINUTES).toInstant()
    return !instant.isBefore(start) || instant.isBefore(end)
}

private fun Window.setBrightness(value: Float) {
    attributes = attributes.apply { screenBrightness = value }
}

private const val NIGHT_BRIGHTNESS = 0.04f
private const val NIGHT_ALPHA = 0.6f
private const val NIGHT_AFTER_ISHA_MINUTES = 45L
private const val NIGHT_BEFORE_FAJR_MINUTES = 20L
private const val CONTROLS_SHOWN_MS = 8_000L
private const val SHIFT_SECONDS = 180L
private val SHIFTS = listOf(0 to 0, 4 to 2, 0 to 4, -4 to 2)
