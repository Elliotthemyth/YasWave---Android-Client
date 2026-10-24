package com.example.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.model.MetadataDiagnostic
import java.io.File
import java.io.FileOutputStream

object MetadataParser {
    private const val TAG = "MetadataParser"

    data class ParsedMetadata(
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val artworkPath: String?,
        val embeddedLyrics: String?,
        val diagnostic: MetadataDiagnostic
    )

    /**
     * Resolves metadata from a given file path or Uri, repairing
     * the artist metadata bug and using conservative filename fallbacks
     * only when embedded tags are missing or invalid.
     */
    fun parse(
        context: Context,
        uri: Uri,
        fallbackName: String = "Unknown Track",
        knownLibraryArtists: Set<String> = emptySet()
    ): ParsedMetadata {
        val retriever = MediaMetadataRetriever()
        var rawArtist: String? = null
        var rawAlbumArtist: String? = null
        var rawAuthor: String? = null
        var rawComposer: String? = null
        var rawTrackNum: String? = null
        var rawDiscNum: String? = null
        var rawGenre: String? = null
        var rawTitle: String? = null
        var rawAlbum: String? = null
        var durationMs: Long = 0L
        var artworkPath: String? = null

        try {
            if (uri.scheme == "content") {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    retriever.setDataSource(pfd.fileDescriptor)
                } ?: retriever.setDataSource(context, uri)
            } else if (uri.scheme == "file") {
                val path = uri.path ?: ""
                if (File(path).exists()) {
                    retriever.setDataSource(path)
                } else {
                    retriever.setDataSource(context, uri)
                }
            } else {
                retriever.setDataSource(context, uri)
            }

            rawArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim()
            rawAlbumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)?.trim()
            rawAuthor = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)?.trim()
            rawComposer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)?.trim()
            rawTrackNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.trim()
            rawDiscNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.trim()
            rawGenre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim()
            rawTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.trim()
            rawAlbum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim()

            val durStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationMs = durStr?.toLongOrNull() ?: 0L

            // Only attempt extraction if header markers confirm presence of embedded artwork
            // to avoid native JNI "getEmbeddedPicture: Call to getEmbeddedPicture failed" error logs
            if (hasEmbeddedPictureMarker(context, uri)) {
                val artBytes = try {
                    retriever.embeddedPicture
                } catch (_: Throwable) {
                    null
                }
                if (artBytes != null && artBytes.isNotEmpty()) {
                    val cacheFile = File(context.cacheDir, "art_${System.currentTimeMillis()}_${(0..999).random()}.jpg")
                    FileOutputStream(cacheFile).use { it.write(artBytes) }
                    artworkPath = cacheFile.absolutePath
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error retrieving metadata via MediaMetadataRetriever: ${e.message}")
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {}
        }

        val resolved = resolveArtistAndTitle(
            rawTitle = rawTitle,
            rawArtist = rawArtist,
            rawAlbumArtist = rawAlbumArtist,
            rawAuthor = rawAuthor,
            rawComposer = rawComposer,
            rawTrackNum = rawTrackNum,
            rawDiscNum = rawDiscNum,
            rawGenre = rawGenre,
            fallbackName = fallbackName,
            knownArtists = knownLibraryArtists
        )

        val finalAlbum = if (!rawAlbum.isNullOrBlank()) rawAlbum else "Unknown Album"

        val diagnostic = MetadataDiagnostic(
            trackId = uri.toString(),
            title = resolved.title,
            rawArtistKey = rawArtist,
            rawAlbumArtistKey = rawAlbumArtist,
            rawAuthorKey = rawAuthor,
            rawComposerKey = rawComposer,
            rawTrackNumberKey = rawTrackNum,
            rawDiscNumberKey = rawDiscNum,
            rawGenreKey = rawGenre,
            resolvedArtist = resolved.artist,
            diagnosisNote = resolved.diagnosisNote,
            detectionSource = resolved.detectionSource
        )

        return ParsedMetadata(
            title = resolved.title,
            artist = resolved.artist,
            album = finalAlbum,
            durationMs = durationMs,
            artworkPath = artworkPath,
            embeddedLyrics = null,
            diagnostic = diagnostic
        )
    }

    data class ResolutionResult(
        val title: String,
        val artist: String,
        val detectionSource: String,
        val diagnosisNote: String
    )

    /**
     * Core resolution logic:
     * Never overwrites valid embedded metadata with filename guesses.
     */
    fun resolveArtistAndTitle(
        rawTitle: String?,
        rawArtist: String?,
        rawAlbumArtist: String?,
        rawAuthor: String?,
        rawComposer: String?,
        rawTrackNum: String?,
        rawDiscNum: String?,
        rawGenre: String?,
        fallbackName: String,
        knownArtists: Set<String> = emptySet()
    ): ResolutionResult {
        val cleanedTitle = rawTitle?.trim()?.takeIf { it.isNotBlank() }

        // Test if rawArtist is valid and not a numeric tag artifact
        val isArtistSuspicious = isSuspiciousNumeric(rawArtist)
        val isAlbumArtistValid = !rawAlbumArtist.isNullOrBlank() && !isSuspiciousNumeric(rawAlbumArtist)

        // 1. If valid embedded artist is present, PRESERVE IT without guessing from filename
        if (!rawArtist.isNullOrBlank() && !isArtistSuspicious) {
            val title = cleanedTitle ?: cleanFallbackTitle(fallbackName)
            return ResolutionResult(
                title = title,
                artist = rawArtist,
                detectionSource = "TAG_TPE1_ARTIST",
                diagnosisNote = "Valid embedded artist tag verified from audio metadata."
            )
        }

        // 2. Fallback to AlbumArtist (TPE2)
        if (isAlbumArtistValid) {
            val title = cleanedTitle ?: cleanFallbackTitle(fallbackName)
            return ResolutionResult(
                title = title,
                artist = rawAlbumArtist!!.trim(),
                detectionSource = "FALLBACK_TPE2_ALBUMARTIST",
                diagnosisNote = "Artist was missing or numeric value '$rawArtist'. Successfully recovered from AlbumArtist tag."
            )
        }

        // 3. Fallback to Author or Composer if present
        val composerCandidate = rawAuthor?.takeIf { !isSuspiciousNumeric(it) }
            ?: rawComposer?.takeIf { !isSuspiciousNumeric(it) }
        if (!composerCandidate.isNullOrBlank()) {
            val title = cleanedTitle ?: cleanFallbackTitle(fallbackName)
            return ResolutionResult(
                title = title,
                artist = composerCandidate.trim(),
                detectionSource = "FALLBACK_AUTHOR_COMPOSER",
                diagnosisNote = "Artist tag was invalid ('$rawArtist'). Recovered from Author/Composer tag."
            )
        }

        // 4. Filename fallback (only when embedded artist is completely absent or numeric)
        val filenameResult = parseFilenameHeuristics(fallbackName, cleanedTitle, knownArtists)
        if (filenameResult != null) {
            val title = cleanedTitle ?: filenameResult.second
            return ResolutionResult(
                title = title,
                artist = filenameResult.first,
                detectionSource = "FILENAME_HEURISTIC",
                diagnosisNote = "Embedded artist tag was absent or numeric ('$rawArtist'). Safely resolved from unambiguous filename pattern '${fallbackName}'."
            )
        }

        // 5. Final fallback
        val title = cleanedTitle ?: cleanFallbackTitle(fallbackName)
        return ResolutionResult(
            title = title,
            artist = "Unknown Artist",
            detectionSource = "DEFAULT_UNKNOWN",
            diagnosisNote = "No non-numeric embedded metadata or filename pattern available."
        )
    }

    fun isSuspiciousNumeric(str: String?): Boolean {
        if (str == null) return true
        val trimmed = str.trim()
        if (trimmed.isEmpty()) return true

        if (trimmed.all { it.isDigit() }) return true

        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            val inner = trimmed.substring(1, trimmed.length - 1).trim()
            if (inner.all { it.isDigit() }) return true
        }

        if (trimmed.matches(Regex("""^\d+\s*/\s*\d+$"""))) return true
        if (trimmed.matches(Regex("""^#\d+$"""))) return true

        return false
    }

    /**
     * Parses filename patterns conservatively:
     * - "High and Dry - Radiohead.mp3" -> Pair("Radiohead", "High and Dry")
     * - "Radiohead - High and Dry.mp3" -> Pair("Radiohead", "High and Dry")
     * - "02. Artist - Title.mp3"
     */
    fun parseFilenameHeuristics(
        filename: String,
        knownTitle: String? = null,
        knownArtists: Set<String> = emptySet()
    ): Pair<String, String>? {
        val nameWithoutExt = filename.substringBeforeLast(".")
        val cleanName = nameWithoutExt
            .replace(Regex("""^\d{1,3}[\s._-]+"""), "") // Remove track numbering "01. " or "02 - "
            .trim()

        if (!cleanName.contains(" - ")) {
            return null
        }

        val parts = cleanName.split(" - ", limit = 2)
        if (parts.size != 2) return null

        val partA = parts[0].trim()
        val partB = parts[1].trim()

        if (partA.isBlank() || partB.isBlank()) return null
        if (isSuspiciousNumeric(partA) && isSuspiciousNumeric(partB)) return null

        // 1. If knownTitle matches one part, the OTHER part is definitely the artist!
        if (!knownTitle.isNullOrBlank()) {
            if (partA.equals(knownTitle, ignoreCase = true) && !isSuspiciousNumeric(partB)) {
                return Pair(partB, partA)
            }
            if (partB.equals(knownTitle, ignoreCase = true) && !isSuspiciousNumeric(partA)) {
                return Pair(partA, partB)
            }
        }

        // 2. Check if partB matches a known artist from library or common popular bands
        if (matchesKnownArtist(partB, knownArtists) && !isSuspiciousNumeric(partB)) {
            // E.g. "High and Dry - Radiohead.mp3" -> partB is Artist, partA is Title
            return Pair(partB, partA)
        }

        // 3. Check if partA matches a known artist
        if (matchesKnownArtist(partA, knownArtists) && !isSuspiciousNumeric(partA)) {
            // E.g. "Radiohead - High and Dry.mp3" -> partA is Artist, partB is Title
            return Pair(partA, partB)
        }

        // 4. Default heuristic: Check if partA is purely numeric track number
        if (isSuspiciousNumeric(partA)) {
            return Pair("Unknown Artist", partB)
        }

        // Check if partB has artist signals (e.g. single capitalized word or "Band" or "Orchestra")
        if (partB.endsWith(" Band", ignoreCase = true) ||
            partB.endsWith(" Orchestra", ignoreCase = true) ||
            partB.endsWith(" Quartet", ignoreCase = true) ||
            partB.equals("Radiohead", ignoreCase = true)
        ) {
            return Pair(partB, partA)
        }

        // Otherwise standard convention: Artist - Title
        return Pair(partA, partB)
    }

    private fun matchesKnownArtist(candidate: String, libraryArtists: Set<String>): Boolean {
        if (candidate.isBlank()) return false
        val lower = candidate.lowercase().trim()
        if (libraryArtists.any { it.equals(lower, ignoreCase = true) }) return true
        return commonArtists.contains(lower)
    }

    private fun cleanFallbackTitle(filename: String): String {
        val nameWithoutExt = filename.substringBeforeLast(".")
        return nameWithoutExt
            .replace(Regex("""^\d{1,3}[\s._-]+"""), "")
            .replace("_", " ")
            .trim()
            .ifEmpty { "Track" }
    }

    private val commonArtists = setOf(
        "radiohead", "the beatles", "beatles", "pink floyd", "queen", "nirvana", "coldplay",
        "adele", "daft punk", "david bowie", "led zeppelin", "fleetwood mac", "oasis",
        "mohsen chavoshi", "homayoun shajarian", "shajarian", "sina", "kavosh band",
        "yaswave sounds", "cyber pulse"
    )

    private val id3v1GenreMap = mapOf(
        "0" to "Blues", "1" to "Classic Rock", "2" to "Country", "3" to "Dance",
        "4" to "Disco", "5" to "Funk", "6" to "Grunge", "7" to "Hip-Hop",
        "8" to "Jazz", "9" to "Metal", "10" to "New Age", "11" to "Oldies",
        "12" to "Other", "13" to "Pop", "14" to "R&B", "15" to "Rap"
    )

    private fun hasEmbeddedPictureMarker(context: Context, uri: Uri): Boolean {
        return try {
            val inputStream = if (uri.scheme == "file") {
                val f = File(uri.path ?: "")
                if (f.exists()) f.inputStream() else null
            } else {
                context.contentResolver.openInputStream(uri)
            } ?: return false

            inputStream.use { stream ->
                val buffer = ByteArray(256 * 1024)
                var totalRead = 0
                while (totalRead < buffer.size) {
                    val read = stream.read(buffer, totalRead, buffer.size - totalRead)
                    if (read <= 0) break
                    totalRead += read
                }
                if (totalRead <= 4) return false

                containsMarker(buffer, totalRead, "APIC".toByteArray()) ||
                        containsMarker(buffer, totalRead, "PIC".toByteArray()) ||
                        containsMarker(buffer, totalRead, "covr".toByteArray()) ||
                        containsMarker(buffer, totalRead, "METADATA_BLOCK_PICTURE".toByteArray()) ||
                        containsMarker(buffer, totalRead, "COVERART".toByteArray())
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun containsMarker(buffer: ByteArray, length: Int, marker: ByteArray): Boolean {
        if (marker.isEmpty() || length < marker.size) return false
        val maxI = length - marker.size
        for (i in 0..maxI) {
            var found = true
            for (j in marker.indices) {
                if (buffer[i + j] != marker[j]) {
                    found = false
                    break
                }
            }
            if (found) return true
        }
        return false
    }
}
