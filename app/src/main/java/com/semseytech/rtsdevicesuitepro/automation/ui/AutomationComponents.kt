package com.semseytech.rtsdevicesuitepro.automation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TimePicker12h(
    initialHour: Int,
    initialMinute: Int,
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedHour by remember { mutableStateOf(if (initialHour % 12 == 0) 12 else initialHour % 12) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }
    var isAm by remember { mutableStateOf(initialHour < 12) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time", color = Color.White) },
        containerColor = Color(0xFF252525),
        text = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                WheelPicker(
                    value = selectedHour,
                    range = 1..12,
                    onValueChange = { selectedHour = it }
                )
                Text(":", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(horizontal = 8.dp))
                WheelPicker(
                    value = selectedMinute,
                    range = 0..59,
                    format = { String.format("%02d", it) },
                    onValueChange = { selectedMinute = it }
                )
                Spacer(modifier = Modifier.width(16.dp))
                WheelPicker(
                    value = if (isAm) 0 else 1,
                    range = 0..1,
                    format = { if (it == 0) "AM" else "PM" },
                    onValueChange = { isAm = it == 0 }
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val hour24 = when {
                    isAm && selectedHour == 12 -> 0
                    !isAm && selectedHour != 12 -> selectedHour + 12
                    else -> selectedHour
                }
                onTimeSelected(hour24, selectedMinute)
            }) {
                Text("Set Time")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    format: (Int) -> String = { it.toString() },
    onValueChange: (Int) -> Unit
) {
    val items = range.toList()
    val currentIndex = items.indexOf(value)
    
    Column(
        modifier = Modifier.height(150.dp).width(60.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(onClick = { 
            val nextIndex = (currentIndex - 1 + items.size) % items.size
            onValueChange(items[nextIndex])
        }) {
            Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.Gray)
        }
        
        Box(
            modifier = Modifier.height(50.dp).fillMaxWidth().background(Color(0xFF333333), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(format(value), color = Color(0xFF00E5FF), fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        IconButton(onClick = { 
            val nextIndex = (currentIndex + 1) % items.size
            onValueChange(items[nextIndex])
        }) {
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray)
        }
    }
}

@Composable
fun DayOfWeekPicker(
    selectedDays: List<Int>,
    onDaysSelected: (List<Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val currentSelected = remember { mutableStateListOf<Int>().apply { addAll(selectedDays) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Days", color = Color.White) },
        containerColor = Color(0xFF252525),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                days.forEachIndexed { index, day ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (currentSelected.contains(index)) currentSelected.remove(index)
                            else currentSelected.add(index)
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = currentSelected.contains(index),
                            onCheckedChange = {
                                if (it) currentSelected.add(index)
                                else currentSelected.remove(index)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF00E5FF))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(day, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onDaysSelected(currentSelected.toList()) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
