package org.example.project.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kallos.viewmodel.GameViewModel
import org.example.project.R

/**
 * Profile screen based on the provided design.
 * Displays user name, avatar circles, steps and sleep metrics,
 * and action buttons for account security and bug reporting.
 */
@Composable
fun ProfileScreen(
    gameViewModel: GameViewModel,
) {
    val player = gameViewModel.player
    val snapshot = gameViewModel.todaySnapshot
    
    val steps = snapshot?.steps ?: 0
    val sleepMin = snapshot?.minutesSlept ?: 0.0
    val sleepHours = (sleepMin / 60).toInt()
    val sleepRemainderMin = (sleepMin % 60).toInt()
    val sleepText = if (sleepHours > 0) "${sleepHours}h ${sleepRemainderMin}m" else "${sleepRemainderMin}m"

    val scrollState = rememberScrollState()
    val nameScale by animateFloatAsState(
        targetValue = (1f - scrollState.value * 0.0005f).coerceIn(0.8f, 1f),
        label = "nameScale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KalliorColors.SecondaryBackground)
            .verticalScroll(scrollState)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar spacing preserved
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(modifier = Modifier.size(36.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        // User Name in brackets
        Text(
            text = "[${player.name.ifEmpty { "User Name" }}]",
            style = MaterialTheme.typography.displaySmall.copy(
                fontFamily = Philosopher,
                fontWeight = FontWeight.Normal,
                fontSize = 36.sp,
                color = Color.White
            ),
            modifier = Modifier.graphicsLayer { scaleY = nameScale }
        )

        Spacer(modifier = Modifier.height(60.dp))

        // Avatar Section - Large circle with three smaller ones in an arc below it
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Main Avatar Circle
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .border(2.dp, KalliorColors.AccentOrange, CircleShape)
                    .padding(6.dp)
                    .background(Color(0xFF242424), CircleShape)
            )

            // Left side small circle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = 20.dp, y = (-20).dp)
                    .size(48.dp)
                    .border(1.dp, Color(0xFF424242), CircleShape)
            )
            
            // Right side small circle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-20).dp, y = (-20).dp)
                    .size(48.dp)
                    .border(1.dp, Color(0xFF424242), CircleShape)
            )
            
            // Center bottom small circle
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(48.dp)
                    .border(1.dp, Color(0xFF424242), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(60.dp))

        // Health Metrics: Steps and Sleep
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ProfileMetricItem(label = "Steps: ", value = steps.toString())
            ProfileMetricItem(label = "Sleep: ", value = sleepText)
        }

        Spacer(modifier = Modifier.height(80.dp))

        // Bottom Navigation/Action Buttons
        ProfileActionButton(text = "Account & Security", onClick = {})
        Spacer(modifier = Modifier.height(20.dp))
        ProfileActionButton(text = "Report a bug", onClick = {})
        
        Spacer(modifier = Modifier.height(60.dp))
    }
}

/**
 * A metric item with a label, an orange dash decoration, and the dynamic value.
 */
@Composable
fun ProfileMetricItem(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Philosopher,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = Philosopher,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = KalliorColors.AccentOrange
            )
        )
    }
}

/**
 * A large rounded button with an arrow indicator on the right.
 */
@Composable
fun ProfileActionButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(KalliorColors.ForegroundCard)
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = Philosopher,
                    fontSize = 20.sp,
                    color = Color.White
                )
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = KalliorColors.MutedText,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(-45f) // Rotated to point North-East
            )
        }
    }
}
