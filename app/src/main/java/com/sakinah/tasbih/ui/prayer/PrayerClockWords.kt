package com.sakinah.tasbih.ui.prayer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sakinah.tasbih.R
import com.sakinah.tasbih.prayer.PrayerMoment
import com.sakinah.tasbih.prayer.PrayerPhase
import java.time.Duration
import java.time.Instant

/**
 * What the prayer card says for where the clock stands: its heading, the prayer, the time, and
 * how long until it or since it. [marked] is the prayer the day's times mark with it.
 */
data class PrayerClockWords(val label: String, val name: String, val time: String, val span: String, val marked: PrayerMoment)

/** The same three steps as the widgets: to the adhan, to the iqama, and the quarter hour after it. */
@Composable
fun prayerClockWords(phase: PrayerPhase, now: Instant, text: PrayerText): PrayerClockWords = when (phase) {
    is PrayerPhase.ToAdhan -> PrayerClockWords(
        label = stringResource(if (phase.next.tomorrow) R.string.prayer_next_tomorrow else R.string.prayer_next),
        name = text.name(phase.next.prayer, phase.next.time),
        time = text.time(phase.next.time),
        span = text.remaining(Duration.between(now, phase.next.time.toInstant())),
        marked = phase.next,
    )
    is PrayerPhase.ToIqama -> PrayerClockWords(
        label = stringResource(R.string.prayer_iqama_label),
        name = text.name(phase.prayer.prayer, phase.prayer.time),
        time = text.time(phase.iqama),
        span = text.remaining(Duration.between(now, phase.iqama.toInstant())),
        marked = phase.prayer,
    )
    is PrayerPhase.SinceIqama -> PrayerClockWords(
        label = stringResource(R.string.prayer_iqama_done),
        name = text.name(phase.prayer.prayer, phase.prayer.time),
        time = text.time(phase.iqama),
        span = text.since(Duration.between(phase.iqama.toInstant(), now)),
        marked = phase.prayer,
    )
}
