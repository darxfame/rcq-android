package com.rcq.messenger.ui.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.AudioRoomRepository
import com.rcq.messenger.domain.model.AudioRoom
import com.rcq.messenger.domain.model.RoomSpeaker
import com.rcq.messenger.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AudioRoomsViewModel @Inject constructor(
    private val audioRoomRepository: AudioRoomRepository
) : ViewModel() {

    private val _rooms = MutableStateFlow<List<AudioRoom>>(emptyList())
    val rooms: StateFlow<List<AudioRoom>> = _rooms.asStateFlow()

    private val _currentRoom = MutableStateFlow<AudioRoom?>(null)
    val currentRoom: StateFlow<AudioRoom?> = _currentRoom.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            audioRoomRepository.getAudioRooms()
                .onSuccess { _rooms.value = it }
            _isLoading.value = false
        }
    }

    fun createRoom(title: String) {
        viewModelScope.launch {
            _isLoading.value = true
            audioRoomRepository.createRoom(title, true)
                .onSuccess { room ->
                    _currentRoom.value = room
                    refresh()
                }
            _isLoading.value = false
        }
    }

    fun joinRoom(roomId: String) {
        viewModelScope.launch {
            audioRoomRepository.joinRoom(roomId)
                .onSuccess { _currentRoom.value = it }
        }
    }

    fun leaveRoom() {
        viewModelScope.launch {
            _currentRoom.value?.let { room ->
                audioRoomRepository.leaveRoom(room.id)
                _currentRoom.value = null
            }
        }
    }

    fun toggleMute() {
        viewModelScope.launch {
            _currentRoom.value?.let { room ->
                audioRoomRepository.toggleMute(room.id)
                _isMuted.value = !_isMuted.value
            }
        }
    }

    fun raiseHand() {
        viewModelScope.launch {
            _currentRoom.value?.let { room ->
                audioRoomRepository.raiseHand(room.id)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioRoomsScreen(
    viewModel: AudioRoomsViewModel = hiltViewModel(),
    onRoomClick: (String) -> Unit
) {
    val rooms by viewModel.rooms.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreateRoomDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title ->
                viewModel.createRoom(title)
                showCreateDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Audio Rooms",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create Room")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = TextPrimary,
                    actionIconContentColor = Primary
                )
            )
        },
        containerColor = Background
    ) { paddingValues ->
        if (rooms.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = TextTertiary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No active rooms",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text("Create Room")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                items(rooms) { room ->
                    AudioRoomItem(
                        room = room,
                        onClick = {
                            viewModel.joinRoom(room.id)
                            onRoomClick(room.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AudioRoomItem(
    room: AudioRoom,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "${room.speakerCount + room.listenerCount} listening",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Hosted by ${room.hostNickname}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Speakers preview
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                room.speakers.take(5).forEach { speaker ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(speaker.avatarUrl?.let { SurfaceVariant } ?: Primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = speaker.nickname.firstOrNull()?.uppercase() ?: "?",
                            color = Primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
                if (room.speakers.size > 5) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+${room.speakers.size - 5}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreateRoomDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Audio Room") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Room Title") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = SurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title) },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ActiveRoomView(
    room: AudioRoom,
    isMuted: Boolean,
    onLeave: () -> Unit,
    onToggleMute: () -> Unit,
    onRaiseHand: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = room.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            IconButton(onClick = onLeave) {
                Icon(Icons.Default.Close, "Leave", tint = Error)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Speakers
        Text(
            text = "Speakers (${room.speakers.size})",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            room.speakers.forEach { speaker ->
                SpeakerItem(
                    speaker = speaker,
                    isMuted = isMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Listeners
        Text(
            text = "Listeners (${room.listeners.size})",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.weight(1f))

        // Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onToggleMute,
                containerColor = if (isMuted) Error else Success,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    modifier = Modifier.size(28.dp),
                    tint = OnPrimary
                )
            }

            FloatingActionButton(
                onClick = onRaiseHand,
                containerColor = Primary,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.PanTool,
                    contentDescription = "Raise Hand",
                    modifier = Modifier.size(28.dp),
                    tint = OnPrimary
                )
            }

            FloatingActionButton(
                onClick = onLeave,
                containerColor = SurfaceVariant,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Leave",
                    modifier = Modifier.size(28.dp),
                    tint = Error
                )
            }
        }
    }
}

@Composable
fun SpeakerItem(
    speaker: RoomSpeaker,
    isMuted: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (speaker.isSpeaking && !isMuted) Primary else SurfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = speaker.nickname.firstOrNull()?.uppercase() ?: "?",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (isMuted) {
                Icon(
                    Icons.Default.MicOff,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.BottomEnd),
                    tint = Error
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = speaker.nickname,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            maxLines = 1
        )
    }
}
