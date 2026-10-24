package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Track
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary

@Composable
fun LyricsEditorDialog(
    track: Track,
    onDismiss: () -> Unit,
    onSave: (syncedLyrics: String?, persianLyrics: String?, plainLyrics: String?, targetMode: String) -> Unit,
    isPersian: Boolean
) {
    var selectedTab by remember { mutableStateOf(track.lyricsMode) } // "SYNCED" or "PLAIN"
    var syncedLyricsText by remember { mutableStateOf(track.lyrics ?: "") }
    var persianLyricsText by remember { mutableStateOf(track.persianLyrics ?: "") }
    var plainLyricsText by remember { mutableStateOf(track.plainLyrics ?: "") }
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "ویرایش متن ترانه" else "Edit Lyrics",
                color = YasTextPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Mode selector pills
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = YasSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTab == "SYNCED") accentColor else Color.Transparent)
                                .clickable { selectedTab = "SYNCED" }
                                .padding(vertical = 8.dp)
                                .testTag("editor_tab_synced"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPersian) "متن هماهنگ (LRC)" else "Synchronized (LRC)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedTab == "SYNCED") Color.Black else YasTextSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedTab == "PLAIN") accentColor else Color.Transparent)
                                .clickable { selectedTab = "PLAIN" }
                                .padding(vertical = 8.dp)
                                .testTag("editor_tab_plain"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPersian) "متن ساده" else "Plain Text",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (selectedTab == "PLAIN") Color.Black else YasTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == "SYNCED") {
                    Text(
                        text = if (isPersian)
                            "فرمت استاندارد: [دقیقه:ثانیه.صدم] متن ترانه"
                        else
                            "Format: [mm:ss.xx] Lyric text | Optional translation",
                        style = MaterialTheme.typography.bodySmall,
                        color = YasTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = syncedLyricsText,
                        onValueChange = { syncedLyricsText = it },
                        label = { Text(if (isPersian) "متن ترانه (LRC)" else "LRC Lyrics") },
                        placeholder = { Text("[00:05.00] Enter lyric lines...", color = YasTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("lyrics_input"),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = YasTextPrimary,
                            unfocusedTextColor = YasTextPrimary,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = YasBorder,
                            focusedContainerColor = YasSurfaceVariant,
                            unfocusedContainerColor = YasSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = persianLyricsText,
                        onValueChange = { persianLyricsText = it },
                        label = { Text(if (isPersian) "ترجمه فارسی مجزا (اختیاری)" else "Persian Translation (Optional)") },
                        placeholder = { Text("[00:05.00] متن ترجمه فارسی...", color = YasTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = YasTextPrimary,
                            unfocusedTextColor = YasTextPrimary,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = YasBorder,
                            focusedContainerColor = YasSurfaceVariant,
                            unfocusedContainerColor = YasSurfaceVariant
                        )
                    )
                } else {
                    Text(
                        text = if (isPersian)
                            "متن ساده بدون برچسب زمان‌بندی. شکست خطوط، خطوط خالی و نوشتار راست‌به‌چپ حفظ می‌شود."
                        else
                            "Plain text without timestamps. Line breaks, blank lines, and RTL text are preserved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = YasTextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = plainLyricsText,
                        onValueChange = { plainLyricsText = it },
                        label = { Text(if (isPersian) "متن ساده ترانه" else "Plain Lyrics") },
                        placeholder = { Text(if (isPersian) "متن ترانه را اینجا بنویسید یا الصاق کنید..." else "Type or paste lyrics here...", color = YasTextMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(248.dp)
                            .testTag("plain_lyrics_input"),
                        shape = RoundedCornerShape(8.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = YasTextPrimary,
                            unfocusedTextColor = YasTextPrimary,
                            focusedIndicatorColor = accentColor,
                            unfocusedIndicatorColor = YasBorder,
                            focusedContainerColor = YasSurfaceVariant,
                            unfocusedContainerColor = YasSurfaceVariant
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        syncedLyricsText.trim().ifBlank { null },
                        persianLyricsText.trim().ifBlank { null },
                        plainLyricsText.trim().ifBlank { null },
                        selectedTab
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_lyrics_confirm")
            ) {
                Text(if (isPersian) "ذخیره" else "Save", color = Color(0xFF101012))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(8.dp)) {
                Text(if (isPersian) "انصراف" else "Cancel", color = YasTextPrimary)
            }
        },
        containerColor = YasSurfaceElevated
    )
}
