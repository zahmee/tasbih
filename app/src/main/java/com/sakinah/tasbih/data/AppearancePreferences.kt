package com.sakinah.tasbih.data

enum class ThemeMode {
    System,
    Light,
    Dark,
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: System
    }
}

enum class ArabicFontStyle {
    Sakinah,
    Amiri,
    Ruqaa,
    Baqiyat,
    ;

    companion object {
        fun fromStorage(value: String?): ArabicFontStyle = entries.firstOrNull {
            it.name.equals(value, ignoreCase = true)
        } ?: Sakinah
    }
}
