package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LrclibCandidate
import com.example.data.LrclibException
import com.example.data.LrclibService
import com.example.data.LyricsParser
import com.example.data.MusicRepository
import com.example.model.Track
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyricsComprehensiveTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // 1. HTTP 403 error handling
    @Test
    fun testHttp403ErrorHandling() = runBlocking {
        val originalClient = LrclibService.okHttpClient
        try {
            LrclibService.okHttpClient = OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(403)
                        .message("Forbidden")
                        .body("{\"error\":\"Forbidden\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                })
                .build()

            val result = LrclibService.searchLyrics("Test Song", "Test Artist")
            assertTrue("Expected failure for 403", result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue("Expected AccessForbiddenException", exception is LrclibException.AccessForbiddenException)
            assertEquals(403, (exception as LrclibException.AccessForbiddenException).statusCode)
            assertTrue(exception.message!!.contains("403"))
        } finally {
            LrclibService.okHttpClient = originalClient
        }
    }

    // 2. Successful online lyrics retrieval (JSON parsing & model mapping)
    @Test
    fun testSuccessfulOnlineLyricsRetrievalParsing() {
        val mockJson = """
            [
                {
                    "id": 12345,
                    "trackName": "High and Dry",
                    "artistName": "Radiohead",
                    "albumName": "The Bends",
                    "duration": 257,
                    "instrumental": false,
                    "plainLyrics": "Two flights up, a room with a view\nTwo flights down, my life with you",
                    "syncedLyrics": "[00:15.50] Two flights up, a room with a view\n[00:20.00] Two flights down, my life with you"
                }
            ]
        """.trimIndent()

        val candidates = LrclibService.parseJsonArrayCandidates(mockJson)
        assertEquals(1, candidates.size)
        val candidate = candidates[0]
        assertEquals(12345L, candidate.id)
        assertEquals("High and Dry", candidate.trackName)
        assertEquals("Radiohead", candidate.artistName)
        assertEquals("The Bends", candidate.albumName)
        assertEquals(257, candidate.durationSeconds)
        assertTrue(candidate.hasSyncedLyrics)
        assertTrue(candidate.hasPlainLyrics)
        assertEquals("4:17", candidate.formattedDuration)
    }

    // 3. Network errors and HTTP 429 behavior
    @Test
    fun testHttp429RateLimitAndNetworkErrorHandling() = runBlocking {
        val originalClient = LrclibService.okHttpClient
        try {
            // Test 429
            LrclibService.okHttpClient = OkHttpClient.Builder()
                .addInterceptor(Interceptor { chain ->
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(429)
                        .message("Too Many Requests")
                        .body("{\"error\":\"Rate limit exceeded\"}".toResponseBody("application/json".toMediaTypeOrNull()))
                        .build()
                })
                .build()

            val res429 = LrclibService.searchLyrics("Fast Song", "Artist")
            assertTrue("Expected failure for 429", res429.isFailure)
            assertTrue(res429.exceptionOrNull() is LrclibException.RateLimitedException)
            assertEquals(429, (res429.exceptionOrNull() as LrclibException.RateLimitedException).statusCode)

            // Test Network IOException
            LrclibService.okHttpClient = OkHttpClient.Builder()
                .addInterceptor(Interceptor {
                    throw IOException("Connection timed out")
                })
                .build()

            val resNet = LrclibService.searchLyrics("Offline Song", "Artist")
            assertTrue("Expected failure for network timeout", resNet.isFailure)
            assertTrue(resNet.exceptionOrNull() is LrclibException.NetworkException)
        } finally {
            LrclibService.okHttpClient = originalClient
        }
    }

    // 4. Parsing synchronized LRC content
    @Test
    fun testParsingSynchronizedLrcContent() {
        val lrc = """
            [ti:Test]
            [ar:Artist]
            [00:05.20] First line of song
            [00:12.80] Second line of song | ترجمه خط دوم
            [00:20.00] Third line of song
        """.trimIndent()

        val parsed = LyricsParser.parseLrc(lrc)
        assertEquals(3, parsed.size)
        assertEquals(5200L, parsed[0].timeMs)
        assertEquals("First line of song", parsed[0].text)

        assertEquals(12800L, parsed[1].timeMs)
        assertEquals("Second line of song", parsed[1].text)
        assertEquals("ترجمه خط دوم", parsed[1].persianText)

        // Test active line detection
        val activeIndexAt15s = LyricsParser.findActiveLyricIndex(parsed, 15000L)
        assertEquals(1, activeIndexAt15s)

        val activeIndexAt25s = LyricsParser.findActiveLyricIndex(parsed, 25000L)
        assertEquals(2, activeIndexAt25s)
    }

    // 5. Saving and reopening plain lyrics
    @Test
    fun testSavingAndReopeningPlainLyrics() {
        val repo = MusicRepository(context)
        val trackId = "test_plain_${System.currentTimeMillis()}"
        val initialTrack = Track(
            id = trackId,
            title = "Acoustic Ballad",
            artist = "Solo Performer"
        )
        // Add to repo
        repo.importLyricsFileToTrack(trackId, "Initial text", false)

        val plainLyricsText = """
            Just a guitar and a voice
            Singing through the quiet room
            No clocks, no timestamps, no noise
        """.trimIndent()

        repo.savePlainLyrics(trackId, plainLyricsText)

        val updated = repo.tracks.value.find { it.id == trackId }
        assertNotNull(updated)
        assertEquals(plainLyricsText, updated!!.plainLyrics)
        assertEquals("PLAIN", updated.lyricsMode)
    }

    // 6. Importing .txt and .lrc files
    @Test
    fun testImportingTxtAndLrcFiles() {
        val repo = MusicRepository(context)
        val trackId = "test_import_${System.currentTimeMillis()}"

        // Import .lrc
        val lrcContent = "[00:01.00] Hello world\n[00:05.00] Goodbye world"
        assertTrue(LyricsParser.hasLrcTimestamps(lrcContent))
        repo.importLyricsFileToTrack(trackId, lrcContent, isLrcFormat = true)

        var track = repo.tracks.value.find { it.id == trackId }
        assertNotNull(track)
        assertEquals(lrcContent, track!!.lyrics)
        assertEquals("SYNCED", track.lyricsMode)

        // Import .txt
        val txtContent = "Line one without timestamps\nLine two without timestamps"
        assertFalse(LyricsParser.hasLrcTimestamps(txtContent))
        repo.importLyricsFileToTrack(trackId, txtContent, isLrcFormat = false)

        track = repo.tracks.value.find { it.id == trackId }
        assertNotNull(track)
        assertEquals(txtContent, track!!.plainLyrics)
        assertEquals("PLAIN", track.lyricsMode)
        // Ensure synced lyrics were not destroyed when txt was imported
        assertEquals(lrcContent, track.lyrics)
    }

    // 7. Persian and Unicode lyrics with preserved line breaks
    @Test
    fun testPersianAndUnicodeLyricsWithPreservedLineBreaks() {
        val repo = MusicRepository(context)
        val trackId = "persian_test_${System.currentTimeMillis()}"

        val persianPlainLyrics = """
            در سکوت شب، نغمه‌ای جاریست
            صدای بال شاپرک‌ها در باد
            
            طنین هستی در ژرفای تاریکی
            
            نوری از دوردست می‌تابد:
            «امید، هرگز نمی‌میرد»
        """.trimIndent()

        repo.savePlainLyrics(trackId, persianPlainLyrics)

        val track = repo.tracks.value.find { it.id == trackId }
        assertNotNull(track)
        assertEquals(persianPlainLyrics, track!!.plainLyrics)
        // Verify line break preservation
        assertTrue(track.plainLyrics!!.contains("\n\nطنین هستی"))
        assertTrue(track.plainLyrics!!.contains("«امید، هرگز نمی‌میرد»"))
    }

    // 8. Switching between lyrics modes without losing saved content
    @Test
    fun testSwitchingBetweenLyricsModesWithoutLosingSavedContent() {
        val repo = MusicRepository(context)
        val trackId = "mode_switch_${System.currentTimeMillis()}"

        val synced = "[00:10.00] Synchronized line"
        val plain = "Plain lyrics line"

        repo.updateTrackLyrics(
            trackId = trackId,
            newLyrics = synced,
            newPlainLyrics = plain,
            newMode = "SYNCED"
        )

        var track = repo.tracks.value.find { it.id == trackId }
        assertNotNull(track)
        assertEquals("SYNCED", track!!.lyricsMode)
        assertEquals(synced, track.lyrics)
        assertEquals(plain, track.plainLyrics)

        // Switch to PLAIN mode
        repo.setTrackLyricsMode(trackId, "PLAIN")
        track = repo.tracks.value.find { it.id == trackId }
        assertEquals("PLAIN", track!!.lyricsMode)
        assertEquals(synced, track.lyrics)
        assertEquals(plain, track.plainLyrics)

        // Switch back to SYNCED mode
        repo.setTrackLyricsMode(trackId, "SYNCED")
        track = repo.tracks.value.find { it.id == trackId }
        assertEquals("SYNCED", track!!.lyricsMode)
        assertEquals(synced, track.lyrics)
        assertEquals(plain, track.plainLyrics)
    }

    // 9. Persistence after application restart
    @Test
    fun testPersistenceAfterApplicationRestart() {
        val repo1 = MusicRepository(context)
        val trackId = "persistence_test_track"

        val syncedContent = "[00:03.00] Saved synced"
        val plainContent = "Saved plain lyrics content with multiple\nlines and Persian: سلام دنیا"

        repo1.updateTrackLyrics(
            trackId = trackId,
            newLyrics = syncedContent,
            newPlainLyrics = plainContent,
            newMode = "PLAIN"
        )

        // Simulate app restart by creating a new MusicRepository instance reading from disk
        val repo2 = MusicRepository(context)
        val loadedTrack = repo2.tracks.value.find { it.id == trackId }
        assertNotNull("Track should persist after restart", loadedTrack)
        assertEquals(syncedContent, loadedTrack!!.lyrics)
        assertEquals(plainContent, loadedTrack.plainLyrics)
        assertEquals("PLAIN", loadedTrack.lyricsMode)
    }

    // 10. Existing synchronized lyrics remaining functional
    @Test
    fun testExistingSynchronizedLyricsRemainingFunctional() {
        val repo = MusicRepository(context)
        val anthem = repo.tracks.value.find { it.id == "demo_1" }
        assertNotNull("Demo 1 anthem should exist in library", anthem)
        assertTrue("Demo 1 should have valid synced lyrics", anthem!!.hasSyncedLyrics)

        val parsed = LyricsParser.parseLrc(anthem.lyrics)
        assertTrue("Parsed lines should not be empty", parsed.isNotEmpty())
        assertTrue("First line should have timestamp > 0", parsed[0].timeMs > 0)
        assertTrue("Lyrics should contain text", parsed[0].text.isNotBlank())
    }
}
