package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.LrclibCandidate
import com.example.data.LrclibException
import com.example.data.LrclibService
import com.example.data.LyricsParser
import com.example.data.MusicRepository
import com.example.model.LyricLine
import com.example.model.MetadataDiagnostic
import com.example.model.Playlist
import com.example.model.Track
import com.example.playback.AudioPlayerEngine
import com.example.playback.EqualizerPreset
import com.example.playback.RepeatMode
import com.example.ui.theme.AccentTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationTab {
    LIBRARY, PLAYLISTS, LYRICS, VISUALIZER, SETTINGS
}

enum class SortMode {
    TITLE, ARTIST, DURATION, DATE_ADDED
}

enum class LibraryFilter {
    ALL, TRACKS, ARTISTS, ALBUMS, FOLDERS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = MusicRepository(application)
    val playerEngine = AudioPlayerEngine(application)
    private val prefs = application.getSharedPreferences("yaswave_ui_prefs", Application.MODE_PRIVATE)

    // Accent Theme
    private val _accentTheme = MutableStateFlow(
        try {
            AccentTheme.valueOf(prefs.getString("accent_theme", AccentTheme.SAGE.name) ?: AccentTheme.SAGE.name)
        } catch (_: Exception) {
            AccentTheme.SAGE
        }
    )
    val accentTheme: StateFlow<AccentTheme> = _accentTheme.asStateFlow()

    // Navigation & Sheet state
    private val _currentTab = MutableStateFlow(NavigationTab.LIBRARY)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _isQueueSheetVisible = MutableStateFlow(false)
    val isQueueSheetVisible: StateFlow<Boolean> = _isQueueSheetVisible.asStateFlow()

    // Sub-screens
    private val _selectedPlaylist = MutableStateFlow<Playlist?>(null)
    val selectedPlaylist: StateFlow<Playlist?> = _selectedPlaylist.asStateFlow()

    private val _selectedArtist = MutableStateFlow<String?>(null)
    val selectedArtist: StateFlow<String?> = _selectedArtist.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<String?>(null)
    val selectedAlbum: StateFlow<String?> = _selectedAlbum.asStateFlow()

    private val _selectedFolder = MutableStateFlow<String?>(null)
    val selectedFolder: StateFlow<String?> = _selectedFolder.asStateFlow()

    // Dialogs
    private val _showCreatePlaylistDialog = MutableStateFlow(false)
    val showCreatePlaylistDialog: StateFlow<Boolean> = _showCreatePlaylistDialog.asStateFlow()

    private val _trackToAddToPlaylist = MutableStateFlow<Track?>(null)
    val trackToAddToPlaylist: StateFlow<Track?> = _trackToAddToPlaylist.asStateFlow()

    private val _diagnosticTrack = MutableStateFlow<Track?>(null)
    val diagnosticTrack: StateFlow<Track?> = _diagnosticTrack.asStateFlow()

    private val _showLyricsEditor = MutableStateFlow(false)
    val showLyricsEditor: StateFlow<Boolean> = _showLyricsEditor.asStateFlow()

    // LRCLIB candidate search
    private val _showLrclibCandidateDialog = MutableStateFlow(false)
    val showLrclibCandidateDialog: StateFlow<Boolean> = _showLrclibCandidateDialog.asStateFlow()

    private val _isSearchingLrclib = MutableStateFlow(false)
    val isSearchingLrclib: StateFlow<Boolean> = _isSearchingLrclib.asStateFlow()

    private val _lrclibCandidates = MutableStateFlow<List<LrclibCandidate>>(emptyList())
    val lrclibCandidates: StateFlow<List<LrclibCandidate>> = _lrclibCandidates.asStateFlow()

    private val _lrclibSearchError = MutableStateFlow<String?>(null)
    val lrclibSearchError: StateFlow<String?> = _lrclibSearchError.asStateFlow()

    // Lyrics Timing Offset (+/- ms)
    private val _lyricsOffsetMs = MutableStateFlow(prefs.getLong("lyrics_offset_ms", 0L))
    val lyricsOffsetMs: StateFlow<Long> = _lyricsOffsetMs.asStateFlow()

    // Onboarding & Language
    private val _showOnboarding = MutableStateFlow(prefs.getBoolean("first_launch", true))
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    private val _isPersianLanguage = MutableStateFlow(prefs.getBoolean("persian_lang", false))
    val isPersianLanguage: StateFlow<Boolean> = _isPersianLanguage.asStateFlow()

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _libraryFilter = MutableStateFlow(LibraryFilter.ALL)
    val libraryFilter: StateFlow<LibraryFilter> = _libraryFilter.asStateFlow()

    private val _sortMode = MutableStateFlow(SortMode.TITLE)
    val sortMode: StateFlow<SortMode> = _sortMode.asStateFlow()

    // Fully synchronized current track state from repository
    val currentTrackFull: StateFlow<Track?> = combine(
        playerEngine.currentTrack,
        repository.tracks
    ) { current, allTracks ->
        allTracks.find { it.id == current?.id } ?: current
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Current Lyrics Mode: "SYNCED" or "PLAIN"
    val currentLyricsMode: StateFlow<String> = currentTrackFull.combine(
        MutableStateFlow(Unit)
    ) { track, _ ->
        track?.lyricsMode ?: "SYNCED"
    }.stateIn(viewModelScope, SharingStarted.Lazily, "SYNCED")

    // Active parsed synchronized lyrics
    val activeLyrics: StateFlow<List<LyricLine>> = currentTrackFull.combine(
        MutableStateFlow(Unit)
    ) { track, _ ->
        if (track?.lyrics != null) {
            LyricsParser.parseLrc(track.lyrics, track.persianLyrics)
        } else {
            emptyList()
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Active plain lyrics text
    val activePlainLyrics: StateFlow<String?> = currentTrackFull.combine(
        MutableStateFlow(Unit)
    ) { track, _ ->
        track?.plainLyrics
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Active lyric index (taking offset into account)
    val activeLyricIndex: StateFlow<Int> = combine(
        activeLyrics,
        playerEngine.currentPositionMs,
        _lyricsOffsetMs
    ) { lyrics, pos, offset ->
        val adjustedPos = (pos - offset).coerceAtLeast(0L)
        LyricsParser.findActiveLyricIndex(lyrics, adjustedPos)
    }.stateIn(viewModelScope, SharingStarted.Lazily, -1)

    // Filtered and sorted tracks
    val filteredTracks: StateFlow<List<Track>> = combine(
        repository.tracks,
        _searchQuery,
        _sortMode
    ) { tracks, query, sort ->
        var list = if (query.isBlank()) {
            tracks
        } else {
            val q = query.trim().lowercase()
            tracks.filter {
                it.title.lowercase().contains(q) ||
                        it.artist.lowercase().contains(q) ||
                        it.album.lowercase().contains(q) ||
                        it.folderName.lowercase().contains(q)
            }
        }

        list = when (sort) {
            SortMode.TITLE -> list.sortedBy { it.title.lowercase() }
            SortMode.ARTIST -> list.sortedBy { it.artist.lowercase() }
            SortMode.DURATION -> list.sortedByDescending { it.durationMs }
            SortMode.DATE_ADDED -> list.sortedByDescending { it.dateAdded }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setAccentTheme(theme: AccentTheme) {
        _accentTheme.value = theme
        prefs.edit().putString("accent_theme", theme.name).apply()
    }

    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
    }

    fun openNowPlaying() {
        _isNowPlayingExpanded.value = true
    }

    fun closeNowPlaying() {
        _isNowPlayingExpanded.value = false
    }

    fun setQueueSheetVisible(visible: Boolean) {
        _isQueueSheetVisible.value = visible
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setLibraryFilter(filter: LibraryFilter) {
        _libraryFilter.value = filter
    }

    fun setSortMode(sort: SortMode) {
        _sortMode.value = sort
    }

    fun playTrack(track: Track, customQueue: List<Track>? = null) {
        val q = customQueue ?: filteredTracks.value
        playerEngine.playTrack(track, q)
        repository.recordRecentlyPlayed(track)
    }

    fun togglePlayPause() {
        playerEngine.togglePlayPause()
    }

    fun skipNext() {
        playerEngine.skipNext()
        playerEngine.currentTrack.value?.let { repository.recordRecentlyPlayed(it) }
    }

    fun skipPrevious() {
        playerEngine.skipPrevious()
        playerEngine.currentTrack.value?.let { repository.recordRecentlyPlayed(it) }
    }

    fun seekTo(posMs: Long) {
        playerEngine.seekTo(posMs)
    }

    fun toggleShuffle() {
        playerEngine.toggleShuffle()
    }

    fun cycleRepeatMode() {
        playerEngine.cycleRepeatMode()
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        playerEngine.selectPreset(preset)
    }

    fun toggleFavorite(track: Track) {
        repository.toggleFavorite(track.id)
    }

    // Playlist sub-navigation
    fun openPlaylist(playlist: Playlist) {
        _selectedPlaylist.value = playlist
    }

    fun closePlaylist() {
        _selectedPlaylist.value = null
    }

    fun openArtist(artistName: String) {
        _selectedArtist.value = artistName
    }

    fun closeArtist() {
        _selectedArtist.value = null
    }

    fun openAlbum(albumName: String) {
        _selectedAlbum.value = albumName
    }

    fun closeAlbum() {
        _selectedAlbum.value = null
    }

    fun openFolder(folderName: String) {
        _selectedFolder.value = folderName
    }

    fun closeFolder() {
        _selectedFolder.value = null
    }

    // Dialog controls
    fun showCreatePlaylistDialog(show: Boolean) {
        _showCreatePlaylistDialog.value = show
    }

    fun showAddToPlaylistDialog(track: Track?) {
        _trackToAddToPlaylist.value = track
    }

    fun showDiagnostic(track: Track?) {
        _diagnosticTrack.value = track
    }

    fun showLyricsEditor(show: Boolean) {
        _showLyricsEditor.value = show
    }

    fun showLrclibDialog(show: Boolean) {
        _showLrclibCandidateDialog.value = show
        if (show) {
            searchLrclibForCurrentTrack()
        }
    }

    fun searchLrclibForCurrentTrack() {
        val current = playerEngine.currentTrack.value ?: return
        viewModelScope.launch {
            _isSearchingLrclib.value = true
            _lrclibSearchError.value = null
            _lrclibCandidates.value = emptyList()

            val result = LrclibService.searchLyrics(current.title, current.artist)
            result.onSuccess { list ->
                _lrclibCandidates.value = list
                if (list.isEmpty()) {
                    _lrclibSearchError.value = "No lyrics candidates found on LRCLIB."
                }
            }.onFailure { err ->
                val errorMessage = when (err) {
                    is LrclibException.AccessForbiddenException ->
                        "Access forbidden (HTTP 403). LRCLIB may be restricting requests or requiring updated client headers."
                    is LrclibException.RateLimitedException ->
                        "Rate limit reached (HTTP 429). Please wait a moment before searching again."
                    is LrclibException.NetworkException ->
                        "Network error: Unable to connect to LRCLIB. Please check your internet connection."
                    is LrclibException.ServerException ->
                        "LRCLIB service error (${err.message})."
                    else -> err.message ?: "Failed to connect to LRCLIB."
                }
                _lrclibSearchError.value = errorMessage
            }
            _isSearchingLrclib.value = false
        }
    }

    fun applyLrclibCandidate(candidate: LrclibCandidate) {
        val current = playerEngine.currentTrack.value ?: return
        val hasSynced = candidate.hasSyncedLyrics
        val hasPlain = candidate.hasPlainLyrics

        if (hasSynced || hasPlain) {
            val targetMode = if (hasSynced) "SYNCED" else "PLAIN"
            repository.updateTrackLyrics(
                trackId = current.id,
                newLyrics = candidate.syncedLyrics,
                newPlainLyrics = candidate.plainLyrics,
                newMode = targetMode
            )
        }
        _showLrclibCandidateDialog.value = false
    }

    fun setLyricsMode(mode: String) {
        val current = playerEngine.currentTrack.value ?: return
        repository.setTrackLyricsMode(current.id, mode)
    }

    fun savePlainLyrics(text: String) {
        val current = playerEngine.currentTrack.value ?: return
        repository.savePlainLyrics(current.id, text)
    }

    fun importLyricsContent(content: String, fileName: String) {
        val current = playerEngine.currentTrack.value ?: return
        val isLrc = fileName.endsWith(".lrc", ignoreCase = true) || LyricsParser.hasLrcTimestamps(content)
        repository.importLyricsFileToTrack(current.id, content, isLrc)
    }

    fun clearCurrentTrackLyrics(clearSynced: Boolean = true, clearPlain: Boolean = true) {
        val current = playerEngine.currentTrack.value ?: return
        repository.clearTrackLyrics(current.id, clearSynced, clearPlain)
    }

    fun importLrcContent(lrcText: String) {
        importLyricsContent(lrcText, "lyrics.lrc")
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val newOffset = _lyricsOffsetMs.value + deltaMs
        _lyricsOffsetMs.value = newOffset
        prefs.edit().putLong("lyrics_offset_ms", newOffset).apply()
    }

    fun resetLyricsOffset() {
        _lyricsOffsetMs.value = 0L
        prefs.edit().putLong("lyrics_offset_ms", 0L).apply()
    }

    fun saveEditedLyrics(
        newLyrics: String?,
        newPersianLyrics: String? = null,
        newPlainLyrics: String? = null,
        newMode: String? = null
    ) {
        val current = playerEngine.currentTrack.value ?: return
        repository.updateTrackLyrics(
            trackId = current.id,
            newLyrics = newLyrics,
            newPersianLyrics = newPersianLyrics,
            newPlainLyrics = newPlainLyrics,
            newMode = newMode
        )
        _showLyricsEditor.value = false
    }

    fun reprocessLibraryMetadata(onComplete: (updatedCount: Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.reprocessLibraryMetadata()
            onComplete(count)
        }
    }

    fun togglePersianLanguage() {
        val next = !_isPersianLanguage.value
        _isPersianLanguage.value = next
        prefs.edit().putBoolean("persian_lang", next).apply()
    }

    fun dismissOnboarding() {
        _showOnboarding.value = false
        prefs.edit().putBoolean("first_launch", false).apply()
    }

    fun resetOnboarding() {
        _showOnboarding.value = true
        prefs.edit().putBoolean("first_launch", true).apply()
    }

    fun importFiles(uris: List<Uri>) {
        viewModelScope.launch {
            repository.importUris(uris)
        }
    }

    fun importFolder(treeUri: Uri) {
        viewModelScope.launch {
            repository.importFromFolder(treeUri)
        }
    }

    fun handleBackPress(): Boolean {
        if (_showLrclibCandidateDialog.value) {
            _showLrclibCandidateDialog.value = false
            return true
        }
        if (_showLyricsEditor.value) {
            _showLyricsEditor.value = false
            return true
        }
        if (_diagnosticTrack.value != null) {
            _diagnosticTrack.value = null
            return true
        }
        if (_trackToAddToPlaylist.value != null) {
            _trackToAddToPlaylist.value = null
            return true
        }
        if (_showCreatePlaylistDialog.value) {
            _showCreatePlaylistDialog.value = false
            return true
        }
        if (_isQueueSheetVisible.value) {
            _isQueueSheetVisible.value = false
            return true
        }
        if (_isNowPlayingExpanded.value) {
            _isNowPlayingExpanded.value = false
            return true
        }
        if (_selectedPlaylist.value != null) {
            _selectedPlaylist.value = null
            return true
        }
        if (_selectedArtist.value != null) {
            _selectedArtist.value = null
            return true
        }
        if (_selectedAlbum.value != null) {
            _selectedAlbum.value = null
            return true
        }
        if (_selectedFolder.value != null) {
            _selectedFolder.value = null
            return true
        }
        if (_searchQuery.value.isNotEmpty()) {
            _searchQuery.value = ""
            return true
        }
        if (_currentTab.value != NavigationTab.LIBRARY) {
            _currentTab.value = NavigationTab.LIBRARY
            return true
        }
        return false
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.onDestroy()
    }
}
