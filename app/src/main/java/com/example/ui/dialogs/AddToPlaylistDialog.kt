package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.model.Playlist
import com.example.model.Track
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary

@Composable
fun AddToPlaylistDialog(
    track: Track,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onSelectPlaylist: (playlistId: String) -> Unit,
    onCreateNewPlaylist: () -> Unit,
    isPersian: Boolean
) {
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "افزودن به پلی‌لیست" else "Add to Playlist",
                color = YasTextPrimary
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${track.title} — ${track.artist}",
                    style = MaterialTheme.typography.bodySmall,
                    color = YasTextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Create new playlist shortcut
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCreateNewPlaylist)
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = accentColor
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPersian) "ایجاد پلی‌لیست تازه..." else "Create New Playlist...",
                        color = accentColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                ) {
                    items(playlists) { playlist ->
                        val alreadyInPlaylist = playlist.trackIds.contains(track.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectPlaylist(playlist.id)
                                    onDismiss()
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = if (alreadyInPlaylist) accentColor else YasTextMuted
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = playlist.name,
                                    color = YasTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "${playlist.trackIds.size} tracks" + if (alreadyInPlaylist) " (Added)" else "",
                                    color = YasTextMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text(if (isPersian) "بستن" else "Close", color = YasTextPrimary)
            }
        },
        containerColor = YasSurfaceElevated
    )
}
