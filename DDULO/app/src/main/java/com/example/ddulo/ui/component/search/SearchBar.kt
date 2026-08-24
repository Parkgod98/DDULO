package com.example.ddulo.ui.component.search

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.example.ddulo.viewmodel.FocusedField

/**
 * 검색창 + swap 버튼
 */
@Composable
fun SearchBar(
    departure: String,
    destination: String,
    departureConfirmed: Boolean, // 추가: 출발지 확정 여부
    destinationConfirmed: Boolean, // 추가: 도착지 확정 여부
    onDepartureChange: (String) -> Unit,
    onDestinationChange: (String) -> Unit,
    onSwap: () -> Unit,
    onFocusChange: (FocusedField) -> Unit,
    modifier: Modifier = Modifier
) {
    var departureFocused by remember { mutableStateOf(false) }
    var destinationFocused by remember { mutableStateOf(false) }

    // Notify parent about focus changes
    LaunchedEffect(departureFocused, destinationFocused) {
        val field = when {
            departureFocused -> FocusedField.DEPARTURE
            destinationFocused -> FocusedField.DESTINATION
            else -> FocusedField.NONE
        }
        onFocusChange(field)
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onSwap) {
                Icon(
                    imageVector = Icons.Default.SwapVert,
                    contentDescription = "Swap",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 출발지 입력 필드
                SearchTextField(
                    value = departure,
                    onValueChange = onDepartureChange,
                    label = "출발지",
                    isConfirmed = departureConfirmed,
                    modifier = Modifier.onFocusChanged { departureFocused = it.isFocused }
                )

                // 도착지 입력 필드
                SearchTextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    label = "도착지",
                    isConfirmed = destinationConfirmed,
                    modifier = Modifier.onFocusChanged { destinationFocused = it.isFocused }
                )
            }
        }
    }
}
