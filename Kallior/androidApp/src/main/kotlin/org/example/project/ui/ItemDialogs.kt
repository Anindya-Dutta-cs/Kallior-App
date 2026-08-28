package org.example.project.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetDefaults
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import android.os.Build
import kallos.model.Category
import org.example.project.R

/** Drawable resource for each task category. */
internal fun Category.iconRes(): Int = when (this) {
    Category.Exercise -> R.drawable.exercise
    Category.Work -> R.drawable.work
    Category.Meditation -> R.drawable.meditation
    Category.Diet -> R.drawable.diet
    Category.Other -> R.drawable.other
}

// ── Colors: Solid #212529 for sheet background ──────────────────────────────
private val SheetDark = Color(0xFF000000)
private val CheckmarkOrange = Color(0xFFFB5607)
private val CrossBlack = Color(0xFF000000)

private val LightGrey = Color(0xFF737373)

// ── Stroke shine modifier for circular action buttons ──────────────────────
private fun Modifier.strokeShine(
    glowColor: Color = LightGrey,
): Modifier = this.drawWithCache {
    val strokeWidth = 0.4.dp.toPx()
    val radius = (size.minDimension / 2f) - strokeWidth / 2f
    val shineBrush = Brush.sweepGradient(
        0.00f to glowColor.copy(alpha = 0.1f),
        0.12f to glowColor.copy(alpha = 0.75f),
        0.24f to glowColor.copy(alpha = 0.3f),
        0.36f to glowColor.copy(alpha = 0.15f),
        0.48f to glowColor.copy(alpha = 0.8f),
        0.60f to glowColor.copy(alpha = 0.2f),
    )
    onDrawBehind {
        drawCircle(
            brush = shineBrush,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth),
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ADD TASK BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: Category, description: String, customTitle: String?) -> Unit,
) {
    var descriptionField by remember { mutableStateOf(TextFieldValue("")) }
    var category by remember { mutableStateOf(Category.Exercise) }
    var title by remember { mutableStateOf(Category.Exercise.displayName) }
    var showError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 15.3.dp, topEnd = 15.3.dp),
        containerColor = SheetDark,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
            isAppearanceLightStatusBars = false,
            isAppearanceLightNavigationBars = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.navigationBarColor = android.graphics.Color.BLACK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SheetDark)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Header: Cross button · Title · Checkmark button ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Cross button (#000000 with stroke shine)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CrossBlack)
                            .strokeShine(Color.White.copy(alpha = 0.6f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Text(
                        text = "Create New Task",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Philosopher,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        ),
                        color = Color.White,
                    )

                    // Checkmark button (#FB5607 with stroke shine)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CheckmarkOrange)
                            .strokeShine(Color.White)
                            .clickable {
                                if (category == Category.Other && title.isBlank()) {
                                    showError = true
                                } else {
                                    val customTitle = if (category == Category.Other) {
                                        title.trim().takeIf { it.isNotBlank() }
                                    } else null
                                    onConfirm(category, descriptionField.text, customTitle)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Category Section ──
                Text(
                    text = "Category:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Category Circle Chips inside horizontal scroll
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                        )
                        .padding(vertical = 16.dp, horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Category.entries.forEach { cat ->
                            CategoryCircleChip(
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

                if (category == Category.Other) {
                    Spacer(modifier = Modifier.height(16.dp))
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

                Spacer(modifier = Modifier.height(28.dp))

                // ── Description Section ──
                Text(
                    text = "Description:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Formatting toolbar
                FormattingToolbar(
                    textFieldValue = descriptionField,
                    onValueChange = { descriptionField = it },
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Description Input — Unlimited characters with auto-bullet & auto-number continuation
                OutlinedTextField(
                    value = descriptionField,
                    onValueChange = { newValue ->
                        descriptionField = handleDescriptionValueChange(descriptionField, newValue)
                    },
                    placeholder = {
                        Text(
                            text = "Add task details...",
                            color = KalliorColors.MutedText.copy(alpha = 0.5f),
                        )
                    },
                    singleLine = false,
                    minLines = 6,
                    colors = dialogFieldColors().copy(
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
                        focusedIndicatorColor = CheckmarkOrange.copy(alpha = 0.8f),
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  ADD REMINDER BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, frequency: Long) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var descriptionField by remember { mutableStateOf(TextFieldValue("")) }
    var showError by remember { mutableStateOf(false) }
    var selectedFrequency by remember { mutableLongStateOf(0L) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 15.3.dp, topEnd = 15.3.dp),
        containerColor = SheetDark,
        dragHandle = null,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true,
            shouldDismissOnClickOutside = true,
            isAppearanceLightStatusBars = false,
            isAppearanceLightNavigationBars = false
        )
    ) {
        val view = LocalView.current
        SideEffect {
            val window = (view.parent as? DialogWindowProvider)?.window
            if (window != null) {
                window.navigationBarColor = android.graphics.Color.BLACK
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SheetDark)
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── Header: Cross button · Title · Checkmark button ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CrossBlack)
                            .strokeShine(Color.White.copy(alpha = 0.6f))
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    Text(
                        text = "Create New Reminder",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = Philosopher,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        ),
                        color = Color.White,
                    )

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(CheckmarkOrange)
                            .strokeShine(Color.White)
                            .clickable {
                                if (title.isBlank()) {
                                    showError = true
                                } else {
                                    onConfirm(
                                        title.trim(),
                                        descriptionField.text.ifBlank { null },
                                        selectedFrequency,
                                    )
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Confirm",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Reminder Name ──
                Text(
                    text = "Reminder Name:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; showError = false },
                    placeholder = { Text("Name", color = KalliorColors.MutedText.copy(alpha = 0.5f)) },
                    singleLine = true,
                    isError = showError,
                    colors = dialogFieldColors(),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (showError) {
                    Text(
                        text = "Reminder name is required",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ── Frequency ──
                Text(
                    text = "Frequency:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FrequencySelector(
                    selectedFrequency = selectedFrequency,
                    onFrequencySelected = { selectedFrequency = it },
                )

                Spacer(modifier = Modifier.height(20.dp))

                // ── Description ──
                Text(
                    text = "Description:",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = Philosopher,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    ),
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(12.dp))

                FormattingToolbar(
                    textFieldValue = descriptionField,
                    onValueChange = { descriptionField = it },
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = descriptionField,
                    onValueChange = { newValue ->
                        descriptionField = handleDescriptionValueChange(descriptionField, newValue)
                    },
                    placeholder = {
                        Text(
                            text = "Add reminder details...",
                            color = KalliorColors.MutedText.copy(alpha = 0.5f),
                        )
                    },
                    singleLine = false,
                    minLines = 4,
                    colors = dialogFieldColors().copy(
                        unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
                        focusedIndicatorColor = CheckmarkOrange.copy(alpha = 0.8f),
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  CATEGORY CIRCLE CHIP  (icon inside circle + label below)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun CategoryCircleChip(cat: Category, selected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
        label = "chipBorder",
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) Color.White else KalliorColors.MutedText,
        label = "chipIcon",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .border(width = 1.5.dp, color = borderColor, shape = CircleShape)
                .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(cat.iconRes()),
                contentDescription = cat.displayName,
                colorFilter = ColorFilter.tint(iconTint),
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = cat.displayName,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = Philosopher,
                fontSize = 13.sp,
            ),
            color = if (selected) Color.White else KalliorColors.MutedText,
            textAlign = TextAlign.Center,
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  FORMATTING TOOLBAR  (bold · italic · bullet · numbered)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun FormattingToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FormatButton(icon = Icons.Default.FormatBold, description = "Bold") {
            wrapSelection(textFieldValue, onValueChange, "**", "**")
        }
        FormatButton(icon = Icons.Default.FormatItalic, description = "Italic") {
            wrapSelection(textFieldValue, onValueChange, "_", "_")
        }
        FormatButton(icon = Icons.Default.FormatListBulleted, description = "Bullet") {
            insertLinePrefix(textFieldValue, onValueChange, "• ")
        }
        FormatButton(icon = Icons.Default.FormatListNumbered, description = "Numbered") {
            insertNumberedPrefix(textFieldValue, onValueChange)
        }
    }
}

@Composable
private fun FormatButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun wrapSelection(
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: String,
    suffix: String,
) {
    val text = tfv.text
    val sel = tfv.selection
    if (sel.collapsed) {
        val newText = text.substring(0, sel.start) + prefix + suffix + text.substring(sel.start)
        val cursor = sel.start + prefix.length
        onValueChange(TextFieldValue(newText, TextRange(cursor, cursor)))
    } else {
        val selected = text.substring(sel.start, sel.end)
        val newText = text.substring(0, sel.start) + prefix + selected + suffix + text.substring(sel.end)
        val newEnd = sel.start + prefix.length + selected.length
        onValueChange(TextFieldValue(newText, TextRange(sel.start + prefix.length, newEnd)))
    }
}

private fun insertLinePrefix(
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    prefix: String,
) {
    val text = tfv.text
    val cursor = tfv.selection.start
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    val newCursor = cursor + prefix.length
    onValueChange(TextFieldValue(newText, TextRange(newCursor, newCursor)))
}

private fun insertNumberedPrefix(
    tfv: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
) {
    val text = tfv.text
    val cursor = tfv.selection.start
    val lineStart = text.lastIndexOf('\n', cursor - 1) + 1
    val textBefore = text.substring(0, lineStart)
    val existingNumbers = Regex("""^(\d+)\. """).findAll(textBefore.replace("\r", ""))
        .toList()
    val nextNum = (existingNumbers.lastOrNull()?.groupValues?.get(1)?.toIntOrNull() ?: 0) + 1
    val prefix = "$nextNum. "
    val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
    val newCursor = cursor + prefix.length
    onValueChange(TextFieldValue(newText, TextRange(newCursor, newCursor)))
}

/** Handles automatic list continuation (bullets and numbers) when Enter is pressed. */
private fun handleDescriptionValueChange(
    oldValue: TextFieldValue,
    newValue: TextFieldValue,
): TextFieldValue {
    // Detect if user inserted a newline ('\n')
    if (newValue.text.length == oldValue.text.length + 1 &&
        newValue.selection.collapsed &&
        newValue.selection.start > 0 &&
        newValue.text[newValue.selection.start - 1] == '\n'
    ) {
        val cursor = newValue.selection.start
        val textBeforeCursor = newValue.text.substring(0, cursor - 1)
        val lastLineStart = textBeforeCursor.lastIndexOf('\n') + 1
        val lastLine = textBeforeCursor.substring(lastLineStart)

        // Bullet continuation
        if (lastLine.startsWith("• ")) {
            if (lastLine.trim() == "•") {
                // User pressed Enter on empty bullet line -> exit bullet mode
                val newText = newValue.text.substring(0, lastLineStart) + newValue.text.substring(cursor)
                return TextFieldValue(newText, TextRange(lastLineStart))
            }
            val prefix = "• "
            val newText = newValue.text.substring(0, cursor) + prefix + newValue.text.substring(cursor)
            return TextFieldValue(newText, TextRange(cursor + prefix.length))
        }

        // Numbered list continuation
        val numMatch = Regex("""^(\d+)\.\s""").find(lastLine)
        if (numMatch != null) {
            val currentNum = numMatch.groupValues[1].toIntOrNull() ?: 1
            if (lastLine.trim() == "$currentNum.") {
                // User pressed Enter on empty number line -> exit number mode
                val newText = newValue.text.substring(0, lastLineStart) + newValue.text.substring(cursor)
                return TextFieldValue(newText, TextRange(lastLineStart))
            }
            val nextNum = currentNum + 1
            val prefix = "$nextNum. "
            val newText = newValue.text.substring(0, cursor) + prefix + newValue.text.substring(cursor)
            return TextFieldValue(newText, TextRange(cursor + prefix.length))
        }
    }
    return newValue
}

/**
 * Parses markdown formatting (**bold** and _italic_ / *italic*) into an AnnotatedString,
 * completely hiding the formatting symbol characters (** and _) from output.
 */
fun parseFormattedText(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    val len = text.length
    while (i < len) {
        // Match **bold**
        if (i + 1 < len && text[i] == '*' && text[i + 1] == '*') {
            val end = text.indexOf("**", i + 2)
            if (end != -1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(text.substring(i + 2, end))
                pop()
                i = end + 2
                continue
            }
        }
        // Match _italic_ or *italic*
        if (text[i] == '_' || text[i] == '*') {
            val targetChar = text[i]
            val end = text.indexOf(targetChar, i + 1)
            if (end != -1 && end > i + 1) {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(text.substring(i + 1, end))
                pop()
                i = end + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SHARED UTILITIES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
internal fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CheckmarkOrange,
    unfocusedBorderColor = Color.White.copy(alpha = 0.25f),
    focusedTextColor = KalliorColors.NormalText,
    unfocusedTextColor = KalliorColors.NormalText,
    cursorColor = CheckmarkOrange,
    focusedLabelColor = CheckmarkOrange,
    unfocusedLabelColor = KalliorColors.MutedText,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySelector(
    selectedFrequency: Long,
    onFrequencySelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        0L to "Only once",
        60L to "Every 1 hour",
        90L to "Every 1.5 hours",
        120L to "Every 2 hours",
        180L to "Every 3 hours",
        360L to "Every 6 hours",
        720L to "Every 12 hours",
        1440L to "Every 24 hours",
    )

    var expanded by remember { mutableStateOf(false) }
    val selectedText = options.find { it.first == selectedFrequency }?.second ?: "Only once"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Frequency") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = dialogFieldColors(),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (value, text) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        onFrequencySelected(value)
                        expanded = false
                    },
                )
            }
        }
    }
}
