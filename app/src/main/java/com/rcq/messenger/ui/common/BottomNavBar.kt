package com.rcq.messenger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.rcq.messenger.ui.theme.*

@Composable
fun BottomNavBar(
    items: List<com.rcq.messenger.ui.Screen>,
    currentRoute: String?,
    onNavigate: (com.rcq.messenger.ui.Screen) -> Unit
) {
    val rcq = LocalRCQColors.current
    NavigationBar(
        containerColor = rcq.navBar,
        contentColor = rcq.textPrimary
    ) {
        items.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(screen) },
                icon = {
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(text = screen.title, style = MaterialTheme.typography.labelSmall)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = rcq.accent,
                    selectedTextColor = rcq.accent,
                    unselectedIconColor = rcq.textSecondary,
                    unselectedTextColor = rcq.textSecondary,
                    indicatorColor = rcq.accent.copy(alpha = 0.12f)
                )
            )
        }
    }
}

@Composable
fun RCQButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Primary,
            disabledContainerColor = Primary.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = OnPrimary,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun RoundedImage(
    imageUrl: String?,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier.size(48.dp),
    shape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier.clip(shape).background(SurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (imageUrl != null) {
            // Use Coil for actual image loading
            // AsyncImage(model = imageUrl, contentDescription = null)
            content()
        } else {
            content()
        }
    }
}