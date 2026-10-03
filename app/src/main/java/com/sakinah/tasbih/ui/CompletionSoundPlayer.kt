package com.sakinah.tasbih.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sakinah.tasbih.R
import com.sakinah.tasbih.data.CompletionSound
import com.sakinah.tasbih.data.normalizedCompletionSoundVolume

internal class CompletionSoundPlayer(context: Context) {
    private val pool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val ready = mutableSetOf<Int>()
    private val samples: Map<CompletionSound, Int>
    private var pending: Pair<Int, Float>? = null
    private var streamId = 0
    private var released = false

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            synchronized(this) {
                if (!released && status == 0) {
                    ready.add(sampleId)
                    pending?.takeIf { it.first == sampleId }?.let { request ->
                        pending = null
                        playLoaded(request.first, request.second)
                    }
                }
            }
        }
        samples = CompletionSound.entries.associateWith { sound ->
            pool.load(context.applicationContext, sound.rawResource(), 1)
        }
    }

    @Synchronized
    fun play(sound: CompletionSound, volume: Float) {
        if (released) return
        stop()
        val gain = normalizedCompletionSoundVolume(volume)
        if (gain <= 0f) return
        val sample = samples.getValue(sound)
        if (sample in ready) playLoaded(sample, gain) else pending = sample to gain
    }

    private fun playLoaded(sample: Int, gain: Float) {
        streamId = pool.play(sample, gain, gain, 1, 0, 1f)
    }

    @Synchronized
    fun stop() {
        pending = null
        if (!released && streamId != 0) pool.stop(streamId)
        streamId = 0
    }

    @Synchronized
    fun release() {
        if (released) return
        stop()
        released = true
        pool.release()
    }
}

internal fun CompletionSound.rawResource(): Int = when (this) {
    CompletionSound.ClearBell -> R.raw.completion_clear_bell
    CompletionSound.DoubleChime -> R.raw.completion_double_chime
    CompletionSound.TriplePulse -> R.raw.completion_triple_pulse
    CompletionSound.SoftChime -> R.raw.completion_soft_chime
}

@Composable
internal fun rememberCompletionSoundPlayer(): CompletionSoundPlayer {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val player = remember(context, lifecycle) { CompletionSoundPlayer(context) }
    DisposableEffect(player, lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) player.stop()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            player.release()
        }
    }
    return player
}
