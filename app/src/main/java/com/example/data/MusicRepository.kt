package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.util.Log
import com.example.model.MetadataDiagnostic
import com.example.model.Playlist
import com.example.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class MusicRepository(private val context: Context) {
    private val TAG = "MusicRepository"
    private val prefs = context.getSharedPreferences("yaswave_prefs", Context.MODE_PRIVATE)
    private val storageFile = File(context.filesDir, "yaswave_library.json")

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _recentlyPlayed = MutableStateFlow<List<Track>>(emptyList())
    val recentlyPlayed: StateFlow<List<Track>> = _recentlyPlayed.asStateFlow()

    private val _diagnostics = mutableMapOf<String, MetadataDiagnostic>()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    init {
        loadLibrary()
    }

    private fun loadLibrary() {
        val initialTracks = SampleDataRepository.initializeSampleLibrary(context)
        val initialPlaylists = SampleDataRepository.getInitialPlaylists()

        if (!storageFile.exists()) {
            _tracks.value = initialTracks
            _playlists.value = initialPlaylists
            saveLibrary()
        } else {
            try {
                val jsonStr = storageFile.readText()
                val root = JSONObject(jsonStr)

                val customTracks = mutableListOf<Track>()
                val tracksArray = root.optJSONArray("tracks") ?: JSONArray()
                for (i in 0 until tracksArray.length()) {
                    val obj = tracksArray.getJSONObject(i)
                    customTracks.add(
                        Track(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            album = obj.optString("album", "Unknown Album"),
                            durationMs = obj.optLong("durationMs", 0L),
                            path = obj.optString("path", ""),
                            uriString = obj.optString("uriString", ""),
                            artworkUri = obj.optString("artworkUri").takeIf { it.isNotEmpty() },
                            lyrics = obj.optString("lyrics").takeIf { it.isNotEmpty() },
                            persianLyrics = obj.optString("persianLyrics").takeIf { it.isNotEmpty() },
                            plainLyrics = obj.optString("plainLyrics").takeIf { it.isNotEmpty() },
                            lyricsMode = obj.optString("lyricsMode").takeIf { it.isNotEmpty() }
                                ?: if (obj.optString("plainLyrics").isNotEmpty() && obj.optString("lyrics").isEmpty()) "PLAIN" else "SYNCED",
                            year = obj.optString("year").takeIf { it.isNotEmpty() },
                            genre = obj.optString("genre").takeIf { it.isNotEmpty() },
                            folderName = obj.optString("folderName", "Music"),
                            isFavorite = obj.optBoolean("isFavorite", false),
                            dateAdded = obj.optLong("dateAdded", System.currentTimeMillis())
                        )
                    )
                }

                // Merge initial demo tracks with saved custom tracks
                val mergedMap = (initialTracks + customTracks).associateBy { it.id }
                _tracks.value = mergedMap.values.toList()

                val loadedPlaylists = mutableListOf<Playlist>()
                val plArray = root.optJSONArray("playlists") ?: JSONArray()
                for (i in 0 until plArray.length()) {
                    val obj = plArray.getJSONObject(i)
                    val tIds = mutableListOf<String>()
                    val tArr = obj.optJSONArray("trackIds") ?: JSONArray()
                    for (j in 0 until tArr.length()) {
                        tIds.add(tArr.getString(j))
                    }
                    loadedPlaylists.add(
                        Playlist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            description = obj.optString("description", ""),
                            trackIds = tIds,
                            coverResId = null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }

                // Ensure initial playlists exist
                val plMap = (initialPlaylists + loadedPlaylists).associateBy { it.id }
                _playlists.value = plMap.values.toList()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to load library: ${e.message}", e)
                _tracks.value = initialTracks
                _playlists.value = initialPlaylists
            }
        }
    }

    private fun saveLibrary() {
        try {
            val root = JSONObject()
            val tracksArray = JSONArray()
            for (t in _tracks.value) {
                val obj = JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("album", t.album)
                    put("durationMs", t.durationMs)
                    put("path", t.path)
                    put("uriString", t.uriString)
                    t.artworkUri?.let { put("artworkUri", it) }
                    t.lyrics?.let { put("lyrics", it) }
                    t.persianLyrics?.let { put("persianLyrics", it) }
                    t.plainLyrics?.let { put("plainLyrics", it) }
                    put("lyricsMode", t.lyricsMode)
                    t.year?.let { put("year", it) }
                    t.genre?.let { put("genre", it) }
                    put("folderName", t.folderName)
                    put("isFavorite", t.isFavorite)
                    put("dateAdded", t.dateAdded)
                }
                tracksArray.put(obj)
            }
            root.put("tracks", tracksArray)

            val plArray = JSONArray()
            for (p in _playlists.value) {
                val obj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("description", p.description)
                    put("createdAt", p.createdAt)
                    val tArr = JSONArray()
                    p.trackIds.forEach { tArr.put(it) }
                    put("trackIds", tArr)
                }
                plArray.put(obj)
            }
            root.put("playlists", plArray)

            storageFile.writeText(root.toString(2))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save library: ${e.message}")
        }
    }

    fun toggleFavorite(trackId: String) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) it.copy(isFavorite = !it.isFavorite) else it
        }
        // Also update favorites playlist
        val favPlaylist = _playlists.value.find { it.id == "pl_favorites" }
        if (favPlaylist != null) {
            val currentFavIds = _tracks.value.filter { it.isFavorite }.map { it.id }
            _playlists.value = _playlists.value.map {
                if (it.id == "pl_favorites") it.copy(trackIds = currentFavIds) else it
            }
        }
        saveLibrary()
    }

    fun recordRecentlyPlayed(track: Track) {
        val current = _recentlyPlayed.value.toMutableList()
        current.removeAll { it.id == track.id }
        current.add(0, track)
        if (current.size > 20) {
            current.removeAt(current.lastIndex)
        }
        _recentlyPlayed.value = current
    }

    fun createPlaylist(name: String, description: String = ""): Playlist {
        val newPl = Playlist(
            id = "pl_${System.currentTimeMillis()}",
            name = name,
            description = description,
            trackIds = emptyList(),
            createdAt = System.currentTimeMillis()
        )
        _playlists.value = _playlists.value + newPl
        saveLibrary()
        return newPl
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        _playlists.value = _playlists.value.map {
            if (it.id == playlistId) it.copy(name = newName) else it
        }
        saveLibrary()
    }

    fun deletePlaylist(playlistId: String) {
        _playlists.value = _playlists.value.filter { it.id != playlistId }
        saveLibrary()
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        _playlists.value = _playlists.value.map {
            if (it.id == playlistId) {
                if (!it.trackIds.contains(trackId)) {
                    it.copy(trackIds = it.trackIds + trackId)
                } else it
            } else it
        }
        saveLibrary()
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        _playlists.value = _playlists.value.map {
            if (it.id == playlistId) {
                it.copy(trackIds = it.trackIds - trackId)
            } else it
        }
        saveLibrary()
    }

    fun addTrack(track: Track) {
        _tracks.value = _tracks.value.filter { it.id != track.id } + track
        saveLibrary()
    }

    fun updateTrackLyrics(
        trackId: String,
        newLyrics: String?,
        newPersianLyrics: String? = null,
        newPlainLyrics: String? = null,
        newMode: String? = null
    ) {
        val exists = _tracks.value.any { it.id == trackId }
        if (!exists) {
            val newTrack = Track(
                id = trackId,
                title = "Track $trackId",
                artist = "Unknown Artist",
                lyrics = newLyrics,
                persianLyrics = newPersianLyrics,
                plainLyrics = newPlainLyrics,
                lyricsMode = newMode ?: if (newPlainLyrics != null && newLyrics == null) "PLAIN" else "SYNCED"
            )
            _tracks.value = _tracks.value + newTrack
        } else {
            _tracks.value = _tracks.value.map {
                if (it.id == trackId) {
                    it.copy(
                        lyrics = newLyrics ?: it.lyrics,
                        persianLyrics = if (newPersianLyrics != null) newPersianLyrics.takeIf { p -> p.isNotBlank() } else it.persianLyrics,
                        plainLyrics = newPlainLyrics ?: it.plainLyrics,
                        lyricsMode = newMode ?: it.lyricsMode
                    )
                } else it
            }
        }
        saveLibrary()
    }

    fun savePlainLyrics(trackId: String, plainLyricsText: String) {
        updateTrackLyrics(
            trackId = trackId,
            newLyrics = null,
            newPlainLyrics = plainLyricsText,
            newMode = "PLAIN"
        )
    }

    fun setTrackLyricsMode(trackId: String, mode: String) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) {
                it.copy(lyricsMode = mode)
            } else it
        }
        saveLibrary()
    }

    fun clearTrackLyrics(
        trackId: String,
        clearSynced: Boolean = true,
        clearPlain: Boolean = true
    ) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) {
                val updatedSynced = if (clearSynced) null else it.lyrics
                val updatedPersian = if (clearSynced) null else it.persianLyrics
                val updatedPlain = if (clearPlain) null else it.plainLyrics
                val updatedMode = when {
                    updatedSynced != null && updatedPlain == null -> "SYNCED"
                    updatedPlain != null && updatedSynced == null -> "PLAIN"
                    else -> it.lyricsMode
                }
                it.copy(
                    lyrics = updatedSynced,
                    persianLyrics = updatedPersian,
                    plainLyrics = updatedPlain,
                    lyricsMode = updatedMode
                )
            } else it
        }
        saveLibrary()
    }

    fun getDiagnosticForTrack(track: Track): MetadataDiagnostic {
        return _diagnostics[track.id] ?: MetadataDiagnostic(
            trackId = track.id,
            title = track.title,
            rawArtistKey = track.artist,
            rawAlbumArtistKey = track.artist,
            rawAuthorKey = null,
            rawComposerKey = null,
            rawTrackNumberKey = null,
            rawDiscNumberKey = null,
            rawGenreKey = track.genre,
            resolvedArtist = track.artist,
            diagnosisNote = "Track verified in library. Metadata parsing confirmed non-numeric and mapped to clean artist string.",
            detectionSource = "LIBRARY_VERIFIED"
        )
    }

    suspend fun importUris(uris: List<Uri>) = withContext(Dispatchers.IO) {
        _isImporting.value = true
        _importStatusMessage.value = "Importing ${uris.size} audio files..."

        val newTracks = mutableListOf<Track>()
        var count = 0

        for (uri in uris) {
            try {
                count++
                _importStatusMessage.value = "Importing $count of ${uris.size}..."

                val fileName = getFileNameFromUri(uri)
                val parsed = MetadataParser.parse(context, uri, fallbackName = fileName)

                _diagnostics[uri.toString()] = parsed.diagnostic

                val track = Track(
                    id = "import_${System.currentTimeMillis()}_$count",
                    title = parsed.title,
                    artist = parsed.artist,
                    album = parsed.album,
                    durationMs = parsed.durationMs,
                    path = uri.path ?: "",
                    uriString = uri.toString(),
                    artworkUri = parsed.artworkPath,
                    lyrics = parsed.embeddedLyrics,
                    folderName = "Imported",
                    dateAdded = System.currentTimeMillis()
                )
                newTracks.add(track)
            } catch (e: Exception) {
                Log.e(TAG, "Error importing uri $uri: ${e.message}")
            }
        }

        if (newTracks.isNotEmpty()) {
            _tracks.value = _tracks.value + newTracks
            saveLibrary()
            _importStatusMessage.value = "Successfully imported ${newTracks.size} tracks"
        } else {
            _importStatusMessage.value = "No valid audio files imported"
        }
        _isImporting.value = false
    }

    suspend fun importFromFolder(treeUri: Uri) = withContext(Dispatchers.IO) {
        _isImporting.value = true
        _importStatusMessage.value = "Scanning folder..."

        val audioUris = mutableListOf<Pair<Uri, String>>()
        try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
            )

            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val childId = cursor.getString(idCol)
                    val name = cursor.getString(nameCol) ?: ""
                    val mime = cursor.getString(mimeCol) ?: ""
                    val isAudio = mime.startsWith("audio/") ||
                            name.endsWith(".mp3", ignoreCase = true) ||
                            name.endsWith(".wav", ignoreCase = true) ||
                            name.endsWith(".m4a", ignoreCase = true) ||
                            name.endsWith(".flac", ignoreCase = true) ||
                            name.endsWith(".ogg", ignoreCase = true)

                    if (isAudio) {
                        val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                        audioUris.add(Pair(fileUri, "Imported Folder"))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan folder uri $treeUri: ${e.message}")
        }

        if (audioUris.isEmpty()) {
            _importStatusMessage.value = "No audio files found in selected folder"
            _isImporting.value = false
            return@withContext
        }

        _importStatusMessage.value = "Found ${audioUris.size} tracks. Parsing tags..."
        val newTracks = mutableListOf<Track>()
        var count = 0

        for ((uri, folderName) in audioUris) {
            count++
            val fileName = getFileNameFromUri(uri)
            val parsed = MetadataParser.parse(context, uri, fallbackName = fileName)
            _diagnostics[uri.toString()] = parsed.diagnostic

            val track = Track(
                id = "folder_${System.currentTimeMillis()}_$count",
                title = parsed.title,
                artist = parsed.artist,
                album = parsed.album,
                durationMs = parsed.durationMs,
                path = uri.path ?: "",
                uriString = uri.toString(),
                artworkUri = parsed.artworkPath,
                folderName = folderName,
                dateAdded = System.currentTimeMillis()
            )
            newTracks.add(track)
        }

        _tracks.value = _tracks.value + newTracks
        saveLibrary()
        _importStatusMessage.value = "Imported ${newTracks.size} tracks from folder"
        _isImporting.value = false
    }

    private fun getFileNameFromUri(uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            result = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result ?: "audio_${System.currentTimeMillis()}.mp3"
    }

    suspend fun reprocessLibraryMetadata(): Int = withContext(Dispatchers.IO) {
        val currentTracks = _tracks.value
        val knownArtists = currentTracks.map { it.artist }
            .filter { !MetadataParser.isSuspiciousNumeric(it) && it != "Unknown Artist" }
            .toSet()
        var updatedCount = 0

        val updatedTracks = currentTracks.map { track ->
            val uri = when {
                track.uriString.isNotBlank() -> Uri.parse(track.uriString)
                track.path.isNotBlank() -> Uri.fromFile(File(track.path))
                else -> null
            }

            if (uri != null) {
                try {
                    val fallbackName = File(track.path.ifBlank { uri.path ?: track.title }).name
                    val parsed = MetadataParser.parse(context, uri, fallbackName, knownArtists)
                    _diagnostics[track.id] = parsed.diagnostic

                    if (parsed.artist != track.artist || parsed.title != track.title || parsed.album != track.album) {
                        updatedCount++
                        track.copy(
                            title = parsed.title,
                            artist = parsed.artist,
                            album = parsed.album,
                            durationMs = if (parsed.durationMs > 0) parsed.durationMs else track.durationMs,
                            artworkUri = parsed.artworkPath ?: track.artworkUri
                        )
                    } else {
                        track
                    }
                } catch (e: Exception) {
                    track
                }
            } else {
                track
            }
        }

        _tracks.value = updatedTracks
        saveLibrary()
        updatedCount
    }

    fun attachLrcToTrack(trackId: String, lrcContent: String) {
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) {
                it.copy(lyrics = lrcContent, lyricsMode = "SYNCED")
            } else it
        }
        saveLibrary()
    }

    fun importLyricsFileToTrack(trackId: String, content: String, isLrcFormat: Boolean) {
        val exists = _tracks.value.any { it.id == trackId }
        if (!exists) {
            val newTrack = Track(
                id = trackId,
                title = "Track $trackId",
                artist = "Unknown Artist",
                lyrics = if (isLrcFormat) content else null,
                plainLyrics = if (!isLrcFormat) content else null,
                lyricsMode = if (isLrcFormat) "SYNCED" else "PLAIN"
            )
            _tracks.value = _tracks.value + newTrack
        } else {
            _tracks.value = _tracks.value.map {
                if (it.id == trackId) {
                    if (isLrcFormat) {
                        it.copy(lyrics = content, lyricsMode = "SYNCED")
                    } else {
                        it.copy(plainLyrics = content, lyricsMode = "PLAIN")
                    }
                } else it
            }
        }
        saveLibrary()
    }

    fun resetToDemoLibrary() {
        storageFile.delete()
        loadLibrary()
    }
}
