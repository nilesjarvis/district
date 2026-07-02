package com.district.jellyfinmono.core.media

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.district.jellyfinmono.MainActivity
import com.district.jellyfinmono.domain.AuthHeaders
import com.district.jellyfinmono.domain.RemoteResource
import com.district.jellyfinmono.domain.Track
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlin.math.sin
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end smoke test for the playback stack: Media3PlaybackController connects to
 * PlaybackService, media items cross the session boundary, the service's data source
 * attaches auth headers, and audio actually reaches the playing state and advances.
 *
 * MainActivity is launched so the process is foreground and audio focus is granted;
 * without a foreground app Media3 (correctly) refuses to start playback.
 */
@RunWith(AndroidJUnit4::class)
class PlaybackServiceSmokeTest {
    private lateinit var server: MockWebServer
    private var scenario: ActivityScenario<MainActivity>? = null
    private var controller: Media3PlaybackController? = null

    @Before
    fun setUp() {
        server = MockWebServer()
        val wav = sineWav()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                MockResponse()
                    .setHeader("Content-Type", "audio/wav")
                    .setBody(Buffer().write(wav))
        }
        server.start()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After
    fun tearDown() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller?.release() }
        scenario?.close()
        server.shutdown()
    }

    @Test
    fun playsStreamedAudioThroughServiceWithAuthHeaders() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val track = Track(
            id = "smoke-1",
            title = "Smoke Test Tone",
            artist = "District",
            albumId = null,
            indexNumber = 1,
            durationMs = 4000L,
            stream = RemoteResource(
                url = server.url("/audio.wav").toString(),
                authHeaders = AuthHeaders("MediaBrowser Token=\"smoke-token\"", "smoke-token"),
            ),
        )

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            controller = Media3PlaybackController(context).also {
                it.playQueue(listOf(track), 0)
            }
        }

        // The stream request must reach the server with auth headers attached by the service.
        val request = server.takeRequest(10, TimeUnit.SECONDS)
            ?: throw AssertionError("No HTTP request reached the stream server")
        assertEquals("smoke-token", request.getHeader("X-Emby-Token"))
        assertEquals("MediaBrowser Token=\"smoke-token\"", request.getHeader("Authorization"))

        // Audio must actually start and the position must advance past zero.
        val deadline = System.currentTimeMillis() + 30_000
        var lastState = PlayerState()
        var advanced = false
        while (System.currentTimeMillis() < deadline) {
            lastState = controller!!.state.value
            if (lastState.errorMessage != null) {
                throw AssertionError("Playback surfaced error: ${lastState.errorMessage}")
            }
            if (lastState.isPlaying && lastState.positionMs > 0) {
                advanced = true
                break
            }
            Thread.sleep(150)
        }
        assertTrue("Playback did not start/advance within 30s; last state: $lastState", advanced)
        assertEquals("smoke-1", lastState.currentTrack?.id)
        assertTrue("Duration was not resolved from the stream", lastState.durationMs > 0)
    }

    private fun sineWav(seconds: Int = 4, sampleRate: Int = 8000): ByteArray {
        val samples = seconds * sampleRate
        val dataSize = samples * 2
        val out = ByteArrayOutputStream()
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
