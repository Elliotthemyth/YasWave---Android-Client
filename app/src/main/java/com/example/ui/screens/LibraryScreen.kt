package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.Track
import com.example.ui.components.TrackItem
import com.example.ui.theme.YasBackground
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurface
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary
import com.example.viewmodel.LibraryFilter
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.SortMode

@Composable
fun LibraryScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val filteredTracks by viewModel.filteredTracks.collectAsState()
    val allTracks by viewModel.repository.tracks.collectAsState()
    val recentlyPlayed by viewModel.repository.recentlyPlayed.collectAsState()
    val currentTrack by viewModel.playerEngine.currentTrack.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val filter by viewModel.libraryFilter.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val isImporting by viewModel.repository.isImporting.collectAsState()
    val importMsg by viewModel.repository.importStatusMessage.collectAsState()
    val isPersian by viewModel.isPersianLanguage.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    // Sub-screen states
    val selectedArtist by viewModel.selectedArtist.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showAddMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // SAF File pickers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri>? ->
        if (!uris.isNullOrEmpty()) {
            viewModel.importFiles(uris)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            viewModel.importFolder(treeUri)
        }
    }

    // Sub-screens for Artist, Album, Folder
    if (selectedArtist != null) {
        SubScreenDetail(
            title = selectedArtist!!,
            subtitle = if (isPersian) "آهنگ‌های هنرمند" else "Artist Tracks",
            tracks = allTracks.filter { it.artist.equals(selectedArtist, ignoreCase = true) },
            currentTrack = currentTrack,
            onBack = { viewModel.closeArtist() },
            viewModel = viewModel
        )
        return
    }

    if (selectedAlbum != null) {
        SubScreenDetail(
            title = selectedAlbum!!,
            subtitle = if (isPersian) "ترانه‌های آلبوم" else "Album Tracks",
            tracks = allTracks.filter { it.album.equals(selectedAlbum, ignoreCase = true) },
            currentTrack = currentTrack,
            onBack = { viewModel.closeAlbum() },
            viewModel = viewModel
        )
        return
    }

    if (selectedFolder != null) {
        SubScreenDetail(
            title = selectedFolder!!,
            subtitle = if (isPersian) "فایل‌های صوتی پوشه" else "Folder Files",
            tracks = allTracks.filter { it.folderName.equals(selectedFolder, ignoreCase = true) },
            currentTrack = currentTrack,
            onBack = { viewModel.closeFolder() },
            viewModel = viewModel
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("library_screen"),
        contentPadding = PaddingValues(bottom = 120.dp) // Ensures mini-player does not obscure last item
    ) {
        // Compact App Bar with clean typography and unobtrusive action icons
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Branding
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "YASWAVE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            ),
                            color = YasTextPrimary
                        )
                    }

                    // Compact Actions: Search, Sort, Unobtrusive Add/Import Menu
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = if (isSearchExpanded) accentColor else YasTextSecondary
                            )
                        }

                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort Options",
                                    tint = YasTextSecondary
                                )
                            }

                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false },
                                modifier = Modifier.background(YasSurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Sort by Title", color = if (sortMode == SortMode.TITLE) accentColor else YasTextPrimary) },
                                    onClick = {
                                        viewModel.setSortMode(SortMode.TITLE)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Artist", color = if (sortMode == SortMode.ARTIST) accentColor else YasTextPrimary) },
                                    onClick = {
                                        viewModel.setSortMode(SortMode.ARTIST)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Duration", color = if (sortMode == SortMode.DURATION) accentColor else YasTextPrimary) },
                                    onClick = {
                                        viewModel.setSortMode(SortMode.DURATION)
                                        showSortMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sort by Date Added", color = if (sortMode == SortMode.DATE_ADDED) accentColor else YasTextPrimary) },
                                    onClick = {
                                        viewModel.setSortMode(SortMode.DATE_ADDED)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }

                        // Unobtrusive Add/Import Button
                        Box {
                            IconButton(
                                onClick = { showAddMenu = true },
                                modifier = Modifier.testTag("add_import_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add music",
                                    tint = accentColor
                                )
                            }

                            DropdownMenu(
                                expanded = showAddMenu,
                                onDismissRequest = { showAddMenu = false },
                                modifier = Modifier.background(YasSurfaceElevated)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null, tint = YasTextSecondary) },
                                    text = { Text(if (isPersian) "افزودن فایل‌های صوتی..." else "Import Audio Files...", color = YasTextPrimary) },
                                    onClick = {
                                        showAddMenu = false
                                        filePickerLauncher.launch(arrayOf("audio/*"))
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = YasTextSecondary) },
                                    text = { Text(if (isPersian) "افزودن پوشه موسیقی..." else "Import Music Folder...", color = YasTextPrimary) },
                                    onClick = {
                                        showAddMenu = false
                                        folderPickerLauncher.launch(null)
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = YasTextSecondary) },
                                    text = { Text(if (isPersian) "پردازش مجدد متادیتا" else "Reprocess Library Metadata", color = YasTextPrimary) },
                                    onClick = {
                                        showAddMenu = false
                                        viewModel.reprocessLibraryMetadata { count ->
                                            Toast.makeText(context, "Metadata reprocessed ($count tracks updated)", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Compact Search Bar
                AnimatedVisibility(visible = isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(if (isPersian) "جستجوی آهنگ، خواننده، آلبوم..." else "Search tracks, artists, albums...", color = YasTextMuted, fontSize = 14.sp) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = YasTextMuted)
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("library_search_input"),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = YasTextPrimary,
                            unfocusedTextColor = YasTextPrimary,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = YasBorder,
                            focusedContainerColor = YasSurfaceVariant,
                            unfocusedContainerColor = YasSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Import Status Progress Banner (only when active)
                if (isImporting || importMsg != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = YasSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = accentColor
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = importMsg ?: "Importing...",
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextPrimary
                            )
                        }
                    }
                }

                // Library Filter Chips (Clean charcoal pills)
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterItems = listOf(
                        Pair(LibraryFilter.ALL, if (isPersian) "همه" else "All"),
                        Pair(LibraryFilter.TRACKS, if (isPersian) "قطعات" else "Tracks"),
                        Pair(LibraryFilter.ARTISTS, if (isPersian) "هنرمندان" else "Artists"),
                        Pair(LibraryFilter.ALBUMS, if (isPersian) "آلبوم‌ها" else "Albums"),
                        Pair(LibraryFilter.FOLDERS, if (isPersian) "پوشه‌ها" else "Folders")
                    )

                    items(filterItems) { (f, label) ->
                        val isSelected = filter == f
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setLibraryFilter(f) },
                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = YasSurfaceElevated,
                                selectedLabelColor = accentColor,
                                containerColor = YasSurfaceVariant,
                                labelColor = YasTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) accentColor else YasBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
        }

        // Compact Recently Played Carousel (does not dominate screen)
        if (recentlyPlayed.isNotEmpty() && (filter == LibraryFilter.ALL || filter == LibraryFilter.TRACKS) && searchQuery.isBlank()) {
            item {
                Column(modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)) {
                    Text(
                        text = if (isPersian) "اخیراً پخش شده" else "Recently Played",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = YasTextSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(recentlyPlayed.take(8)) { track ->
                            CompactRecentlyPlayedCard(
                                track = track,
                                isPlaying = track.id == currentTrack?.id,
                                onClick = { viewModel.playTrack(track) }
                            )
                        }
                    }
                }
            }
        }

        // Category Content
        when (filter) {
            LibraryFilter.ARTISTS -> {
                val artistsGroup = allTracks.groupBy { it.artist }.toList().sortedBy { it.first.lowercase() }
                items(artistsGroup) { (artistName, tracks) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openArtist(artistName) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(YasSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = YasTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = artistName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = YasTextPrimary
                            )
                            Text(
                                text = "${tracks.size} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextMuted
                            )
                        }
                    }
                }
            }

            LibraryFilter.ALBUMS -> {
                val albumsGroup = allTracks.groupBy { it.album }.toList().sortedBy { it.first.lowercase() }
                items(albumsGroup) { (albumName, tracks) ->
                    val sample = tracks.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openAlbum(albumName) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(YasSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (sample?.artworkResId != null) {
                                Image(
                                    painter = painterResource(id = sample.artworkResId),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp)
                                )
                            } else {
                                Icon(Icons.Default.Album, contentDescription = null, tint = YasTextMuted)
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = albumName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = YasTextPrimary
                            )
                            Text(
                                text = "${sample?.artist ?: "Unknown"} • ${tracks.size} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextMuted
                            )
                        }
                    }
                }
            }

            LibraryFilter.FOLDERS -> {
                val folderGroups = allTracks.groupBy { it.folderName }.toList().sortedBy { it.first.lowercase() }
                items(folderGroups) { (folderName, tracks) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.openFolder(folderName) }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(YasSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = YasTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = folderName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = YasTextPrimary
                            )
                            Text(
                                text = "${tracks.size} audio files",
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextMuted
                            )
                        }
                    }
                }
            }

            else -> {
                // ALL or TRACKS List Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isPersian) "تمام قطعات (${filteredTracks.size})" else "Tracks (${filteredTracks.size})",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = YasTextSecondary
                        )

                        Text(
                            text = sortMode.name.replace("_", " "),
                            style = MaterialTheme.typography.labelSmall,
                            color = YasTextMuted
                        )
                    }
                }

                if (filteredTracks.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = YasSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    tint = YasTextMuted,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty())
                                        "No tracks matched '$searchQuery'"
                                    else
                                        if (isPersian) "کتابخانه شما خالی است" else "Your library is empty",
                                    color = YasTextPrimary,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isPersian) "فایل‌های صوتی یا پوشه موسیقی خود را اضافه کنید" else "Add audio files or folders to start listening.",
                                    color = YasTextMuted,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                                        colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                    ) {
                                        Text(if (isPersian) "انتخاب فایل" else "Select Files", color = Color(0xFF101012))
                                    }
                                    OutlinedButton(
                                        onClick = { folderPickerLauncher.launch(null) }
                                    ) {
                                        Text(if (isPersian) "انتخاب پوشه" else "Select Folder", color = YasTextPrimary)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredTracks, key = { it.id }) { track ->
                        TrackItem(
                            track = track,
                            isPlaying = track.id == currentTrack?.id,
                            onClick = { viewModel.playTrack(track) },
                            onToggleFavorite = { viewModel.toggleFavorite(track) },
                            onAddToPlaylist = { viewModel.showAddToPlaylistDialog(track) },
                            onShowDiagnostics = { viewModel.showDiagnostic(track) },
                            onEditLyrics = { viewModel.showLyricsEditor(true) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubScreenDetail(
    title: String,
    subtitle: String,
    tracks: List<Track>,
    currentTrack: Track?,
    onBack: () -> Unit,
    viewModel: MainViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = YasTextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = YasTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$subtitle • ${tracks.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

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

@Composable
private fun CompactRecentlyPlayedCard(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Surface(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = YasSurfaceVariant,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPlaying) accentColor else YasBorder)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (track.artworkResId != null) {
                    Image(
                        painter = painterResource(id = track.artworkResId),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp)
                    )
                } else if (!track.artworkUri.isNullOrBlank()) {
                    AsyncImage(
                        model = track.artworkUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(88.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = YasTextMuted,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = track.title,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                color = if (isPlaying) accentColor else YasTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = YasTextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
