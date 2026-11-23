package com.sakinah.tasbih.data

/**
 * Which digits numbers are written in: the Hindi digits «١٢٣» by default, or the Arabic digits
 * «123», as the user may choose since November 23, 2026, for the app and its widgets together.
 * Kept where it can be read at once, before anything is drawn: the app's start reads it, and so
 * does a widget's or an alarm's, since each starts the app's process before running.
 */
object DigitStyle {
    private const val STORE = "display"
    private const val KEY_WESTERN = "western_digits"

    @Volatile
    var western: Boolean = false
        private set

    fun load(context: android.content.Context) {
        western = context.getSharedPreferences(STORE, android.content.Context.MODE_PRIVATE).getBoolean(KEY_WESTERN, false)
    }

    fun save(context: android.content.Context, western: Boolean) {
        context.getSharedPreferences(STORE, android.content.Context.MODE_PRIVATE).edit().putBoolean(KEY_WESTERN, western).commit()
        this.western = western
    }

    /** For tests: the digits to write in, without touching storage. */
    internal fun use(western: Boolean) {
        this.western = western
    }
}

/** A number in the digits the user chose, whatever the phone's own language. */
fun arabicNumber(value: Int): String =
    if (DigitStyle.western) value.toString() else value.toString().map { digit ->
        if (digit in '0'..'9') '٠' + (digit - '0') else digit
    }.joinToString("")

/** Two digits at least, as minutes on a clock: «٠٥» or «05». */
fun twoDigits(value: Int): String = arabicNumber(value).padStart(2, if (DigitStyle.western) '0' else '٠')

fun dhikrQuantity(count: Int): String = when {
    count == 0 -> "لا أذكار"
    count == 1 -> "ذكر واحد"
    count == 2 -> "ذكران"
    count % 100 in 3..10 -> "${arabicNumber(count)} أذكار"
    else -> "${arabicNumber(count)} ذكرًا"
}

fun dayQuantity(count: Int): String = when {
    count == 0 -> "لا أيام بعد"
    count == 1 -> "يوم واحد"
    count == 2 -> "يومان"
    count % 100 in 3..10 -> "${arabicNumber(count)} أيام"
    else -> "${arabicNumber(count)} يومًا"
}
