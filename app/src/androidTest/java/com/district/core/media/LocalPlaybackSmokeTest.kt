package com.district.core.media

import android.net.Uri
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.district.MainActivity
import com.district.domain.RemoteResource
import com.district.domain.Track
import java.io.File
import kotlin.math.sin
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies the offline path: a downloaded track lives on disk as a file:// URI with no auth
 * headers, and PlaybackService's DefaultDataSource must play it locally and advance.
 */
@RunWith(AndroidJUnit4::class)
class LocalPlaybackSmokeTest {
    private var scenario: ActivityScenario<MainActivity>? = null
    private var controller: Media3PlaybackController? = null
    private lateinit var file: File

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        file = File(context.cacheDir, "offline-tone.wav")
        file.writeBytes(sineWav())
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller?.release() }
        scenario?.close()
        file.delete()
    }

    @Test
    fun playsLocalFileWithoutNetwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val track = Track(
            id = "local-1",
            title = "Offline Tone",
            artist = "District",
            albumId = "album-1",
            indexNumber = 1,
            durationMs = 4000L,
            stream = RemoteResource(url = Uri.fromFile(file).toString(), authHeaders = null),
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            controller = Media3PlaybackController(context).also { it.playQueue(listOf(track), 0) }
        }

        val deadline = System.currentTimeMillis() + 30_000
        var lastState = PlayerState()
        var advanced = false
        while (System.currentTimeMillis() < deadline) {
            lastState = controller!!.state.value
            if (lastState.errorMessage != null) {
                throw AssertionError("Local playback surfaced error: ${lastState.errorMessage}")
            }
            if (lastState.isPlaying && lastState.positionMs > 0) {
                advanced = true
                break
            }
            Thread.sleep(150)
        }
        assertTrue("Local file playback did not start/advance within 30s; last state: $lastState", advanced)
        assertEquals("local-1", lastState.currentTrack?.id)
        assertTrue("Duration was not resolved from the local file", lastState.durationMs > 0)
    }

    private fun sineWav(seconds: Int = 4, sampleRate: Int = 8000): ByteArray {
        val samples = seconds * sampleRate
        val dataSize = samples * 2
        val out = java.io.ByteArrayOutputStream()
        fun int32(value: Int) {
            out.write(value); out.write(value shr 8); out.write(value shr 16); out.write(value shr 24)
        }
        fun int16(value: Int) {
            out.write(value); out.write(value shr 8)
        }
        out.write("RIFF".toByteArray()); int32(36 + dataSize); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); int32(16); int16(1); int16(1)
        int32(sampleRate); int32(sampleRate * 2); int16(2); int16(16)
        out.write("data".toByteArray()); int32(dataSize)
        repeat(samples) { i ->
            int16((sin(2.0 * Math.PI * 440.0 * i / sampleRate) * 12000).toInt())
        }
        return out.toByteArray()
    }
}
