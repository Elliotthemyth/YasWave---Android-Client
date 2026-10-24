package com.example

import com.example.data.LyricsParser
import com.example.data.MetadataParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataParserRegressionTest {

    @Test
    fun `test numeric artist bug resolution via AlbumArtist`() {
        // Simulating the bug where rawArtist has numeric track position "2" or "3",
        // but AlbumArtist has the actual band name
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = "Midnight Groove",
            rawArtist = "2",
            rawAlbumArtist = "Kavosh Band",
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = "2",
            rawDiscNum = "1",
            rawGenre = "Electronic",
            fallbackName = "02. Kavosh Band - Midnight Groove.mp3"
        )

        assertEquals("Kavosh Band", result.artist)
        assertEquals("Midnight Groove", result.title)
        assertEquals("FALLBACK_TPE2_ALBUMARTIST", result.detectionSource)
        assertTrue(result.diagnosisNote.contains("numeric value '2'"))
    }

    @Test
    fun `test parenthesized numeric tag resolution`() {
        // Parenthesized ID3 genre/track tag like "(3)" or "(04)"
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = "Desert Wind",
            rawArtist = "(3)",
            rawAlbumArtist = "Sina",
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = "3",
            rawDiscNum = null,
            rawGenre = "Dance",
            fallbackName = "Desert Wind.mp3"
        )

        assertEquals("Sina", result.artist)
        assertEquals("FALLBACK_TPE2_ALBUMARTIST", result.detectionSource)
    }

    @Test
    fun `test track fraction numeric tag resolution`() {
        // Tag with disc or track fraction e.g. "4/10"
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = "Cyber City",
            rawArtist = "4/10",
            rawAlbumArtist = "Cyber Pulse",
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = "4",
            rawDiscNum = null,
            rawGenre = "Synth",
            fallbackName = "Cyber City.mp3"
        )

        assertEquals("Cyber Pulse", result.artist)
        assertEquals("FALLBACK_TPE2_ALBUMARTIST", result.detectionSource)
    }

    @Test
    fun `test legitimate non-numeric artist is preserved`() {
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = "Yas Wave Anthem",
            rawArtist = "YASWAVE Sounds",
            rawAlbumArtist = "YASWAVE Sounds",
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = "1",
            rawDiscNum = "1",
            rawGenre = "Synthwave",
            fallbackName = "yas_anthem.wav"
        )

        assertEquals("YASWAVE Sounds", result.artist)
        assertEquals("Yas Wave Anthem", result.title)
        assertEquals("TAG_TPE1_ARTIST", result.detectionSource)
    }

    @Test
    fun `test numeric artist fallback to filename heuristic`() {
        // Both artist and album artist are missing or numeric, but filename has artist pattern
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = null,
            rawArtist = "2",
            rawAlbumArtist = null,
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = "2",
            rawDiscNum = null,
            rawGenre = null,
            fallbackName = "02. Mohsen Chavoshi - Shahrzad.mp3"
        )

        assertEquals("Mohsen Chavoshi", result.artist)
        assertEquals("Shahrzad", result.title)
        assertEquals("FILENAME_HEURISTIC", result.detectionSource)
    }

    @Test
    fun `test High and Dry - Radiohead pattern with numeric artist tag`() {
        // High and Dry - Radiohead.mp3 (Title - Artist order) where rawArtist has numeric "3"
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = null,
            rawArtist = "3",
            rawAlbumArtist = null,
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = "3",
            rawDiscNum = null,
            rawGenre = null,
            fallbackName = "High and Dry - Radiohead.mp3"
        )

        assertEquals("Radiohead", result.artist)
        assertEquals("High and Dry", result.title)
        assertEquals("FILENAME_HEURISTIC", result.detectionSource)
    }

    @Test
    fun `test Title - Artist order resolved when embedded title is known`() {
        // Embedded title is "High and Dry", rawArtist is invalid numeric "4"
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = "High and Dry",
            rawArtist = "4",
            rawAlbumArtist = null,
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = null,
            rawDiscNum = null,
            rawGenre = null,
            fallbackName = "High and Dry - AnyCustomBand.mp3"
        )

        assertEquals("AnyCustomBand", result.artist)
        assertEquals("High and Dry", result.title)
        assertEquals("FILENAME_HEURISTIC", result.detectionSource)
    }

    @Test
    fun `test valid embedded artist is never overwritten by filename heuristic`() {
        // Valid tag has artist "Original Artist", but filename says "Other - OtherSong.mp3"
        val result = MetadataParser.resolveArtistAndTitle(
            rawTitle = "Song Title",
            rawArtist = "Original Artist",
            rawAlbumArtist = null,
            rawAuthor = null,
            rawComposer = null,
            rawTrackNum = "1",
            rawDiscNum = null,
            rawGenre = "Rock",
            fallbackName = "Radiohead - High and Dry.mp3"
        )

        assertEquals("Original Artist", result.artist)
        assertEquals("Song Title", result.title)
        assertEquals("TAG_TPE1_ARTIST", result.detectionSource)
    }

    @Test
    fun `test synchronized lyrics parser with Persian bilingual translation`() {
        val lrc = """
            [00:01.00] Riding the neon frequencies | سوار بر بسامدهای نئونی
            [00:05.50] YASWAVE pulses in the air | یس‌ویو در هوا می‌تپد
        """.trimIndent()

        val parsed = LyricsParser.parseLrc(lrc)
        assertEquals(2, parsed.size)
        assertEquals(1000L, parsed[0].timeMs)
        assertEquals("Riding the neon frequencies", parsed[0].text)
        assertEquals("سوار بر بسامدهای نئونی", parsed[0].persianText)

        val activeIndex = LyricsParser.findActiveLyricIndex(parsed, 3000L)
        assertEquals(0, activeIndex)

        val activeIndex2 = LyricsParser.findActiveLyricIndex(parsed, 6000L)
        assertEquals(1, activeIndex2)
    }
}
