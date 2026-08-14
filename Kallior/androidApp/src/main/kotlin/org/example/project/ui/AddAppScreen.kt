package org.example.project.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import android.os.Build
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.example.project.AddAppViewModel
import org.example.project.InstalledAppInfo
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun AddAppScreen(navController: NavHostController, mode: String = "BLOCKER") {
    val context = LocalContext.current.applicationContext
    val viewModel = remember(mode) { AddAppViewModel(context, mode) }
    val uiState by viewModel.uiState.collectAsState()
    val permissionError by viewModel.permissionError.collectAsState()

    var expandedCategories by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Top Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Close Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.PrimaryLayer)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = KalliorColors.NormalText,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Done Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.AccentOrange)
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Done",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (mode == "TIME_WASTING") "Select Time Wasters" else "Limit an App",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                letterSpacing = 0.5.sp
            ),
            color = KalliorColors.NormalText,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(uiState.categories) { category ->
                val apps = uiState.appsByCategory[category] ?: emptyList()
                if (apps.isNotEmpty()) {
                    CategorySection(
                        category = category,
                        apps = apps,
                        isExpanded = expandedCategories.contains(category),
                        onToggle = {
                            expandedCategories = if (expandedCategories.contains(category)) {
                                expandedCategories - category
                            } else {
                                expandedCategories + category
                            }
                        },
                        blockedApps = uiState.blockedApps,
                        onAppToggle = viewModel::toggleAppBlock,
                        loadIcon = viewModel::loadIcon
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
            title = {
                val view = LocalView.current
                SideEffect {
                    val window = (view.parent as? DialogWindowProvider)?.window
                    if (window != null) {
                        window.navigationBarColor = android.graphics.Color.TRANSPARENT
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            window.isNavigationBarContrastEnforced = false
                        }
                    }
                }
                Text("Permissions required")
            },
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
private fun CategorySection(
    category: String,
    apps: List<InstalledAppInfo>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    blockedApps: Set<String>,
    onAppToggle: (String) -> Unit,
    loadIcon: (String) -> Drawable?,
) {
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Category Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(1.dp, KalliorColors.RadarLine, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(KalliorColors.PrimaryLayer)
                .clickable(onClick = onToggle)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = category,
                color = KalliorColors.NormalText,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                )
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = if (isExpanded) KalliorColors.AccentOrange else KalliorColors.AccentOrange.copy(alpha = 0.7f),
                modifier = Modifier.size(28.dp).rotate(rotation)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                // App Grid
                val columns = 6
                val rows = (apps.size + columns - 1) / columns
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (i in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (j in 0 until columns) {
                                val index = i * columns + j
                                if (index < apps.size) {
                                    val app = apps[index]
                                    AppGridItem(
                                        app = app,
                                        isBlocked = blockedApps.contains(app.packageName),
                                        onToggle = { onAppToggle(app.packageName) },
                                        loadIcon = loadIcon,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AppGridItem(
    app: InstalledAppInfo,
    isBlocked: Boolean,
    onToggle: () -> Unit,
    loadIcon: (String) -> Drawable?,
    modifier: Modifier = Modifier
) {
    val icon by produceState<Drawable?>(initialValue = null, app.packageName) {
        value = withContext(Dispatchers.IO) { loadIcon(app.packageName) }
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isBlocked) KalliorColors.AccentOrange.copy(alpha = 0.15f) else KalliorColors.ForegroundCard.copy(alpha = 0.5f))
            .border(
                width = 1.dp,
                color = if (isBlocked) KalliorColors.AccentOrange else KalliorColors.RadarLine.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        icon?.let {
            Image(
                bitmap = it.toImageBitmap(),
                contentDescription = app.appName,
                modifier = Modifier.size(32.dp)
            )
        }
        
        if (isBlocked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(KalliorColors.AccentOrange),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
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
