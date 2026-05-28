package com.rcq.messenger.ui.contacts

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rcq.messenger.domain.model.Contact
import com.rcq.messenger.data.api.ContactRequest
import com.rcq.messenger.domain.model.User
import com.rcq.messenger.domain.model.UserStatus
import com.rcq.messenger.ui.common.StatusIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    onBack: () -> Unit,
    onUserClick: (Long) -> Unit = {},
    onSent: () -> Unit = {}
) {
    val viewModel: AddContactViewModel = hiltViewModel()
    val query by viewModel.query.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val contactRequests by viewModel.contactRequests.collectAsState()
    val sentRequests by viewModel.sentRequests.collectAsState()
    val error by viewModel.error.collectAsState()

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Contact") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.updateQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by UIN or nickname") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        viewModel.search()
                        focusManager.clearFocus()
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            )

            // Pending Requests Section at TOP (like iOS AddContactView)
            if (contactRequests.isNotEmpty()) {
                PendingRequestsSection(
                    requests = contactRequests,
                    onAccept = { viewModel.acceptRequest(it) },
                    onDecline = { viewModel.declineRequest(it) },
                    onUserClick = onUserClick
                )
            }

            // Loading
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Error message
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Search results
            if (!isLoading && query.isNotEmpty() && searchResults.isEmpty() && error == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No users found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(searchResults) { user ->
                    AddContactResultItem(
                        user = user,
                        isSent = sentRequests.contains(user.id),
                        onAddClick = { viewModel.sendRequest(user.id) },
                        onUserClick = { onUserClick(user.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingRequestsSection(
    requests: List<ContactRequest>,
    onAccept: (Long) -> Unit,
    onDecline: (Long) -> Unit,
    onUserClick: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Pending Requests (${requests.size})",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        requests.forEach { request ->
            PendingRequestItem(
                request = request,
                onAccept = { onAccept(request.id) },
                onDecline = { onDecline(request.id) },
                onUserClick = { onUserClick(request.fromUin) }
            )
        }
    }
}

@Composable
private fun PendingRequestItem(
    request: ContactRequest,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onUserClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            if (request.avatarUrl != null) {
                // AsyncImage would go here
                Text(
                    text = request.nickname.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = request.nickname.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = request.nickname,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "UIN: ${request.fromUin}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Accept/Decline buttons
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(
                onClick = onAccept,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = "Accept")
            }
            IconButton(
                onClick = onDecline,
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFFF44336)
                )
            ) {
                Icon(Icons.Default.Close, contentDescription = "Decline")
            }
        }
    }
}

@Composable
private fun AddContactResultItem(
    user: User,
    isSent: Boolean,
    onAddClick: () -> Unit,
    onUserClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onUserClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            StatusIndicator(
                status = user.status ?: UserStatus.OFFLINE,
                size = 48
            )
            Text(
                text = user.nickname.firstOrNull()?.uppercase() ?: "?",
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = user.nickname,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "ID: ${user.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (user.bio.isNotEmpty()) {
                Text(
                    text = user.bio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Status badge
        when {
            isSent -> {
                Text(
                    text = "Sent",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFFC107)
                )
            }
            else -> {
                IconButton(onClick = onAddClick) {
                    Icon(
                        Icons.Default.PersonAdd,
                        contentDescription = "Add",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    HorizontalDivider()
}