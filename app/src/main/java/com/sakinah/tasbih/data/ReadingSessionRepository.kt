package com.sakinah.tasbih.data

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class ReaderUndo(
    val before: ReadingProgress,
    val eventId: Long,
    val completionEventId: Long = 0,
    val dayKey: String,
) {
    internal fun encode(collectionId: String): String {
        val previous = Base64.getUrlEncoder().withoutPadding().encodeToString(
            before.encodeForPreferences(collectionId).toByteArray(StandardCharsets.UTF_8),
        )
        return "$collectionId|$previous|$eventId|$completionEventId|$dayKey"
    }
}

internal fun decodeReaderUndo(values: Set<String>): Map<String, ReaderUndo> = values.mapNotNull { value ->
    runCatching {
        val parts = value.split('|')
        require(parts.size == 5)
        val previous = String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8)
        parts[0] to ReaderUndo(
            before = decodeReadingProgress(setOf(previous)).getValue(parts[0]),
            eventId = parts[2].toLong().also { require(it > 0) },
            completionEventId = parts[3].toLong().also { require(it >= 0) },
            dayKey = validDailyStateToken(parts[4]),
        )
    }.getOrNull()
}.toMap()

/** Serializes reader actions and uses saved state instead of potentially stale UI snapshots. */
class ReadingSessionRepository(
    private val preferences: AppPreferencesRepository,
    private val activity: ActivityRepository,
) {
    private val actions = Mutex()

    suspend fun increment(collection: DhikrCollection) = actions.withLock {
        if (collection.entries.isEmpty()) return@withLock
        // Retry once if the reset boundary or its settings change during the event write.
        repeat(2) {
            val saved = preferences.preferences.first()
            val actionDay = saved.dailyStateKey
            val before = saved.readingProgress[collection.id] ?: ReadingProgress()
            val index = before.entryIndex.coerceIn(0, collection.entries.lastIndex)
            if (before.completed || before.isEntryCompleted(index, collection)) return@withLock
            val entry = collection.entries[index]
            val count = before.repetitionCountFor(index) + 1
            val after = before.copy(
                entryIndex = index,
                repetitionCounts = before.repetitionCounts + (index to count),
                completedEntryIndices = if (count >= entry.repetitions) before.completedEntryIndices + index else before.completedEntryIndices,
            )
            val accepted = withContext(NonCancellable) {
                val undo = ReaderUndo(before.copy(entryIndex = index), activity.recordReader(entry, collection.title), dayKey = actionDay)
                try {
                    preferences.saveReaderAction(collection.id, after, undo, actionDay).also { savedAction ->
                        if (!savedAction) activity.removeReaderAction(undo)
                    }
                } catch (error: Exception) {
                    activity.removeReaderAction(undo)
                    throw error
                }
            }
            if (accepted) return@withLock
        }
    }

    suspend fun advance(collection: DhikrCollection) = actions.withLock {
        if (collection.entries.isEmpty()) return@withLock
        val saved = preferences.preferences.first()
        val actionDay = saved.dailyStateKey
        val progress = saved.readingProgress[collection.id] ?: ReadingProgress()
        if (progress.completed || !progress.isEntryCompleted(progress.entryIndex, collection)) return@withLock
        val completedIndices = collection.entries.indices.filter { progress.isEntryCompleted(it, collection) }.toSet()
        val completed = completedIndices.size == collection.entries.size
        val next = if (completed) progress.entryIndex else (1..collection.entries.size)
            .map { (progress.entryIndex + it) % collection.entries.size }
            .first { it !in completedIndices }
        val after = progress.copy(entryIndex = next, completedEntryIndices = completedIndices, completed = completed)
        withContext(NonCancellable) {
            val completionId = if (completed) activity.recordCompletion(collection) else 0
            val undo = saved.readerUndo[collection.id]?.copy(completionEventId = completionId)
            try {
                if (!preferences.saveReaderAction(collection.id, after, undo, actionDay) && completionId > 0) {
                    activity.removeReaderAction(ReaderUndo(progress, completionId, dayKey = actionDay))
                }
            } catch (error: Exception) {
                if (completionId > 0) activity.removeReaderAction(ReaderUndo(progress, completionId, dayKey = actionDay))
                throw error
            }
        }
    }

    suspend fun navigate(collection: DhikrCollection, direction: Int) = actions.withLock {
        if (collection.entries.isEmpty()) return@withLock
        repeat(2) {
            val saved = preferences.preferences.first()
            val actionDay = saved.dailyStateKey
            val progress = saved.readingProgress[collection.id] ?: ReadingProgress()
            if (preferences.saveReaderAction(
                collection.id,
                progress.copy(entryIndex = (progress.entryIndex + direction).coerceIn(0, collection.entries.lastIndex)),
                saved.readerUndo[collection.id], actionDay,
            )) return@withLock
        }
    }

    suspend fun openEntry(collection: DhikrCollection, entryId: String) = actions.withLock {
        val index = collection.entries.indexOfFirst { it.id == entryId }
        if (index < 0) return@withLock
        repeat(2) {
            val saved = preferences.preferences.first()
            val actionDay = saved.dailyStateKey
            val progress = saved.readingProgress[collection.id] ?: ReadingProgress()
            if (preferences.saveReaderAction(collection.id, progress.copy(entryIndex = index), saved.readerUndo[collection.id], actionDay)) {
                preferences.recordVisitedEntry(entryId)
                return@withLock
            }
        }
    }

    suspend fun undo(collectionId: String) = actions.withLock {
        val saved = preferences.preferences.first()
        val undo = saved.readerUndo[collectionId] ?: return@withLock
        if (undo.dayKey != saved.dailyStateKey) return@withLock
        withContext(NonCancellable) {
            val removed = activity.removeReaderAction(undo)
            try {
                if (!preferences.saveReaderAction(collectionId, undo.before, null, undo.dayKey)) {
                    activity.restoreEvents(removed)
                }
            } catch (error: Exception) {
                activity.restoreEvents(removed)
                throw error
            }
        }
    }

    suspend fun restart(collectionId: String) = actions.withLock {
        preferences.saveReaderAction(collectionId, ReadingProgress(), null)
    }
}
