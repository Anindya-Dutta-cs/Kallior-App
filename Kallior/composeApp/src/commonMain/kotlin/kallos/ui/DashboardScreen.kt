package kallos.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kallos.model.Category
import kallos.ui.theme.KalliorMuted
import kallos.ui.theme.KalliorOnSurface
import kallos.ui.theme.KalliorSurface
import kallos.ui.theme.KalliorSurfaceVariant
import kallos.ui.theme.KalliorTertiary
import kallos.viewmodel.GameViewModel

/**
 * The main dashboard content pane.
 *
 * Contains an empty "Dashboard" hero card, the Tasks section
 * (with inline add form), and a Badges placeholder section.
 */
@Composable
fun DashboardScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(KalliorSurface)
            .padding(24.dp),
    ) {
        // ── Dashboard header row ─────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.displayMedium,
                color = KalliorOnSurface,
            )

            // Small dark pill (decorative, matching mockup)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(KalliorSurfaceVariant)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) { /* empty placeholder */ }
        }

        Spacer(Modifier.height(16.dp))

        // ── Dashboard hero card (empty) ──────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(KalliorSurfaceVariant),
        ) { /* intentionally empty per requirements */ }

        Spacer(Modifier.height(24.dp))

        // ── Tasks + Badges row ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // ── Tasks section ────────────────────────────────
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                TaskSectionHeader(viewModel)

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KalliorSurfaceVariant)
                        .padding(16.dp),
                ) {
                    if (viewModel.tasks.isEmpty()) {
                        // ── Empty state ──────────────────────
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Click + to add a new task",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KalliorMuted,
                            )
                        }
                    } else {
                        // ── Task list ────────────────────────
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(
                                items = viewModel.tasks,
                                key = { it.id },
                            ) { task ->
                                TaskCard(
                                    task = task,
                                    onDelete = { viewModel.deleteTask(task.id) },
                                )
                            }
                        }
                    }
                }
            }

            // ── Badges section ───────────────────────────────
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Badges",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KalliorOnSurface,
                    )
                    Button(
                        onClick = { /* TODO */ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KalliorTertiary,
                            contentColor = KalliorSurface,
                        ),
                        shape = RoundedCornerShape(20.dp),
                    ) {
                        Text("Show All >", style = MaterialTheme.typography.labelLarge)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(16.dp))
                        .background(KalliorSurfaceVariant)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Wow, such empty!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KalliorMuted,
                    )
                }
            }
        }
    }
}

// ── Tasks section header with inline add form ────────────────

@Composable
private fun TaskSectionHeader(viewModel: GameViewModel) {
    var showForm by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(Category.Other) }
    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // ── Title row ────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Tasks",
            style = MaterialTheme.typography.headlineMedium,
            color = KalliorOnSurface,
        )
        Button(
            onClick = { showForm = !showForm },
            colors = ButtonDefaults.buttonColors(
                containerColor = KalliorTertiary,
                contentColor = KalliorSurface,
            ),
            shape = RoundedCornerShape(20.dp),
        ) {
            Text("New +", style = MaterialTheme.typography.labelLarge)
        }
    }

    // ── Inline add form ──────────────────────────────────────
    AnimatedVisibility(
        visible = showForm,
        enter = slideInVertically(initialOffsetY = { -it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it / 2 }) + fadeOut(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KalliorSurfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text(
                        if (selectedCategory == Category.Other) "Custom title…" else "Fixed to ${selectedCategory.displayName}",
                        color = KalliorMuted,
                    )
                },
                enabled = selectedCategory == Category.Other,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KalliorTertiary,
                    unfocusedBorderColor = KalliorMuted.copy(alpha = 0.4f),
                    focusedTextColor = KalliorOnSurface,
                    unfocusedTextColor = KalliorOnSurface,
                    disabledTextColor = KalliorMuted.copy(alpha = 0.6f),
                    disabledBorderColor = KalliorMuted.copy(alpha = 0.2f),
                    cursorColor = KalliorTertiary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(50) },
                placeholder = {
                    Text("Description (optional, max 50)", color = KalliorMuted)
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = KalliorTertiary,
                    unfocusedBorderColor = KalliorMuted.copy(alpha = 0.4f),
                    focusedTextColor = KalliorOnSurface,
                    unfocusedTextColor = KalliorOnSurface,
                    cursorColor = KalliorTertiary,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Category selector
                Box {
                    TextButton(onClick = { categoryDropdownExpanded = true }) {
                        Text(
                            text = selectedCategory.displayName,
                            color = KalliorTertiary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false },
                    ) {
                        Category.entries.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.displayName) },
                                onClick = {
                                    selectedCategory = cat
                                    categoryDropdownExpanded = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                // Cancel
                TextButton(onClick = {
                    showForm = false
                    title = ""
                    description = ""
                    selectedCategory = Category.Other
                }) {
                    Text("Cancel", color = KalliorMuted)
                }

                // Add
                Button(
                    onClick = {
                        val customTitle = if (selectedCategory == Category.Other) {
                            title.trim().takeIf { it.isNotBlank() }
                        } else {
                            null
                        }
                        viewModel.addTask(
                            category = selectedCategory,
                            description = description.trim(),
                            customTitle = customTitle,
                        )
                        title = ""
                        description = ""
                        selectedCategory = Category.Other
                        showForm = false
                    },
                    enabled = selectedCategory != Category.Other || title.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KalliorTertiary,
                        contentColor = KalliorSurface,
                    ),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Add", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
