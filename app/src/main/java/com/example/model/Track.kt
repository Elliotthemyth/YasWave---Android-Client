package com.example.model

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "Unknown Album",
    val durationMs: Long = 0L,
    val path: String = "",
    val uriString: String = "",
    val artworkResId: Int? = null,
    val artworkUri: String? = null,
    val lyrics: String? = null,
    val persianLyrics: String? = null,
    val plainLyrics: String? = null,
    val lyricsMode: String = "SYNCED", // "SYNCED" or "PLAIN"
    val year: String? = null,
    val genre: String? = null,
    val folderName: String = "Music",
    val isFavorite: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
) {
    val hasSyncedLyrics: Boolean
        get() = !lyrics.isNullOrBlank()

    val hasPlainLyrics: Boolean
        get() = !plainLyrics.isNullOrBlank()

    val isPlainMode: Boolean
        get() = lyricsMode == "PLAIN"
    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
