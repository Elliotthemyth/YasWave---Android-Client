package com.example.ui.screens

import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentTheme
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isPersian by viewModel.isPersianLanguage.collectAsState()
    val allTracks by viewModel.repository.tracks.collectAsState()
    val playlists by viewModel.repository.playlists.collectAsState()
    val currentAccent by viewModel.accentTheme.collectAsState()
    val isHardwareActive by viewModel.playerEngine.isHardwareVisualizerActive.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Text(
                text = if (isPersian) "تنظیمات" else "Settings",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = YasTextPrimary,
                modifier = Modifier.padding(vertical = 10.dp)
            )

            // Section: Appearance & Accent
            SettingsSectionHeader(if (isPersian) "رنگ و ظاهر (Appearance)" else "Appearance & Accent")

            Card(
                colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ColorLens, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPersian) "تم و رنگ تک‌نواخت (Single Accent)" else "Restrained Accent Color",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = YasTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(AccentTheme.values()) { theme ->
                            val isSelected = currentAccent == theme
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.setAccentTheme(theme) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(theme.color)
                                    )
                                },
                                label = { Text(theme.displayName, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = YasSurfaceElevated,
                                    selectedLabelColor = theme.color,
                                    containerColor = Color.Transparent,
                                    labelColor = YasTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) theme.color else YasBorder
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Language & Localization
            SettingsSectionHeader(if (isPersian) "زبان و جهت متن" else "Language & Localization")

            Card(
                colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isPersian) "زبان فارسی (Persian RTL)" else "Persian Language (فارسی)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = YasTextPrimary
                            )
                            Text(
                                text = if (isPersian) "چیدمان راست‌به‌چپ و متن ترانه‌ها" else "Right-to-left layout & lyrics",
                                style = MaterialTheme.typography.bodySmall,
                                color = YasTextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isPersian,
                        onCheckedChange = { viewModel.togglePersianLanguage() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF101012),
                            checkedTrackColor = accentColor,
                            uncheckedThumbColor = YasTextMuted,
                            uncheckedTrackColor = YasBorder
                        ),
                        modifier = Modifier.testTag("persian_lang_toggle")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Metadata Reprocessing & Diagnostics
            SettingsSectionHeader(if (isPersian) "متادیتا و عیب‌یابی" else "Metadata & Diagnostics")

            Card(
                colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Metadata Processing Engine",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = YasTextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Resolves numeric artist bugs ('2', '3', '4') by falling back to AlbumArtist or unambiguous filename patterns.",
                        style = MaterialTheme.typography.bodySmall,
                        color = YasTextSecondary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.reprocessLibraryMetadata { count ->
                                    Toast.makeText(context, "Reprocessed library ($count tracks updated)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF101012), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isPersian) "پردازش مجدد" else "Reprocess Tags", color = Color(0xFF101012), fontSize = 12.sp)
                        }

                        val testTrack = allTracks.find { it.id == "demo_3" } ?: allTracks.firstOrNull()
                        if (testTrack != null) {
                            OutlinedButton(
                                onClick = { viewModel.showDiagnostic(testTrack) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = YasTextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inspector", color = YasTextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Audio Hardware & Permissions
            SettingsSectionHeader(if (isPersian) "سخت‌افزار صوت و مجوزها" else "Audio & Permissions")

            Card(
                colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hardware Visualizer:", style = MaterialTheme.typography.bodySmall, color = YasTextMuted)
                        Text(
                            text = if (isHardwareActive) "Active (RECORD_AUDIO granted)" else "Fallback Audio Engine",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (isHardwareActive) accentColor else YasTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Android restricts direct audio-session visualization to apps with RECORD_AUDIO permission. When disabled or on emulators, YASWAVE seamlessly uses a calibrated audio engine fallback.",
                        style = MaterialTheme.typography.bodySmall,
                        color = YasTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Section: Library & Reset
            SettingsSectionHeader(if (isPersian) "مدیریت حافظه" else "Library Storage")

            Card(
                colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isPersian) "تعداد کل آهنگ‌ها:" else "Total Tracks:", style = MaterialTheme.typography.bodySmall, color = YasTextMuted)
                        Text("${allTracks.size}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = YasTextPrimary)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(if (isPersian) "پلی‌لیست‌ها:" else "Playlists:", style = MaterialTheme.typography.bodySmall, color = YasTextMuted)
                        Text("${playlists.size}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = YasTextPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                viewModel.repository.resetToDemoLibrary()
                                Toast.makeText(context, "Reset library to default demo tracks", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isPersian) "بازنشانی دمو" else "Reset Demo", color = YasTextPrimary, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetOnboarding() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isPersian) "راهنمای کاربری" else "Show Guide", color = YasTextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Branding Footer
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "YASWAVE",
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = YasTextPrimary
                )
                Text(
                    text = "Version 2.1 (Restrained Dark Grayscale Edition)",
                    style = MaterialTheme.typography.bodySmall,
                    color = YasTextMuted
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp
        ),
        color = YasTextMuted,
        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
    )
}
