package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.components.WaveVisualizer
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary
import com.example.viewmodel.MainViewModel

@Composable
fun VisualizerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val spectrumBands by viewModel.playerEngine.spectrumBands.collectAsState()
    val isPlaying by viewModel.playerEngine.isPlaying.collectAsState()
    val isHardwareActive by viewModel.playerEngine.isHardwareVisualizerActive.collectAsState()
    val activePreset by viewModel.playerEngine.activePreset.collectAsState()
    val availablePresets = viewModel.playerEngine.availablePresets
    val isPersian by viewModel.isPersianLanguage.collectAsState()
    val accentColor = MaterialTheme.colorScheme.primary

    var bassSliderVal by remember { mutableFloatStateOf(activePreset.bassBoost) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("visualizer_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isPersian) "اکولایزر و ویژوالایزر صوتی" else "Audio Visualizer & EQ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = YasTextPrimary
                    )
                }

                // Audio Capture Status Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = YasSurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder)
                ) {
                    Text(
                        text = if (isHardwareActive) "HAL Active" else "Engine Mode",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = if (isHardwareActive) accentColor else YasTextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Wave Visualizer Canvas Card (Clean charcoal container)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, YasBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    WaveVisualizer(
                        bands = spectrumBands,
                        height = 130.dp,
                        barSpacingRatio = 0.28f,
                        accentColor = accentColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("20 Hz", style = MaterialTheme.typography.labelSmall, color = YasTextMuted)
                        Text("1 kHz", style = MaterialTheme.typography.labelSmall, color = YasTextMuted)
                        Text("20 kHz", style = MaterialTheme.typography.labelSmall, color = YasTextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Equalizer Presets
            Text(
                text = if (isPersian) "پریست‌های اکولایزر" else "Sound Presets",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = YasTextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(availablePresets) { preset ->
                    val isSelected = activePreset.name == preset.name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            viewModel.selectEqualizerPreset(preset)
                            bassSliderVal = preset.bassBoost
                        },
                        label = {
                            Text(
                                text = preset.name,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
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

            Spacer(modifier = Modifier.height(16.dp))

            // Bass Boost Slider
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
                        Text(
                            text = if (isPersian) "تقویت فرکانس‌های بم (Bass Boost)" else "Bass Harmonic Boost",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = YasTextPrimary
                        )
                        Text(
                            text = "${(bassSliderVal * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = accentColor
                        )
                    }

                    Slider(
                        value = bassSliderVal,
                        onValueChange = {
                            bassSliderVal = it
                            val updated = activePreset.copy(bassBoost = it)
                            viewModel.selectEqualizerPreset(updated)
                        },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = accentColor,
                            activeTrackColor = accentColor,
                            inactiveTrackColor = YasBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Active Equalizer Band Curves
            Card(
                colors = CardDefaults.cardColors(containerColor = YasSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, YasBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (isPersian) "منحنی فرکانسی فعال" else "Active Equalizer Curves",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = YasTextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val bandLabels = listOf("60 Hz", "230 Hz", "910 Hz", "3.6 kHz", "14 kHz")
                    activePreset.bands.forEachIndexed { i, gain ->
                        val label = bandLabels.getOrElse(i) { "Band $i" }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodySmall, color = YasTextMuted)
                            Text(
                                text = "${if (gain >= 0) "+" else ""}${String.format("%.1f", gain * 12)} dB",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = if (gain != 0f) accentColor else YasTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
