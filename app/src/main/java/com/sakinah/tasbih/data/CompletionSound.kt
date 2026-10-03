package com.sakinah.tasbih.data

enum class CompletionSound(val storageId: String) {
    ClearBell("clear_bell"),
    DoubleChime("double_chime"),
    TriplePulse("triple_pulse"),
    SoftChime("soft_chime");

    companion object {
        fun fromStorage(value: String?): CompletionSound =
            entries.firstOrNull { it.storageId == value } ?: ClearBell
    }
}

const val DefaultCompletionSoundVolume = 0.9f

internal fun normalizedCompletionSoundVolume(value: Float): Float =
    if (value.isFinite()) value.coerceIn(0f, 1f) else DefaultCompletionSoundVolume
