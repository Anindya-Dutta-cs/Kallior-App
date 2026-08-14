package org.example.project.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun SidebarContent(
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = KalliorColors.PrimaryLayer,
        drawerContentColor = KalliorColors.NormalText,
        modifier = Modifier.width(300.dp).fillMaxHeight(),
        drawerShape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Text(
                text = "Kallior",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = KalliorColors.NormalText
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Profile Preview placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFACACAC).copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Profile Preview",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 20.sp,
                    fontFamily = FontFamily.SansSerif // Fallback for League Spartan
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            val navItems = listOf(
                SidebarItem("Home", "home"),
                SidebarItem("AriaAlarm", "alarm"),
                SidebarItem("Focus Fortress", "blocker"),
                SidebarItem("Settings", "settings"),
                SidebarItem("About Us", "about")
            )

            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                navItems.forEach { item ->
                    SidebarRow(
                        item = item,
                        isSelected = currentRoute == item.route,
                        onClick = { onItemClick(item.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "Version - ##.##",
                style = MaterialTheme.typography.labelSmall,
                color = KalliorColors.MutedText,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

data class SidebarItem(val label: String, val route: String)

@Composable
fun SidebarRow(
    item: SidebarItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // Indicator bar
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (isSelected) Color(0xFFACACAC).copy(alpha = 0.2f) else Color(0xFFACACAC).copy(alpha = 0.1f))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = item.label,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = FontFamily.SansSerif // Replace with League Spartan when available
            ),
            color = if (isSelected) KalliorColors.NormalText else KalliorColors.NormalText.copy(alpha = 0.9f)
        )
    }
}
