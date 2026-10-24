package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.BottomNavBar
import com.example.ui.components.MiniPlayer
import com.example.ui.dialogs.AddToPlaylistDialog
import com.example.ui.dialogs.CreatePlaylistDialog
import com.example.ui.dialogs.DiagnosticDialog
import com.example.ui.dialogs.LrclibCandidateDialog
import com.example.ui.dialogs.LyricsEditorDialog
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.NowPlayingSheet
import com.example.ui.screens.OnboardingOverlay
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.QueueSheet
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VisualizerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.YasBackground
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val accentTheme by viewModel.accentTheme.collectAsState()
            MyApplicationTheme(accentTheme = accentTheme) {
                YasWaveApp(viewModel = viewModel, onExitApp = { finish() })
            }
        }
    }
}

@Composable
fun YasWaveApp(
    viewModel: MainViewModel = viewModel(),
    onExitApp: () -> Unit
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val isQueueSheetVisible by viewModel.isQueueSheetVisible.collectAsState()
    val currentTrack by viewModel.playerEngine.currentTrack.collectAsState()
    val isPlaying by viewModel.playerEngine.isPlaying.collectAsState()
    val currentPositionMs by viewModel.playerEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.playerEngine.durationMs.collectAsState()
    val isPersian by viewModel.isPersianLanguage.collectAsState()

    // Dialog & overlay states
    val showCreatePlaylist by viewModel.showCreatePlaylistDialog.collectAsState()
    val trackToAddToPlaylist by viewModel.trackToAddToPlaylist.collectAsState()
    val diagnosticTrack by viewModel.diagnosticTrack.collectAsState()
    val showLyricsEditor by viewModel.showLyricsEditor.collectAsState()
    val showOnboarding by viewModel.showOnboarding.collectAsState()
    val playlists by viewModel.repository.playlists.collectAsState()

    // LRCLIB candidate selection states
    val showLrclibDialog by viewModel.showLrclibCandidateDialog.collectAsState()
    val lrclibCandidates by viewModel.lrclibCandidates.collectAsState()
    val isSearchingLrclib by viewModel.isSearchingLrclib.collectAsState()
    val lrclibError by viewModel.lrclibSearchError.collectAsState()

    // Comprehensive Android Back Handling:
    // Dismisses dialogs -> closes Now Playing -> navigates back from sub-screens -> returns to Library -> exits
    BackHandler {
        if (!viewModel.handleBackPress()) {
            onExitApp()
        }
    }

    val layoutDirection = if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(YasBackground)
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Persistent Mini Player docked directly above Bottom Nav Bar
                        MiniPlayer(
                            currentTrack = currentTrack,
                            isPlaying = isPlaying,
                            currentPositionMs = currentPositionMs,
                            durationMs = durationMs,
                            onExpand = { viewModel.openNowPlaying() },
                            onTogglePlayPause = { viewModel.togglePlayPause() },
                            onSkipNext = { viewModel.skipNext() }
                        )

                        // Bottom Navigation Bar
                        BottomNavBar(
                            currentTab = currentTab,
                            onTabSelected = { viewModel.setTab(it) },
                            isPersian = isPersian
                        )
                    }
                },
                containerColor = YasBackground
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentTab) {
                        NavigationTab.LIBRARY -> LibraryScreen(viewModel = viewModel)
                        NavigationTab.PLAYLISTS -> PlaylistsScreen(viewModel = viewModel)
                        NavigationTab.LYRICS -> LyricsScreen(viewModel = viewModel)
                        NavigationTab.VISUALIZER -> VisualizerScreen(viewModel = viewModel)
                        NavigationTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }

            // Full Screen Now Playing Sheet Overlay
            if (isNowPlayingExpanded) {
                NowPlayingSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.closeNowPlaying() }
                )
            }

            // Queue Sheet
            if (isQueueSheetVisible) {
                QueueSheet(
                    viewModel = viewModel,
                    onDismiss = { viewModel.setQueueSheetVisible(false) }
                )
            }

            // LRCLIB candidate selection dialog
            if (showLrclibDialog && currentTrack != null) {
                LrclibCandidateDialog(
                    currentTrack = currentTrack!!,
                    candidates = lrclibCandidates,
                    isLoading = isSearchingLrclib,
                    errorMessage = lrclibError,
                    onSelectCandidate = { candidate ->
                        viewModel.applyLrclibCandidate(candidate)
                    },
                    onRetry = {
                        viewModel.searchLrclibForCurrentTrack()
                    },
                    onDismiss = {
                        viewModel.showLrclibDialog(false)
                    },
                    isPersian = isPersian
                )
            }

            // Dialogs
            if (showCreatePlaylist) {
                CreatePlaylistDialog(
                    onDismiss = { viewModel.showCreatePlaylistDialog(false) },
                    onConfirm = { name, desc ->
                        viewModel.repository.createPlaylist(name, desc)
                        viewModel.showCreatePlaylistDialog(false)
                    },
                    isPersian = isPersian
                )
            }

            if (trackToAddToPlaylist != null) {
                AddToPlaylistDialog(
                    track = trackToAddToPlaylist!!,
                    playlists = playlists,
                    onDismiss = { viewModel.showAddToPlaylistDialog(null) },
                    onSelectPlaylist = { playlistId ->
                        viewModel.repository.addTrackToPlaylist(playlistId, trackToAddToPlaylist!!.id)
                    },
                    onCreateNewPlaylist = {
                        viewModel.showAddToPlaylistDialog(null)
                        viewModel.showCreatePlaylistDialog(true)
                    },
                    isPersian = isPersian
                )
            }

            if (diagnosticTrack != null) {
                val diag = viewModel.repository.getDiagnosticForTrack(diagnosticTrack!!)
                DiagnosticDialog(
                    track = diagnosticTrack!!,
                    diagnostic = diag,
                    onDismiss = { viewModel.showDiagnostic(null) }
                )
            }

            if (showLyricsEditor && currentTrack != null) {
                val fullTrack = viewModel.currentTrackFull.collectAsState().value ?: currentTrack!!
                LyricsEditorDialog(
                    track = fullTrack,
                    onDismiss = { viewModel.showLyricsEditor(false) },
                    onSave = { synced, fa, plain, mode ->
                        viewModel.saveEditedLyrics(synced, fa, plain, mode)
                    },
                    isPersian = isPersian
                )
            }

            // First-launch Onboarding Overlay
            if (showOnboarding) {
                OnboardingOverlay(
                    onDismiss = { viewModel.dismissOnboarding() },
                    isPersian = isPersian
                )
            }
        }
    }
}
