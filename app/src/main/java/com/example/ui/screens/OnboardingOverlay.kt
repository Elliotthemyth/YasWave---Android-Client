package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YasBackground
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary

@Composable
fun OnboardingOverlay(
    onDismiss: () -> Unit,
    isPersian: Boolean
) {
    var step by remember { mutableIntStateOf(0) }

    val pages = listOf(
        Triple(
            Icons.Default.GraphicEq,
            if (isPersian) "تجربه ناب موسیقی در اندروید" else "Android-First Audio Experience",
            if (isPersian) "طراحی بومی با سطوح تاریک و کنترل‌های دقیق، ویژوالایزر فرکانسی واقعی و مدیریت صوتی پیشرفته." else "Clean native architecture with real-time spectrum visualizer, responsive touch controls, and restrained dark charcoal aesthetic."
        ),
        Triple(
            Icons.Default.Lyrics,
            if (isPersian) "متن هماهنگ ترانه و ترجمه فارسی" else "Live Synced Lyrics & Translation",
            if (isPersian) "پیمایش خودکار خط به خط متن ترانه‌ها، جستجوی آنلاین LRCLIB، تنظیم تأخیر زمانی و ترجمه فارسی." else "Karaoke-style synchronized LRC scrolling, online LRCLIB search, millisecond timing offset adjustments, and bilingual Persian translations."
        ),
        Triple(
            Icons.Default.MusicNote,
            if (isPersian) "اصلاح هوشمند متادیتا و واردسازی فایل" else "Smart Metadata & Local Import",
            if (isPersian) "رفع خطای نمایش اعداد به جای نام خواننده، حفظ تگ‌های معتبر فایل و واردسازی آسان فایل‌ها و پوشه‌ها." else "Safe repair for numeric artist tag artifacts, respectful tag preservation, and seamless file or folder library import."
        )
    )

    val current = pages[step]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(YasBackground.copy(alpha = 0.94f))
            .padding(24.dp)
            .testTag("onboarding_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.dp,
                    color = YasBorder,
                    shape = RoundedCornerShape(24.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = YasSurfaceElevated)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Accent Icon Badge
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(YasSurfaceVariant)
                        .border(1.dp, YasBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = current.first,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = current.second,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = YasTextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = current.third,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                    color = YasTextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Page indicator dots
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (i in pages.indices) {
                        Box(
                            modifier = Modifier
                                .size(if (i == step) 20.dp else 8.dp, 6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (i == step) MaterialTheme.colorScheme.primary
                                    else YasBorder
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isPersian) "رد کردن" else "Skip", color = YasTextMuted)
                    }

                    Button(
                        onClick = {
                            if (step < pages.size - 1) {
                                step++
                            } else {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color(0xFF101012)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("onboarding_next_btn")
                    ) {
                        Text(
                            text = if (step == pages.size - 1) {
                                if (isPersian) "شروع کار با یس‌ویو" else "Get Started"
                            } else {
                                if (isPersian) "بعدی" else "Next"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
