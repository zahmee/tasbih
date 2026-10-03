package com.sakinah.tasbih

import android.media.SoundPool
import androidx.test.platform.app.InstrumentationRegistry
import com.sakinah.tasbih.data.CompletionSound
import com.sakinah.tasbih.ui.rawResource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CompletionSoundResourcesTest {
    @Test
    fun everyBundledCueCanBeDecodedOnAndroid() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loaded = CountDownLatch(CompletionSound.entries.size)
        val failures = AtomicInteger()
        val pool = SoundPool.Builder().setMaxStreams(1).build()
        try {
            pool.setOnLoadCompleteListener { _, _, status ->
                if (status != 0) failures.incrementAndGet()
                loaded.countDown()
            }
            CompletionSound.entries.forEach { assertTrue(pool.load(context, it.rawResource(), 1) > 0) }
            assertTrue("All four sounds must load", loaded.await(10, TimeUnit.SECONDS))
            assertEquals(0, failures.get())
        } finally {
            pool.release()
        }
    }
}
