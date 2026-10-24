package com.example.ui.dialogs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.theme.YasBorder
import com.example.ui.theme.YasSurfaceElevated
import com.example.ui.theme.YasSurfaceVariant
import com.example.ui.theme.YasTextPrimary
import com.example.ui.theme.YasTextSecondary

@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String) -> Unit,
    isPersian: Boolean
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val accentColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isPersian) "ایجاد پلی‌لیست جدید" else "New Playlist",
                color = YasTextPrimary
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isPersian) "نام پلی‌لیست" else "Playlist Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playlist_name_input"),
                    shape = RoundedCornerShape(8.dp),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = YasTextPrimary,
                        unfocusedTextColor = YasTextPrimary,
                        focusedIndicatorColor = accentColor,
                        unfocusedIndicatorColor = YasBorder,
                        focusedContainerColor = YasSurfaceVariant,
                        unfocusedContainerColor = YasSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(if (isPersian) "توضیحات (اختیاری)" else "Description (optional)") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
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
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name.trim(), description.trim())
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("create_playlist_confirm")
            ) {
                Text(if (isPersian) "ایجاد" else "Create", color = Color(0xFF101012))
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
