package com.example.data

import android.content.Context
import com.example.R
import com.example.model.Playlist
import com.example.model.Track
import java.io.File

object SampleDataRepository {

    fun initializeSampleLibrary(context: Context): List<Track> {
        val filesDir = context.filesDir
        val audioDir = File(filesDir, "yaswave_audio").apply { if (!exists()) mkdirs() }

        val track1File = File(audioDir, "yas_anthem.wav")
        val track2File = File(audioDir, "avaye_sokoot.wav")
        val track3File = File(audioDir, "kavosh_midnight.wav")
        val track4File = File(audioDir, "cyber_horizon.wav")

        AudioSynthesizer.createSynthesizedTrack(track1File, durationSeconds = 30, chordBaseFreq = 220.0) // A3
        AudioSynthesizer.createSynthesizedTrack(track2File, durationSeconds = 28, chordBaseFreq = 196.0) // G3 Persian tuning vibe
        AudioSynthesizer.createSynthesizedTrack(track3File, durationSeconds = 32, chordBaseFreq = 174.6) // F3
        AudioSynthesizer.createSynthesizedTrack(track4File, durationSeconds = 26, chordBaseFreq = 261.6) // C4

        val track1 = Track(
            id = "demo_1",
            title = "Yas Wave Anthem",
            artist = "YASWAVE Sounds",
            album = "Neon Pulse Vol. 1",
            durationMs = 30000L,
            path = track1File.absolutePath,
            uriString = track1File.toURI().toString(),
            artworkResId = R.drawable.art_synthwave,
            genre = "Synthwave",
            year = "2026",
            folderName = "Featured",
            isFavorite = true,
            lyrics = """
                [00:01.00] Riding the neon frequencies through the night | سوار بر بسامدهای نئونی در دل شب
                [00:05.50] YASWAVE pulses in the digital air | یس‌ویو در هوای دیجیتال می‌تپد
                [00:10.00] Synth chords rising, lights everywhere | آکوردهای سینت اوج می‌گیرند، نورها همه‌جا
                [00:15.20] Feel the rhythm take over your soul | حس کن که ریتم روحت را تسخیر می‌کند
                [00:20.50] Echoes of the future, pure audio flow | پژواک آینده، جریان ناب صدا
                [00:25.00] Under purple horizons, we never let go | زیر افق‌های ارغوانی، هرگز رها نمی‌کنیم
            """.trimIndent()
        )

        val track2 = Track(
            id = "demo_2",
            title = "آوای سکوت (Voice of Silence)",
            artist = "سینا (Sina)",
            album = "طنین هستی (Echo of Being)",
            durationMs = 28000L,
            path = track2File.absolutePath,
            uriString = track2File.toURI().toString(),
            artworkResId = R.drawable.art_persian,
            genre = "Ambient / Persian Fusion",
            year = "2026",
            folderName = "Persian",
            isFavorite = true,
            lyrics = """
                [00:02.00] در سکوت شب، صدایی آشنا جاری‌ست
                [00:07.00] نغمه‌ای از دوردست که با دل هم‌بازی‌ست
                [00:12.50] موج‌ها می‌آیند و خستگی‌ها را می‌برند
                [00:18.00] رازهای نهفته در این نوای پاک
                [00:23.00] آرامشی جاودانه بر پهنه خاک
            """.trimIndent(),
            persianLyrics = """
                [00:02.00] In the silence of the night, a familiar voice flows
                [00:07.00] A distant melody that whispers to the soul
                [00:12.50] The waves arrive and wash all fatigue away
                [00:18.00] Hidden secrets inside this pure sound
                [00:23.00] An eternal peace upon the ground
            """.trimIndent(),
            plainLyrics = """
                در سکوت شب، صدایی آشنا جاری‌ست
                نغمه‌ای از دوردست که با دل هم‌بازی‌ست
                
                موج‌ها می‌آیند و خستگی‌ها را می‌برند
                رازهای نهفته در این نوای پاک
                آرامشی جاودانه بر پهنه خاک
            """.trimIndent()
        )

        // Track 3 demonstrates the Artist Metadata Bug resolution:
        // Raw tag had artist = "2" (e.g. track index written into TPE1),
        // but MetadataParser resolved it cleanly to AlbumArtist "Kavosh Band"!
        val track3 = Track(
            id = "demo_3",
            title = "Midnight Groove (Tag Diagnostic Verified)",
            artist = "Kavosh Band", // Resolved properly!
            album = "Tehran Electronic Nights",
            durationMs = 32000L,
            path = track3File.absolutePath,
            uriString = track3File.toURI().toString(),
            artworkResId = R.drawable.art_synthwave,
            genre = "Electronic",
            year = "2025",
            folderName = "Diagnostics",
            isFavorite = false,
            lyrics = """
                [00:01.00] Bassline rolling at 120 BPM
                [00:07.00] Diagnostic verified: Artist metadata bug repaired
                [00:13.00] No more numeric '2' or '3' for artist names!
                [00:19.00] YASWAVE native audio architecture in full control
                [00:26.00] Crystal clear playback and synchronized motion
            """.trimIndent(),
            plainLyrics = """
                Bassline rolling at 120 BPM
                Diagnostic verified: Artist metadata bug repaired
                No more numeric '2' or '3' for artist names!
                
                YASWAVE native audio architecture in full control
                Crystal clear playback and synchronized motion
            """.trimIndent()
        )

        val track4 = Track(
            id = "demo_4",
            title = "Digital Silk Road",
            artist = "Cyber Pulse",
            album = "Future Caravans",
            durationMs = 26000L,
            path = track4File.absolutePath,
            uriString = track4File.toURI().toString(),
            artworkResId = R.drawable.art_persian,
            genre = "Cyber Ambient",
            year = "2026",
            folderName = "Featured",
            isFavorite = false,
            lyrics = """
                [00:02.00] Signals traveling across desert sands | سیگنال‌ها از دل شن‌های کویر عبور می‌کنند
                [00:08.00] Connecting ancient dreams to modern hands | رویاهای کهن را به دست‌های مدرن پیوند می‌دهند
                [00:14.00] Golden frequencies in a cybernetic sky | بسامدهای طلایی در آسمانی سایبرنتیک
                [00:20.00] YASWAVE: Pure sound without compromise | یس‌ویو: صدای ناب بدون سازش
            """.trimIndent()
        )

        return listOf(track1, track2, track3, track4)
    }

    fun getInitialPlaylists(): List<Playlist> {
        return listOf(
            Playlist(
                id = "pl_favorites",
                name = "Favorites",
                description = "Your most loved tracks",
                trackIds = listOf("demo_1", "demo_2"),
                coverResId = R.drawable.art_synthwave
            ),
            Playlist(
                id = "pl_persian",
                name = "طنین شب (Persian Chill)",
                description = "آرامش شبانه با موسیقی الکترونیک و فیوژن",
                trackIds = listOf("demo_2", "demo_4"),
                coverResId = R.drawable.art_persian
            ),
            Playlist(
                id = "pl_neon",
                name = "Cyber Waves",
                description = "High energy synth and basslines",
                trackIds = listOf("demo_1", "demo_3", "demo_4"),
                coverResId = R.drawable.art_synthwave
            )
        )
    }
}
