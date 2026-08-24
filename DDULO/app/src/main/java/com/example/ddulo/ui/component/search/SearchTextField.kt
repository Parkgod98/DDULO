package com.example.ddulo.ui.component.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 단일 TextField UI
 */
@Composable
fun SearchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isConfirmed: Boolean,
    modifier: Modifier = Modifier
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontWeight = if (isConfirmed) FontWeight.Bold else FontWeight.Normal,
                color = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        trailingIcon = {
            if (isConfirmed) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Confirmed",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = TextFieldDefaults.colors(
            // 확정 여부에 따른 배경색 및 하단 바 색상 변경
            focusedContainerColor = if (isConfirmed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f) else Color.Transparent,
            unfocusedContainerColor = if (isConfirmed) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.05f) else Color.Transparent,
            focusedIndicatorColor = if (isConfirmed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            unfocusedIndicatorColor = if (isConfirmed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        modifier = modifier.fillMaxWidth()
    )
}