package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.playback.RepeatMode
import com.example.ui.components.WaveVisualizer
import com.example.ui.theme.YasBackground
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.NavigationTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val track by viewModel.playerEngine.currentTrack.collectAsState()
    val isPlaying by viewModel.playerEngine.isPlaying.collectAsState()
    val positionMs by viewModel.playerEngine.currentPositionMs.collectAsState()
    val durationMs by viewModel.playerEngine.durationMs.collectAsState()
    val isShuffle by viewModel.playerEngine.isShuffle.collectAsState()
    val repeatMode by viewModel.playerEngine.repeatMode.collectAsState()
    val spectrumBands by viewModel.playerEngine.spectrumBands.collectAsState()
    val isPersian by viewModel.isPersianLanguage.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPositionMs by remember { mutableFloatStateOf(0f) }

    if (track == null) {
        onDismiss()
        return
    }

    val currentTrack = track!!

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .testTag("now_playing_screen"),
        color = YasBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_now_playing_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse Player",
                        tint = YasTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isPersian) "در حال پخش" else "NOW PLAYING",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = YasTextMuted
                    )
                    Text(
                        text = currentTrack.album,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = YasTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = { viewModel.setQueueSheetVisible(true) },
                    modifier = Modifier.testTag("open_queue_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = "Queue",
                        tint = YasTextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Prominent Album Artwork (Clean, unadorned, allowing its own vivid color to shine)
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, YasBorder, RoundedCornerShape(16.dp))
                    .background(YasSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (currentTrack.artworkResId != null) {
                    Image(
                        painter = painterResource(id = currentTrack.artworkResId),
                        contentDescription = "Artwork for ${currentTrack.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (!currentTrack.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = currentTrack.artworkUri,
                        contentDescription = "Artwork for ${currentTrack.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = YasTextMuted,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }

            // Clean Wave Visualizer bar directly under the artwork
            WaveVisualizer(
                bands = spectrumBands,
                height = 36.dp,
                accentColor = accentColor,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 4.dp)
            )

            // Track Title & Artist
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTrack.title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = YasTextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = currentTrack.artist,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = YasTextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable {
                        viewModel.openArtist(currentTrack.artist)
                        onDismiss()
                    }
                )
            }

            // Scrubbable Seek Bar with elapsed & remaining/total duration
            Column(modifier = Modifier.fillMaxWidth()) {
                val currentSliderValue = if (isScrubbing) scrubPositionMs else positionMs.toFloat()
                val totalDuration = durationMs.coerceAtLeast(1L).toFloat()

                Slider(
                    value = currentSliderValue.coerceIn(0f, totalDuration),
                    onValueChange = {
                        isScrubbing = true
                        scrubPositionMs = it
                    },
                    onValueChangeFinished = {
                        isScrubbing = false
                        viewModel.seekTo(scrubPositionMs.toLong())
                    },
                    valueRange = 0f..totalDuration,
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = YasBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displayPos = if (isScrubbing) scrubPositionMs.toLong() else positionMs
                    Text(
                        text = formatTime(displayPos),
                        style = MaterialTheme.typography.bodySmall,
                        color = YasTextMuted
                    )
                    // Display remaining duration as "-mm:ss"
                    val remainingMs = (durationMs - displayPos).coerceAtLeast(0L)
                    Text(
                        text = "-${formatTime(remainingMs)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = YasTextMuted
                    )
                }
            }

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Button
                IconButton(
                    onClick = { viewModel.toggleShuffle() },
                    modifier = Modifier.testTag("shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (isShuffle) accentColor else YasTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Previous Button
                IconButton(
                    onClick = { viewModel.skipPrevious() },
                    modifier = Modifier.testTag("prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = YasTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Central Play/Pause Button (Solid accent)
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                        .clickable { viewModel.togglePlayPause() }
                        .testTag("now_playing_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color(0xFF101012),
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next Button
                IconButton(
                    onClick = { viewModel.skipNext() },
                    modifier = Modifier.testTag("next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = YasTextPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode Button
                IconButton(
                    onClick = { viewModel.cycleRepeatMode() },
                    modifier = Modifier.testTag("repeat_btn")
                ) {
                    Icon(
                        imageVector = if (repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        tint = if (repeatMode != RepeatMode.OFF) accentColor else YasTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Bottom Actions: Favorite, Dedicated Lyrics Pill Button, EQ, Diagnostic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Favorite Button
                IconButton(onClick = { viewModel.toggleFavorite(currentTrack) }) {
                    Icon(
                        imageVector = if (currentTrack.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (currentTrack.isFavorite) accentColor else YasTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Clear prominent path to Synchronized Lyrics
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            viewModel.setTab(NavigationTab.LYRICS)
                            onDismiss()
                        }
                        .testTag("now_playing_lyrics_path_btn"),
                    color = YasSurfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersian) "مشاهده متن ترانه" else "Lyrics",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = YasTextPrimary
                        )
                    }
                }

                // Equalizer Button
                IconButton(onClick = {
                    viewModel.setTab(NavigationTab.VISUALIZER)
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = YasTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Diagnostic Inspector Button
                IconButton(onClick = {
                    viewModel.showDiagnostic(currentTrack)
                }) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Diagnostic Info",
                        tint = YasTextMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
