package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.components.TrackItem
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun PlaylistsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.repository.playlists.collectAsState()
    val allTracks by viewModel.repository.tracks.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val isPersian by viewModel.isPersianLanguage.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    if (selectedPlaylist != null) {
        val playlist = selectedPlaylist!!
        val playlistTracks = playlist.trackIds.mapNotNull { id -> allTracks.find { it.id == id } }

        PlaylistDetailView(
            playlist = playlist,
            tracks = playlistTracks,
            onBack = { viewModel.closePlaylist() },
            onPlayAll = {
                if (playlistTracks.isNotEmpty()) {
                    viewModel.playTrack(playlistTracks.first(), playlistTracks)
                }
            },
            onShufflePlay = {
                if (playlistTracks.isNotEmpty()) {
                    viewModel.toggleShuffle()
                    viewModel.playTrack(playlistTracks.random(), playlistTracks)
                }
            },
            onRemoveTrack = { trackId ->
                viewModel.repository.removeTrackFromPlaylist(playlist.id, trackId)
                viewModel.openPlaylist(playlist.copy(trackIds = playlist.trackIds - trackId))
            },
            onDeletePlaylist = {
                viewModel.repository.deletePlaylist(playlist.id)
                viewModel.closePlaylist()
            },
            viewModel = viewModel,
            isPersian = isPersian
        )
        return
    }

    Scaffold(
        modifier = modifier.testTag("playlists_screen"),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreatePlaylistDialog(true) },
                containerColor = accentColor,
                contentColor = Color(0xFF101012),
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .testTag("create_playlist_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Playlist")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item {
                Text(
                    text = if (isPersian) "پلی‌لیست‌ها" else "Playlists",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = YasTextPrimary,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            }

            items(playlists) { playlist ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.openPlaylist(playlist) }
                        .testTag("playlist_card_${playlist.id}"),
                    color = YasSurfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF26262B)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (playlist.coverResId != null) {
                                Image(
                                    painter = painterResource(id = playlist.coverResId),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(56.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = null,
                                    tint = YasTextSecondary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = YasTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (playlist.description.isNotBlank()) {
                                Text(
                                    text = playlist.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = YasTextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "${playlist.trackIds.size} " + if (isPersian) "قطعه" else "tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    playlist: Playlist,
    tracks: List<Track>,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onShufflePlay: () -> Unit,
    onRemoveTrack: (trackId: String) -> Unit,
    onDeletePlaylist: () -> Unit,
    viewModel: MainViewModel,
    isPersian: Boolean
) {
    val currentTrack by viewModel.playerEngine.currentTrack.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = YasTextPrimary)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF26262B)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (playlist.coverResId != null) {
                            Image(
                                painter = painterResource(id = playlist.coverResId),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(80.dp)
                            )
                        } else {
                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = YasTextSecondary, modifier = Modifier.size(40.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = YasTextPrimary
                        )
                        if (playlist.description.isNotBlank()) {
                            Text(
                                text = playlist.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextMuted
                            )
                        }
                        Text(
                            text = "${tracks.size} " + if (isPersian) "قطعه" else "tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor
                        )
                    }

                    if (playlist.id != "pl_favorites") {
                        IconButton(onClick = onDeletePlaylist) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Playlist", tint = YasTextMuted)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Play All & Shuffle Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayAll,
                        enabled = tracks.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color(0xFF101012), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPersian) "پخش همه" else "Play All", color = Color(0xFF101012))
                    }

                    OutlinedButton(
                        onClick = onShufflePlay,
                        enabled = tracks.isNotEmpty(),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Shuffle, contentDescription = null, tint = YasTextPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPersian) "پخش تصادفی" else "Shuffle", color = YasTextPrimary)
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Text(
                    text = if (isPersian) "این پلی‌لیست خالی است. از صفحه کتابخانه آهنگ اضافه کنید." else "This playlist is empty. Add tracks from Library.",
                    color = YasTextMuted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            items(tracks, key = { it.id }) { track ->
                TrackItem(
                    track = track,
                    isPlaying = track.id == currentTrack?.id,
                    onClick = { viewModel.playTrack(track, tracks) },
                    onToggleFavorite = { viewModel.toggleFavorite(track) },
                    onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                    onShowDiagnostics = { viewModel.showDiagnostic(track) },
                    onEditLyrics = { viewModel.showLyricsEditor(true) }
                )
            }
        }
    }
}
