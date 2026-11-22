package com.sakinah.tasbih.prayer

/**
 * Which of two positions to show while the phone is followed. A phone reports several at once:
 * the satellites' and the blend's, a few metres wide, and the network's, which from the mobile
 * mast alone can be a kilometre or more out. A rough one must not push aside a fine one that is
 * still current, or the user's dot jumps to the mast each time the satellites pause indoors.
 */
object BetterFix {
    /** A position this much newer wins whatever its accuracy, as the older one no longer says where the phone is. */
    const val SUPERSEDED_MS = 2 * 60_000L

    /** A newer position replaces the one held unless it is this much rougher. */
    private const val LOOSER = 1.5f

    /**
     * Whether the new position should replace the one held. Accuracies are in metres, null where
     * the source gave none, which counts as roughest. Times are on one clock, in milliseconds.
     */
    fun replaces(newAccuracy: Float?, newTimeMs: Long, heldAccuracy: Float?, heldTimeMs: Long?): Boolean {
        if (heldTimeMs == null) return true
        val age = newTimeMs - heldTimeMs
        if (age > SUPERSEDED_MS) return true
        if (age < -SUPERSEDED_MS) return false
        val new = newAccuracy ?: Float.MAX_VALUE
        val held = heldAccuracy ?: Float.MAX_VALUE
        if (new < held) return true
        return age > 0 && new <= held * LOOSER
    }
}
