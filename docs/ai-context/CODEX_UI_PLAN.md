# Codex UI Plan — Chat & Groups Feature Parity with iOS

> Branch: `ios-parity-transport-build`
> iOS reference: rcq-messenger/rcq-ios, Views/ folder
> After each block: `./gradlew compileProductionDebugKotlin`
> Architecture: Jetpack Compose + ViewModel + Hilt. Do NOT change architecture.
> Fix only what is specified. Do NOT add new dependencies.

---

## BLOCK UI-1 — Wire broken stubs in ChatScreen

**File:** `app/src/main/java/com/rcq/messenger/ui/chat/ChatScreen.kt`

### UI-1a. Add parameters and wire Call button
```kotlin
// Change ChatScreen signature:
fun ChatScreen(
    chatId: String,
    onBack: () -> Unit,
    onCall: () -> Unit = {},        // ADD
    onGroupInfo: () -> Unit = {},   // ADD
    viewModel: ChatViewModel = hiltViewModel()
)

// Wire Call button (was: onClick = { /* Call */ }):
IconButton(onClick = onCall) {
    Icon(Icons.Default.Call, contentDescription = "Call")
}
```

### UI-1b. Wire More menu with real actions
```kotlin
// Add state variables after existing state declarations:
var showMoreMenu by remember { mutableStateOf(false) }
var showInChatSearch by remember { mutableStateOf(false) }
var showForwardPicker by remember { mutableStateOf<Message?>(null) }
var reactingMessage by remember { mutableStateOf<Message?>(null) }
val isMuted by viewModel.isMuted.collectAsState()

// Replace the stub IconButton(onClick = { /* More */ }):
Box {
    IconButton(onClick = { showMoreMenu = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = "More")
    }
    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
        if (isGroupChat) {
            DropdownMenuItem(
                text = { Text("Group info") },
                leadingIcon = { Icon(Icons.Default.Info, null) },
                onClick = { showMoreMenu = false; onGroupInfo() }
            )
        }
        DropdownMenuItem(
            text = { Text("Search in chat") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            onClick = { showMoreMenu = false; showInChatSearch = true }
        )
        DropdownMenuItem(
            text = { Text(if (isMuted) "Unmute" else "Mute") },
            leadingIcon = { Icon(if (isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff, null) },
            onClick = { showMoreMenu = false; viewModel.toggleMute() }
        )
        DropdownMenuItem(
            text = { Text("Clear history") },
            leadingIcon = { Icon(Icons.Default.Delete, null) },
            onClick = { showMoreMenu = false; viewModel.clearHistory() }
        )
    }
}
```

### UI-1c. Make group chat title clickable (tap → GroupInfo)
```kotlin
// Wrap the title Row with clickable modifier when isGroupChat:
Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = if (isGroupChat) Modifier.clickable { onGroupInfo() } else Modifier
) {
    // ... existing avatar box and Text(chatTitle) ...
}
```

### UI-1d. Fix reaction picker — remove hardcoded 👍
```kotlin
// BEFORE:
onReact = { viewModel.addReaction(message.id, "👍") }

// AFTER:
onReact = { reactingMessage = message }

// Add after the message list LazyColumn:
reactingMessage?.let { msg ->
    ReactionPickerDialog(
        onDismiss = { reactingMessage = null },
        onReact = { emoji -> viewModel.addReaction(msg.id, emoji); reactingMessage = null }
    )
}
```

Add `ReactionPickerDialog` composable at bottom of file:
```kotlin
@Composable
private fun ReactionPickerDialog(onDismiss: () -> Unit, onReact: (String) -> Unit) {
    val reactions = listOf("👍","❤️","😂","😮","😢","😡","🔥","👏","🎉","💯")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("React") },
        text = {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(reactions) { emoji ->
                    Text(emoji, fontSize = 28.sp, modifier = Modifier.clickable { onReact(emoji) })
                }
            }
        },
        confirmButton = {}
    )
}
```

### UI-1e. Wire forward message to ForwardPickerDialog
```kotlin
// BEFORE:
onForward = { viewModel.forwardMessage(message) }

// AFTER:
onForward = { showForwardPicker = message }

// Add ForwardPickerDialog after reactingMessage block:
showForwardPicker?.let { msg ->
    ForwardPickerDialog(
        onDismiss = { showForwardPicker = null },
        onPick = { targetChatId -> viewModel.forwardMessageTo(msg, targetChatId); showForwardPicker = null },
        contacts = viewModel.contacts.collectAsState().value,
        groups = viewModel.groups.collectAsState().value
    )
}
```

Add `ForwardPickerDialog` composable:
```kotlin
@Composable
private fun ForwardPickerDialog(
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    contacts: List<Contact>,
    groups: List<Group>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forward to…") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                if (contacts.isNotEmpty()) {
                    item { Text("Contacts", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(contacts) { c ->
                        ListItem(
                            headlineContent = { Text(c.nickname) },
                            modifier = Modifier.clickable { onPick("direct_${c.userId}") }
                        )
                    }
                }
                if (groups.isNotEmpty()) {
                    item { Text("Groups", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(vertical = 4.dp)) }
                    items(groups) { g ->
                        ListItem(
                            headlineContent = { Text(g.name) },
                            modifier = Modifier.clickable { onPick(g.id) }
                        )
                    }
                }
            }
        },
        confirmButton = {}
    )
}
```

### UI-1f. Add in-chat search bar
```kotlin
// Add above the LazyColumn (before message list):
if (showInChatSearch) {
    val searchResults by viewModel.inChatSearchResults.collectAsState()
    Column(modifier = Modifier.fillMaxWidth().background(LocalRCQColors.current.bgSecondary)) {
        var q by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = q,
                onValueChange = { q = it; viewModel.searchInChat(it) },
                modifier = Modifier.weight(1f).padding(8.dp),
                placeholder = { Text("Search messages…") },
                singleLine = true
            )
            IconButton(onClick = { showInChatSearch = false; viewModel.clearInChatSearch() }) {
                Icon(Icons.Default.Close, null)
            }
        }
        if (searchResults.isNotEmpty()) {
            LazyColumn(modifier = Modifier.heightIn(max = 160.dp)) {
                items(searchResults) { msg ->
                    Text(
                        text = msg.content,
                        modifier = Modifier.fillMaxWidth().clickable { }.padding(horizontal = 16.dp, vertical = 6.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
```

### UI-1g. Add pinned text banner for group chats
```kotlin
// Add after in-chat search, before LazyColumn:
val pinnedText by viewModel.pinnedText.collectAsState()
if (!pinnedText.isNullOrBlank() && isGroupChat) {
    Surface(modifier = Modifier.fillMaxWidth(), color = LocalRCQColors.current.bgSecondary, tonalElevation = 1.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PushPin, null, tint = LocalRCQColors.current.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(pinnedText!!, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = LocalRCQColors.current.textSecondary)
        }
    }
}
```

---

## BLOCK UI-2 — Add missing state to ChatViewModel

**File:** `app/src/main/java/com/rcq/messenger/ui/chat/ChatViewModel.kt`

Add these fields and functions if they don't exist:
```kotlin
// State
val isMuted: StateFlow<Boolean> = // derive from chatDao or local state
    MutableStateFlow(false).asStateFlow()
val pinnedText: StateFlow<String?> = MutableStateFlow<String?>(null).asStateFlow()
val inChatSearchResults: MutableStateFlow<List<Message>> = MutableStateFlow(emptyList())
val contacts: StateFlow<List<Contact>> = contactRepository.getContacts()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
val groups: StateFlow<List<Group>> = groupRepository.getGroups()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

// Functions
fun toggleMute() {
    viewModelScope.launch { chatRepository.setMuted(_chatId.value, !isMuted.value) }
}
fun clearHistory() {
    viewModelScope.launch { messageDao.clearMessages(_chatId.value) }
}
fun searchInChat(query: String) {
    viewModelScope.launch {
        inChatSearchResults.value = if (query.length >= 2)
            chatRepository.searchInChat(_chatId.value, query) else emptyList()
    }
}
fun clearInChatSearch() { inChatSearchResults.value = emptyList() }
fun forwardMessageTo(message: Message, targetChatId: String) {
    viewModelScope.launch { /* send message.content to targetChatId */ }
}
```

If `GroupRepository` is not injected in ChatViewModel, inject it:
```kotlin
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val contactRepository: ContactRepository,
    private val groupRepository: GroupRepository,  // ADD if missing
    private val messageDao: MessageDao,
    ...
)
```

When loading a group chat (`loadChat(chatId)`), if chatId is numeric, fetch group pinned_text:
```kotlin
fun loadChat(chatId: String) {
    _chatId.value = chatId
    // ... existing logic ...
    if (!chatId.startsWith("direct_")) {
        viewModelScope.launch {
            groupRepository.getGroup(chatId).onSuccess { group ->
                (pinnedText as MutableStateFlow).value = group.pinnedText
            }
        }
    }
}
```

Ensure `Group` domain model has `pinnedText: String?`:
- Check `app/src/main/java/com/rcq/messenger/domain/model/Group.kt`
- If missing, add `val pinnedText: String? = null`
- Also add it to `GroupEntity` and the `GroupApiResponse.toGroupEntity()` mapping

---

## BLOCK UI-3 — Create GroupInfoScreen (MISSING)

**Create new file:** `app/src/main/java/com/rcq/messenger/ui/contacts/GroupInfoScreen.kt`

```kotlin
package com.rcq.messenger.ui.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.rcq.messenger.ui.theme.LocalRCQColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    groupId: String,
    onBack: () -> Unit,
    onMemberClick: (Long) -> Unit = {},
    viewModel: GroupInfoViewModel = hiltViewModel()
) {
    val group by viewModel.group.collectAsState()
    val isOwnerOrAdmin by viewModel.isOwnerOrAdmin.collectAsState()
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val rcq = LocalRCQColors.current

    LaunchedEffect(groupId) { viewModel.load(groupId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Info") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (isOwnerOrAdmin) {
                        IconButton(onClick = { newName = group?.name ?: ""; showRenameDialog = true }) {
                            Icon(Icons.Default.Edit, "Edit")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = rcq.bgPrimary)
            )
        },
        containerColor = rcq.bgPrimary
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Avatar + name
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(rcq.bgSecondary), contentAlignment = Alignment.Center) {
                        Text(group?.name?.firstOrNull()?.uppercase() ?: "G", style = MaterialTheme.typography.headlineLarge, color = rcq.accent)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(group?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = rcq.textPrimary)
                    Text("${group?.memberIds?.size ?: 0} members", style = MaterialTheme.typography.bodyMedium, color = rcq.textSecondary)
                    group?.description?.let { desc ->
                        if (desc.isNotBlank()) { Spacer(Modifier.height(4.dp)); Text(desc, style = MaterialTheme.typography.bodySmall, color = rcq.textSecondary) }
                    }
                }
            }

            // Pinned text
            group?.pinnedText?.let { pinned ->
                if (pinned.isNotBlank()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PushPin, null, tint = rcq.accent)
                                Spacer(Modifier.width(8.dp))
                                Text(pinned, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Members section
            item { Text("Members", style = MaterialTheme.typography.labelLarge, color = rcq.textSecondary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)) }
            items(group?.memberIds ?: emptyList()) { memberId ->
                val isAdmin = group?.adminIds?.contains(memberId) == true
                val isOwner = group?.ownerId == memberId
                ListItem(
                    headlineContent = { Text(memberId.toString()) },
                    supportingContent = { if (isOwner) Text("Owner", color = rcq.accent) else if (isAdmin) Text("Admin", color = rcq.accent) },
                    leadingContent = {
                        Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(rcq.bgSecondary), contentAlignment = Alignment.Center) {
                            Text(memberId.toString().take(2), color = rcq.accent)
                        }
                    },
                    trailingContent = {
                        if (isOwnerOrAdmin && memberId != viewModel.ownUin) {
                            IconButton(onClick = { viewModel.removeMember(memberId) }) {
                                Icon(Icons.Default.PersonRemove, null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    modifier = Modifier.clickable { onMemberClick(memberId) }
                )
            }

            // Actions
            item { Spacer(Modifier.height(16.dp)) }
            item {
                ListItem(
                    headlineContent = { Text("Leave group", color = MaterialTheme.colorScheme.error) },
                    leadingContent = { Icon(Icons.Default.ExitToApp, null, tint = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { showLeaveDialog = true }
                )
            }
            if (isOwnerOrAdmin) {
                item {
                    ListItem(
                        headlineContent = { Text("Delete group", color = MaterialTheme.colorScheme.error) },
                        leadingContent = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        modifier = Modifier.clickable { showDeleteDialog = true }
                    )
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename group") },
            text = { OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = { viewModel.renameGroup(newName); showRenameDialog = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") } }
        )
    }
    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text("Leave group?") },
            confirmButton = { TextButton(onClick = { viewModel.leaveGroup(); showLeaveDialog = false; onBack() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Leave") } },
            dismissButton = { TextButton(onClick = { showLeaveDialog = false }) { Text("Cancel") } }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete group?") },
            text = { Text("This will permanently delete the group for all members.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteGroup(); showDeleteDialog = false; onBack() }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}
```

**Create new file:** `app/src/main/java/com/rcq/messenger/ui/contacts/GroupInfoViewModel.kt`

```kotlin
package com.rcq.messenger.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rcq.messenger.data.repository.GroupRepository
import com.rcq.messenger.domain.model.Group
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInfoViewModel @Inject constructor(
    private val groupRepository: GroupRepository
) : ViewModel() {
    private val _group = MutableStateFlow<Group?>(null)
    val group: StateFlow<Group?> = _group.asStateFlow()

    private val _isOwnerOrAdmin = MutableStateFlow(false)
    val isOwnerOrAdmin: StateFlow<Boolean> = _isOwnerOrAdmin.asStateFlow()

    var ownUin: Long = 0L

    fun load(groupId: String) {
        viewModelScope.launch {
            groupRepository.getGroup(groupId).onSuccess { group ->
                _group.value = group
                _isOwnerOrAdmin.value = group.adminIds.contains(ownUin) || group.ownerId == ownUin
            }
        }
    }

    fun renameGroup(name: String) {
        val g = _group.value ?: return
        viewModelScope.launch {
            groupRepository.updateGroup(g.copy(name = name)).onSuccess { _group.value = it }
        }
    }

    fun removeMember(memberUin: Long) {
        val g = _group.value ?: return
        viewModelScope.launch {
            groupRepository.removeMember(g.id, memberUin).onSuccess { load(g.id) }
        }
    }

    fun leaveGroup() {
        val g = _group.value ?: return
        viewModelScope.launch { groupRepository.removeMember(g.id, ownUin) }
    }

    fun deleteGroup() {
        val g = _group.value ?: return
        viewModelScope.launch { groupRepository.deleteGroup(g.id) }
    }
}
```

---

## BLOCK UI-4 — Register GroupInfoScreen in NavHost (RCQApp.kt)

**File:** `app/src/main/java/com/rcq/messenger/ui/RCQApp.kt`

Add imports:
```kotlin
import com.rcq.messenger.ui.contacts.GroupInfoScreen
```

Add route in NavHost:
```kotlin
// ADD this composable block:
composable(
    route = Routes.GROUP,
    arguments = listOf(navArgument("groupId") { type = NavType.StringType })
) { backStackEntry ->
    val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
    GroupInfoScreen(
        groupId = groupId,
        onBack = { navController.popBackStack() },
        onMemberClick = { userId -> navController.navigate(Routes.userProfile(userId)) }
    )
}
```

Update ChatScreen call site in NavHost to pass onCall and onGroupInfo:
```kotlin
// FIND the existing ChatScreen composable and UPDATE it:
composable(
    route = Routes.CHAT,
    arguments = listOf(navArgument("chatId") { type = NavType.StringType })
) { backStackEntry ->
    val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
    ChatScreen(
        chatId = chatId,
        onBack = { navController.popBackStack() },
        onCall = { navController.navigate(Routes.call(chatId)) },
        onGroupInfo = { navController.navigate(Routes.group(chatId)) }
    )
}
```

Fix AudioRoomsScreen stub:
```kotlin
composable(Screen.AudioRooms.route) {
    AudioRoomsScreen(
        onRoomClick = { roomId -> /* TODO: call joinRoom and navigate to room */ }
    )
}
```

---

## BLOCK UI-5 — Add group join in GroupBrowseScreen

**File:** `app/src/main/java/com/rcq/messenger/ui/contacts/GroupBrowseViewModel.kt`

Add joinGroup function:
```kotlin
fun joinGroup(groupId: String, onSuccess: (String) -> Unit) {
    viewModelScope.launch {
        groupRepository.joinGroup(groupId.toIntOrNull() ?: 0).fold(
            onSuccess = { onSuccess(groupId) },
            onFailure = { _error.value = "Failed to join: ${it.message}" }
        )
    }
}
```

**File:** `app/src/main/java/com/rcq/messenger/ui/contacts/GroupBrowseScreen.kt`

Add a join button to each search result item. Find where groups are rendered and add:
```kotlin
// Each group row should have a trailing "Join" button:
ListItem(
    headlineContent = { Text(group.name) },
    supportingContent = { Text("${group.memberCount} members") },
    trailingContent = {
        TextButton(onClick = {
            viewModel.joinGroup(group.id) { groupId ->
                onGroupJoined(groupId) // navigate to group chat
            }
        }) {
            Text("Join")
        }
    }
)
```

Add `onGroupJoined: (String) -> Unit` parameter to `GroupBrowseScreen` composable.
In RCQApp.kt, wire it: `onGroupJoined = { groupId -> navController.navigate(Routes.chat(groupId)) }`

---

## BLOCK UI-6 — Add Status Picker to SettingsScreen

**File:** `app/src/main/java/com/rcq/messenger/ui/settings/SettingsScreen.kt`

Add at top of settings content list, before profile info:
```kotlin
var showStatusPicker by remember { mutableStateOf(false) }

// Add this ListItem in the settings:
ListItem(
    headlineContent = { Text("Status") },
    supportingContent = { Text(currentStatus.replaceFirstChar { it.uppercase() }) },
    leadingContent = { Icon(Icons.Default.Circle, null, tint = when(currentStatus) {
        "online" -> Color(0xFF4CAF50)
        "away" -> Color(0xFFFF9800)
        "dnd" -> Color(0xFFF44336)
        else -> Color.Gray
    }) },
    modifier = Modifier.clickable { showStatusPicker = true }
)

// Status picker dialog:
if (showStatusPicker) {
    AlertDialog(
        onDismissRequest = { showStatusPicker = false },
        title = { Text("Set Status") },
        text = {
            Column {
                listOf("online" to "Online", "away" to "Away", "dnd" to "Do Not Disturb", "invisible" to "Invisible").forEach { (key, label) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        modifier = Modifier.clickable { onSetStatus(key); showStatusPicker = false }
                    )
                }
            }
        },
        confirmButton = {}
    )
}
```

Add `onSetStatus: (String) -> Unit` to SettingsScreen params and wire from RCQApp.kt:
```kotlin
SettingsScreen(
    ...
    onSetStatus = { status -> authViewModel.setStatus(status) }
)
```

In AuthViewModel (or AppPrefsViewModel) add:
```kotlin
fun setStatus(status: String) {
    viewModelScope.launch { userRepository.updatePresence(status) }
}
```

---

## FINAL VERIFICATION

```bash
./gradlew compileProductionDebugKotlin
./gradlew assembleProductionDebug
./gradlew test
```

Then commit:
```bash
git add app/src/main/java/com/rcq/messenger/ui/
git commit -m "Android UI: GroupInfoScreen, ForwardPicker, ReactionPicker, InChatSearch, PinnedBanner, StatusPicker, wire стабы навигации"
git push origin ios-parity-transport-build
```

---

## What this adds vs iOS parity

| iOS feature | Android fix |
|-------------|------------|
| Group info / members / rename / leave / delete | GroupInfoScreen + GroupInfoViewModel (NEW) |
| Forward message picker | ForwardPickerDialog in ChatScreen |
| Reaction emoji picker | ReactionPickerDialog (was hardcoded 👍) |
| In-chat search | InChatSearchBar overlay |
| Pinned text banner | Surface banner above message list |
| Call button → CallScreen | onCall param wired in NavHost |
| Group header tap → GroupInfo | clickable title row + onGroupInfo param |
| More menu (search/mute/clear) | DropdownMenu replacing stub |
| Group join from browse | Join button in GroupBrowseScreen |
| Status picker (online/away/dnd) | AlertDialog in SettingsScreen |
