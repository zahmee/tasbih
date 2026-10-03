package com.sakinah.tasbih.data

/** Deterministic Arabic digits even when the phone uses an English locale. */
fun arabicNumber(value: Int): String = value.toString().map { digit ->
    if (digit in '0'..'9') '٠' + (digit - '0') else digit
}.joinToString("")

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
