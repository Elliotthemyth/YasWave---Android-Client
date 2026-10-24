package com.example.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.YasBackground
import com.example.ui.theme.YasSurface
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextMuted
import com.example.ui.theme.YasTextPrimary
import com.example.viewmodel.NavigationTab

@Composable
fun BottomNavBar(
    currentTab: NavigationTab,
    onTabSelected: (NavigationTab) -> Unit,
    isPersian: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = MaterialTheme.colorScheme.primary

    NavigationBar(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar"),
        containerColor = YasSurface,
        tonalElevation = 2.dp
    ) {
        val items = listOf(
            Triple(NavigationTab.LIBRARY, if (isPersian) "کتابخانه" else "Library", Icons.Default.LibraryMusic),
            Triple(NavigationTab.PLAYLISTS, if (isPersian) "پلی‌لیست" else "Playlists", Icons.Default.PlaylistPlay),
            Triple(NavigationTab.LYRICS, if (isPersian) "متن ترانه" else "Lyrics", Icons.Default.Lyrics),
            Triple(NavigationTab.VISUALIZER, if (isPersian) "اکولایزر" else "Visualizer", Icons.Default.GraphicEq),
            Triple(NavigationTab.SETTINGS, if (isPersian) "تنظیمات" else "Settings", Icons.Default.Settings)
        )

        items.forEach { (tab, label, icon) ->
            val isSelected = currentTab == tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFF101012),
                    selectedTextColor = YasTextPrimary,
                    indicatorColor = accentColor,
                    unselectedIconColor = YasTextMuted,
                    unselectedTextColor = YasTextMuted
                ),
                modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
            )
        }
    }
}
