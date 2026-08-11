package org.example.project.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kallos.model.Category
import kotlin.time.Instant
import org.example.project.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Drawable resource for each task category, used by the chip selector. */
internal fun Category.iconRes(): Int = when (this) {
    Category.Exercise -> R.drawable.exercise
    Category.Work -> R.drawable.work
    Category.Meditation -> R.drawable.meditation
    Category.Diet -> R.drawable.diet
    Category.Other -> R.drawable.other
}

/**
 * Modal dialog for creating a new task. Follows the app's dark color scheme and
 * rounded card style. The category is picked from a horizontal, scrollable row of
 * chips (icon + label); the chosen category becomes the task title automatically,
 * unless "Other" is selected, in which case a custom title field is shown.
 * [onConfirm] is only invoked once a non-blank name is provided.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: Category, description: String, customTitle: String?) -> Unit,
) {
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.Exercise) }
    var title by remember { mutableStateOf(Category.Exercise.displayName) }
    var showError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = KalliorColors.PrimaryLayer,
        title = {
            Text(
                text = "New Task",
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Title:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KalliorColors.NormalText
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(scrollState),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Category.entries.forEach { cat ->
                                CategoryChip(
                                    cat = cat,
                                    selected = category == cat,
                                    onClick = {
                                        category = cat
                                        if (cat != Category.Other) {
                                            title = cat.displayName
                                            showError = false
                                        }
                                    },
                                )
                            }
                        }
                    }
                    
                    // Horizontal scroll bar indicator
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(KalliorColors.MutedText.copy(alpha = 0.1f))
                    ) {
                        // Simple scroll indicator
                        val scrollFraction = if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.3f)
                                .height(2.dp)
                                .background(KalliorColors.MutedText.copy(alpha = 0.4f))
                                .align(Alignment.CenterStart)
                                .padding(start = (scrollFraction * 70).dp) // basic heuristic
                        )
                    }
                }

                if (category == Category.Other) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; showError = false },
                        label = { Text("Custom Title") },
                        singleLine = true,
                        isError = showError,
                        colors = dialogFieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (showError) {
                        Text(
                            text = "Custom title is required",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Description:",
                        style = MaterialTheme.typography.bodyLarge,
                        color = KalliorColors.NormalText
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(50) },
                        placeholder = { 
                            Text(
                                text = "Optional", 
                                color = KalliorColors.MutedText.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            ) 
                        },
                        singleLine = false,
                        colors = dialogFieldColors().copy(
                            unfocusedIndicatorColor = KalliorColors.MutedText.copy(alpha = 0.3f),
                            focusedIndicatorColor = KalliorColors.AccentOrange.copy(alpha = 0.8f)
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text(
                                text = "${description.length}/50",
                                color = KalliorColors.MutedText,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(KalliorColors.AccentOrange)
                    .clickable {
                        if (category == Category.Other && title.isBlank()) {
                            showError = true
                        } else {
                            val customTitle = if (category == Category.Other) {
                                title.trim().takeIf { it.isNotBlank() }
                            } else {
                                null
                            }
                            onConfirm(category, description, customTitle)
                        }
                    }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Add",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = KalliorColors.MutedText)
            }
        },
    )
}

@Composable
private fun CategoryChip(cat: Category, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) Color.White else KalliorColors.MutedText.copy(alpha = 0.5f)
    val contentColor = if (selected) Color.White else KalliorColors.NormalText
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
            .background(if (selected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(
                painter = painterResource(cat.iconRes()),
                contentDescription = null,
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = cat.displayName,
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * Modal dialog for creating a new reminder. Collects a required name, an optional
 * description, and a date/time (via Material3 pickers) for when it should fire.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, frequency: Long) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableLongStateOf(0L) }


    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = KalliorColors.PrimaryLayer,
        title = { Text("New Reminder", color = KalliorColors.NormalText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    label = { Text("Reminder Name") },
                    singleLine = true,
                    isError = showError,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showError) {
                    Text(
                        text = "Reminder name is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = false,
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                FrequencySelector(
                    selectedFrequency = selectedFrequency,
                    onFrequencySelected = { selectedFrequency = it }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.isBlank()) {
                    showError = true
                    return@TextButton
                }
                onConfirm(title.trim(), description, selectedFrequency)
            }) {
                Text("Add", color = KalliorColors.AccentOrange)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = KalliorColors.MutedText)
            }
        },
    )
}

@Composable
internal fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = KalliorColors.AccentOrange,
    unfocusedBorderColor = KalliorColors.MutedText.copy(alpha = 0.4f),
    focusedTextColor = KalliorColors.NormalText,
    unfocusedTextColor = KalliorColors.NormalText,
    cursorColor = KalliorColors.AccentOrange,
    focusedLabelColor = KalliorColors.AccentOrange,
    unfocusedLabelColor = KalliorColors.MutedText,
)

private fun formatMillis(millis: Long, pattern: String): String =
    SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(
    selectedFrequency: Long,
    onFrequencySelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        0L to "Only once",
        60L to "Every 1 hour",
        90L to "Every 1.5 hours",
        120L to "Every 2 hours",
        180L to "Every 3 hours",
        360L to "Every 6 hours",
        720L to "Every 12 hours",
        1440L to "Every 24 hours"
    )

    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.find { it.first == selectedFrequency }?.second ?: "Only once"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Frequency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onFrequencySelected(value)
                        expanded = false
                    }
                )
            }
        }
    }
}
