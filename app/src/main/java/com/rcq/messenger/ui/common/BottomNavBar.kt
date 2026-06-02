package com.rcq.messenger.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rcq.messenger.ui.theme.*

/** Flat QIP/JIMM-style tab bar — accent top-line on active tab, no pill */
@Composable
fun BottomNavBar(
    items: List<com.rcq.messenger.ui.Screen>,
    currentRoute: String?,
    onNavigate: (com.rcq.messenger.ui.Screen) -> Unit
) {
    val rcq = LocalRCQColors.current
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        HorizontalDivider(thickness = RCQMetrics.dividerThick, color = rcq.divider)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RCQMetrics.navBarHeight)
                .background(rcq.navBar),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            items.forEach { screen ->
                val selected = currentRoute == screen.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onNavigate(screen) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (selected) rcq.accent else rcq.navBar)
                    )
                    Spacer(modifier = Modifier.height(7.dp))
                    Icon(
                        imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = screen.title,
                        tint = if (selected) rcq.accent else rcq.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = screen.title,
                        fontSize = RCQFontSize.monoSmall,
                        color = if (selected) rcq.accent else rcq.textSecondary,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
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