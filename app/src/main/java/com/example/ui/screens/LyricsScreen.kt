package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LyricsParser
import com.example.ui.theme.YasBackground
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun LyricsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTrack by viewModel.currentTrackFull.collectAsState()
    val lyricsMode by viewModel.currentLyricsMode.collectAsState()
    val syncedLyrics by viewModel.activeLyrics.collectAsState()
    val plainLyrics by viewModel.activePlainLyrics.collectAsState()
    val activeIndex by viewModel.activeLyricIndex.collectAsState()
    val offsetMs by viewModel.lyricsOffsetMs.collectAsState()
    val isPersianApp by viewModel.isPersianLanguage.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    var showPersianTranslation by remember { mutableStateOf(true) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val syncedListState = rememberLazyListState()
    val plainScrollState = rememberScrollState()

    // File launcher for user-selected .lrc or .txt files
    val lyricsFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "lyrics"
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            fileName = cursor.getString(nameIndex) ?: fileName
                        }
                    }
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader().use { it.readText() }
                    val isLrc = fileName.endsWith(".lrc", ignoreCase = true) || LyricsParser.hasLrcTimestamps(content)
                    viewModel.importLyricsContent(content, fileName)
                    val modeLabel = if (isLrc) "Synchronized (LRC)" else "Plain Text"
                    Toast.makeText(context, "Loaded lyrics ($modeLabel) from $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read lyrics file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Auto-scroll to active lyric line in Synced mode
    LaunchedEffect(activeIndex, lyricsMode) {
        if (lyricsMode == "SYNCED" && activeIndex >= 0 && activeIndex < syncedLyrics.size) {
            val scrollTarget = (activeIndex - 2).coerceAtLeast(0)
            syncedListState.animateScrollToItem(scrollTarget)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("lyrics_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPersianApp) "متن ترانه" else "Lyrics",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = YasTextPrimary
                )
                if (currentTrack != null) {
                    Text(
                        text = "${currentTrack!!.title} — ${currentTrack!!.artist}",
                        style = MaterialTheme.typography.bodySmall,
                        color = YasTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Online LRCLIB search button
                IconButton(
                    onClick = { viewModel.showLrclibDialog(true) },
                    modifier = Modifier.testTag("search_lrclib_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cloud,
                        contentDescription = "Search LRCLIB",
                        tint = accentColor
                    )
                }

                // Import lyrics file (.lrc or .txt)
                IconButton(
                    onClick = { lyricsFilePicker.launch(arrayOf("text/*", "application/octet-stream", "*/*")) },
                    modifier = Modifier.testTag("import_lrc_file_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = "Load Lyrics File",
                        tint = YasTextSecondary
                    )
                }

                // Edit lyrics button
                IconButton(
                    onClick = { viewModel.showLyricsEditor(true) },
                    modifier = Modifier.testTag("edit_lyrics_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Lyrics",
                        tint = YasTextSecondary
                    )
                }

                // Persian translation toggle (relevant for synced lyrics with translation)
                if (lyricsMode == "SYNCED") {
                    IconButton(
                        onClick = { showPersianTranslation = !showPersianTranslation },
                        modifier = Modifier.testTag("toggle_translation_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = "Toggle Translation",
                            tint = if (showPersianTranslation) accentColor else YasTextMuted
                        )
                    }
                }

                // Clear lyrics action
                if (currentTrack?.lyrics != null || currentTrack?.plainLyrics != null) {
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.testTag("clear_lyrics_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Clear Lyrics",
                            tint = YasTextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Mode Switcher Segmented Tabs: Synchronized vs Plain
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = YasSurfaceVariant,
            border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val isSynced = lyricsMode == "SYNCED"
                val isPlain = lyricsMode == "PLAIN"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isSynced) accentColor else Color.Transparent)
                        .clickable { viewModel.setLyricsMode("SYNCED") }
                        .padding(vertical = 8.dp)
                        .testTag("lyrics_mode_synced"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lyrics,
                            contentDescription = null,
                            tint = if (isSynced) Color.Black else YasTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersianApp) "هماهنگ (LRC)" else "Synchronized",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSynced) Color.Black else YasTextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(if (isPlain) accentColor else Color.Transparent)
                        .clickable { viewModel.setLyricsMode("PLAIN") }
                        .padding(vertical = 8.dp)
                        .testTag("lyrics_mode_plain"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = if (isPlain) Color.Black else YasTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPersianApp) "متن ساده" else "Plain Text",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPlain) Color.Black else YasTextSecondary
                        )
                    }
                }
            }
        }

        // Timing Offset Stepper Row (Visible in Synced Mode when lyrics exist)
        if (lyricsMode == "SYNCED" && syncedLyrics.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = YasSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "-0.5s",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = YasTextSecondary,
                            modifier = Modifier
                                .clickable { viewModel.adjustLyricsOffset(-500L) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Text(
                            text = "Offset: ${if (offsetMs >= 0) "+" else ""}${String.format("%.1f", offsetMs / 1000f)}s",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (offsetMs != 0L) accentColor else YasTextMuted,
                            modifier = Modifier
                                .clickable { viewModel.resetLyricsOffset() }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        )

                        Text(
                            text = "+0.5s",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = YasTextSecondary,
                            modifier = Modifier
                                .clickable { viewModel.adjustLyricsOffset(500L) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (currentTrack == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPersianApp) "هیچ قطعه‌ای در حال پخش نیست" else "No track currently playing",
                    color = YasTextMuted
                )
            }
        } else if (lyricsMode == "PLAIN") {
            // PLAIN LYRICS VIEW
            if (plainLyrics.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = null,
                                tint = YasTextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isPersianApp) "متن ساده‌ای یافت نشد" else "No Plain Lyrics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = YasTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isPersianApp)
                                    "می‌توانید متن ساده تایپ کنید، فایل .txt بارگذاری نمایید یا از LRCLIB دریافت کنید."
                                else
                                    "Enter plain lyrics without timestamps, load a .txt file, or search LRCLIB.",
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.showLyricsEditor(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPersianApp) "ورود یا الصاق متن ساده" else "Type or Paste Plain Lyrics", color = Color(0xFF101012))
                                }

                                OutlinedButton(
                                    onClick = { lyricsFilePicker.launch(arrayOf("text/*", "*/*")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPersianApp) "انتخاب فایل متنی (.txt / .lrc)" else "Load File (.txt / .lrc)", color = YasTextPrimary)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.showLrclibDialog(true) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPersianApp) "جستجو در LRCLIB" else "Search LRCLIB Online", color = YasTextPrimary)
                                }

                                if (!currentTrack!!.lyrics.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { viewModel.setLyricsMode("SYNCED") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isPersianApp) "مشاهده متن هماهنگ موجود" else "Switch to Synchronized Lyrics", color = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Display plain lyrics with vertical scroll and selectable text
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .verticalScroll(plainScrollState)
                        .padding(horizontal = 8.dp, vertical = 16.dp)
                        .testTag("plain_lyrics_content")
                ) {
                    SelectionContainer {
                        Text(
                            text = plainLyrics!!,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = YasTextPrimary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        } else {
            // SYNCHRONIZED LYRICS VIEW
            if (syncedLyrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .clip(RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                        border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = null,
                                tint = YasTextMuted,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isPersianApp) "متن هماهنگ یافت نشد" else "No Synchronized Lyrics",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = YasTextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isPersianApp)
                                    "می‌توانید به صورت آنلاین در LRCLIB جستجو کنید، فایل .lrc بارگذاری نمایید یا از حالت متن ساده استفاده کنید."
                                else
                                    "Search LRCLIB online, load a .lrc file, or switch to Plain Lyrics mode.",
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { viewModel.showLrclibDialog(true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPersianApp) "جستجو در LRCLIB" else "Search LRCLIB Online", color = Color(0xFF101012))
                                }

                                OutlinedButton(
                                    onClick = { lyricsFilePicker.launch(arrayOf("text/*", "*/*")) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPersianApp) "انتخاب فایل .lrc" else "Load .lrc File", color = YasTextPrimary)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.showLyricsEditor(true) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isPersianApp) "ورود دستی متن ترانه" else "Enter Lyrics Manually", color = YasTextPrimary)
                                }

                                if (!currentTrack!!.plainLyrics.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { viewModel.setLyricsMode("PLAIN") },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(if (isPersianApp) "مشاهده متن ساده موجود" else "Switch to Plain Lyrics", color = accentColor)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = syncedListState,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("synced_lyrics_content"),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 140.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    itemsIndexed(syncedLyrics) { index, line ->
                        val isActive = index == activeIndex

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.seekTo((line.timeMs + offsetMs).coerceAtLeast(0L)) }
                                .padding(vertical = 2.dp),
                            color = if (isActive) YasSurfaceElevated else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalAlignment = if (isPersianApp) Alignment.End else Alignment.Start
                            ) {
                                // Primary lyric line
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = if (isActive) 19.sp else 16.sp
                                    ),
                                    color = if (isActive) accentColor else YasTextSecondary,
                                    textAlign = if (isPersianApp) TextAlign.Right else TextAlign.Left
                                )

                                // Persian Translation Line
                                if (showPersianTranslation && !line.persianText.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = line.persianText,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = if (isActive) 14.sp else 13.sp,
                                            fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                                        ),
                                        color = if (isActive) YasTextPrimary else YasTextMuted,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog for clearing lyrics
    if (showClearConfirmDialog && currentTrack != null) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = {
                Text(
                    text = if (isPersianApp) "حذف متن ترانه" else "Clear Lyrics",
                    color = YasTextPrimary
                )
            },
            text = {
                Text(
                    text = if (isPersianApp)
                        "آیا از حذف متن ترانه برای قطعه «${currentTrack!!.title}» اطمینان دارید؟"
                    else
                        "Are you sure you want to clear the saved lyrics for \"${currentTrack!!.title}\"?",
                    color = YasTextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearCurrentTrackLyrics(clearSynced = true, clearPlain = true)
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Lyrics cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (isPersianApp) "حذف" else "Clear", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text(if (isPersianApp) "انصراف" else "Cancel", color = YasTextPrimary)
                }
            },
            containerColor = YasSurfaceElevated
        )
    }
}
