package org.example.project.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.AddAppViewModel
import org.example.project.InstalledAppInfo

@Composable
fun AddAppScreen(navController: NavHostController, mode: String = "BLOCKER") {
    val context = LocalContext.current.applicationContext
    val viewModel = remember(mode) { AddAppViewModel(context, mode) }
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val permissionError by viewModel.permissionError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.PrimaryLayer)
                    .clickable(onClick = { navController.popBackStack() }),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "<", color = Color.White, fontSize = 22.sp)
            }
            Text(
                text = if (mode == "TIME_WASTING") "Select Time Wasters" else "Limit an App",
                style = MaterialTheme.typography.headlineSmall,
                color = KalliorColors.NormalText,
                modifier = Modifier.padding(start = 16.dp),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.categories) { category ->
                FilterChip(
                    selected = category == selectedCategory,
                    onClick = { viewModel.onCategorySelected(category) },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = KalliorColors.PrimaryLayer,
                        selectedContainerColor = KalliorColors.AccentOrange,
                        labelColor = KalliorColors.NormalText,
                        selectedLabelColor = Color.Black,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No apps in this category",
                    color = KalliorColors.MutedText,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.apps, key = { it.packageName }) { app ->
                    AppItem(
                        app = app,
                        isBlocked = uiState.blockedApps.contains(app.packageName),
                        isStrictlyBlocked = uiState.strictlyBlockedApps.contains(app.packageName),
                        mode = mode,
                        onToggle = { viewModel.toggleAppBlock(app.packageName) },
                        loadIcon = viewModel::loadIcon,
                    )
                }
            }
        }
    }

    if (permissionError) {
        AlertDialog(
            onDismissRequest = { viewModel.clearPermissionError() },
            confirmButton = {
                Text(
                    text = "OK",
                    color = KalliorColors.AccentOrange,
                    modifier = Modifier.clickable { viewModel.clearPermissionError() },
                )
            },
            title = { Text("Permissions required") },
            text = {
                Text(
                    "Grant Usage Access and Overlay permission on the Focus Fortress " +
                        "screen first, then try blocking the app again.",
                )
            },
        )
    }
}

@Composable
private fun AppItem(
    app: InstalledAppInfo,
    isBlocked: Boolean,
    isStrictlyBlocked: Boolean,
    mode: String,
    onToggle: () -> Unit,
    loadIcon: (String) -> Drawable?,
) {
    val icon by produceState<Drawable?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) { loadIcon(app.packageName) }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isBlocked) KalliorColors.ForegroundCard else KalliorColors.PrimaryLayer)
            .clickable(onClick = onToggle)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(KalliorColors.SecondaryBackground),
            contentAlignment = Alignment.Center,
        ) {
            icon?.let {
                Image(
                    bitmap = it.toImageBitmap(),
                    contentDescription = app.appName,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.appName,
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (mode != "TIME_WASTING" && isBlocked) {
                Text(
                    text = "Blocked",
                    color = KalliorColors.AccentOrange,
                    style = MaterialTheme.typography.labelSmall,
                )
            } else if (mode == "TIME_WASTING" && !isBlocked && isStrictlyBlocked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Blocked in Fortress",
                        tint = KalliorColors.MutedText,
                        modifier = Modifier.size(12.dp).padding(end = 4.dp)
                    )
                    Text(
                        text = "Blocked in Fortress",
                        color = KalliorColors.MutedText,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }

        Switch(checked = isBlocked, onCheckedChange = { onToggle() })
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    val width = if (intrinsicWidth > 0) intrinsicWidth else 1
    val height = if (intrinsicHeight > 0) intrinsicHeight else 1
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, width, height)
    draw(canvas)
    return bitmap.asImageBitmap()
}
