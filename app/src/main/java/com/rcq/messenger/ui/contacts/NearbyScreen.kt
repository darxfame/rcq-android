package com.rcq.messenger.ui.contacts

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.rcq.messenger.domain.model.User
import com.rcq.messenger.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun NearbyScreen(
    viewModel: NearbyViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit
) {
    val users by viewModel.users.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val rcq = LocalRCQColors.current

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            viewModel.searchLastKnown()
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (locationPermissionState.status.isGranted) viewModel.searchLastKnown()
                            else locationPermissionState.launchPermissionRequest()
                        }
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = rcq.bgPrimary,
                    titleContentColor = rcq.textPrimary,
                    navigationIconContentColor = rcq.textPrimary,
                    actionIconContentColor = rcq.accent
                )
            )
        },
        containerColor = rcq.bgPrimary
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = rcq.accent,
                    trackColor = rcq.bgSecondary
                )
            }
            error?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = RCQMetrics.screenHPad, vertical = RCQMetrics.rowHPad)
                )
            }
            if (!locationPermissionState.status.isGranted) {
                PermissionPrompt(onRequestPermission = { locationPermissionState.launchPermissionRequest() })
            } else if (users.isEmpty() && !isLoading) {
                EmptyNearbyState()
            } else {
                LazyColumn {
                    items(users, key = { it.id }) { user ->
                        NearbyUserRow(user = user, onClick = { onUserClick(user.id) })
                        HorizontalDivider(
                            modifier = Modifier.padding(start = RCQMetrics.screenHPad + RCQMetrics.avatarLg + RCQMetrics.rowHPad + RCQMetrics.rowHPad),
                            thickness = RCQMetrics.dividerThick,
                            color = rcq.divider
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(onRequestPermission: () -> Unit) {
    val rcq = LocalRCQColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = onRequestPermission) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = rcq.textPrimary)
            Spacer(Modifier.width(RCQMetrics.rowHPad))
            Text("Allow location")
        }
    }
}

@Composable
private fun EmptyNearbyState() {
    val rcq = LocalRCQColors.current
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = rcq.textSecondary,
            modifier = Modifier.size(RCQMetrics.avatarLg)
        )
        Spacer(Modifier.height(RCQMetrics.rowHPad))
        Text("No one nearby", color = rcq.textSecondary)
    }
}

@Composable
private fun NearbyUserRow(user: User, onClick: () -> Unit) {
    val rcq = LocalRCQColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = RCQMetrics.screenHPad, vertical = RCQMetrics.rowHPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(RCQMetrics.avatarLg + RCQMetrics.rowHPad)
                .clip(CircleShape)
                .background(rcq.bgSecondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.nickname.firstOrNull()?.uppercase() ?: "?",
                color = rcq.accent,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(RCQMetrics.rowHPad))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.nickname.ifBlank { user.id.toString() },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = rcq.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "UIN: ${user.id}",
                style = MaterialTheme.typography.bodySmall,
                color = rcq.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
