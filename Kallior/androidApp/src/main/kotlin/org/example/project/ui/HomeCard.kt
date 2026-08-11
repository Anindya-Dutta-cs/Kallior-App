package org.example.project.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A titled section card with a centered muted message (when empty) or arbitrary
 * content, and a floating accent button anchored to the top-right corner so it
 * overlaps the card boundary like a traditional FAB.
 *
 * @param emptyText when non-null, shown centered; otherwise [content] is rendered.
 * @param onButtonTap invoked when the floating "+" / action button is tapped.
 */
@Composable
fun SectionView(
    title: String,
    buttonLabel: String,
    onButtonTap: () -> Unit,
    modifier: Modifier = Modifier,
    emptyText: String? = null,
    contentEndPadding: androidx.compose.ui.unit.Dp = 56.dp,
    subtitle: String? = null,
    buttonColor: Color = KalliorColors.AccentOrange,
    buttonGlyphColor: Color = Color.White,
    enabled: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = Philosopher,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = KalliorColors.NormalText,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = Philosopher),
                    color = KalliorColors.MutedText,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 140.dp)
                    .border(
                        BorderStroke(1.dp, Color(0xFF2C2C2C)),
                        RoundedCornerShape(24.dp)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.Transparent)
                    .padding(16.dp),
            ) {
                if (emptyText != null) {
                    Text(
                        text = emptyText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = Philosopher
                        ),
                        color = KalliorColors.MutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(16.dp),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 16.dp, end = contentEndPadding),
                    ) {
                        content()
                    }
                }
            }

            // Floating action button centered on the top-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = (-20).dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(buttonColor)
                    .clickable(enabled = enabled, onClick = onButtonTap),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = buttonGlyphColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
