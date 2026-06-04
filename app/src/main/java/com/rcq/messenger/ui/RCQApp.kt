package com.rcq.messenger.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rcq.messenger.ui.theme.LocalRCQColors
import com.rcq.messenger.ui.theme.StatusOnline
import com.rcq.messenger.ui.theme.StatusAway
import com.rcq.messenger.ui.theme.StatusBusy
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rcq.messenger.ui.auth.*
import com.rcq.messenger.ui.chat.*
import com.rcq.messenger.ui.contacts.ContactsScreen
import com.rcq.messenger.ui.contacts.AddContactScreen
import com.rcq.messenger.ui.contacts.PendingRequestsScreen
import com.rcq.messenger.ui.contacts.CreateGroupScreen
import com.rcq.messenger.ui.contacts.GroupBrowseScreen
import com.rcq.messenger.ui.contacts.GroupInfoScreen
import com.rcq.messenger.ui.contacts.NearbyScreen
import com.rcq.messenger.ui.contacts.ContactInfoScreen
import com.rcq.messenger.ui.contacts.QRCodeScreen
import com.rcq.messenger.ui.common.BottomNavBar
import com.rcq.messenger.ui.stories.*
import com.rcq.messenger.ui.calls.*
import com.rcq.messenger.ui.audio.AudioRoomsScreen
import com.rcq.messenger.ui.settings.SettingsScreen
import com.rcq.messenger.ui.settings.PrivacySettingsScreen
import com.rcq.messenger.ui.settings.NotificationsSettingsScreen
import com.rcq.messenger.ui.settings.BlockedUsersScreen
import com.rcq.messenger.ui.settings.AboutScreen
import com.rcq.messenger.ui.settings.StealthSettingsScreen
import com.rcq.messenger.ui.settings.PINSettingsScreen
import com.rcq.messenger.ui.settings.ConnectionDiagnosticsScreen
import com.rcq.messenger.ui.settings.ConnectionSettingsSheet
import com.rcq.messenger.ui.calls.CallScreen

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Chats : Screen("chats", "Chats", Icons.Filled.Chat, Icons.Outlined.Chat)
    data object Contacts : Screen("contacts", "Contacts", Icons.Filled.Person, Icons.Outlined.Person)
    data object AudioRooms : Screen("audio_rooms", "Rooms", Icons.Filled.Mic, Icons.Outlined.Mic)
    data object Stories : Screen("stories", "Stories", Icons.Filled.Circle, Icons.Outlined.Circle)
    data object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

sealed class AuthScreen(val route: String) {
    data object Welcome : AuthScreen("auth_welcome")
    data object GenerateId : AuthScreen("auth_generate")
    data object RestoreId : AuthScreen("auth_restore")
    data object SaveRecoveryPhrase : AuthScreen("auth_save_phrase")
    data object VerifyIdentity : AuthScreen("auth_verify")
}

object Routes {
    const val CHAT = "chat/{chatId}"
    const val CALL = "call/{chatId}/{targetUin}"
    const val GROUP = "group/{groupId}"
    const val STORY_VIEWER = "story/{userId}"
    const val USER_PROFILE = "profile/{userId}"
    const val NEARBY = "nearby"
    const val QR = "qr/{uin}"

    fun chat(chatId: String) = "chat/$chatId"
    fun call(chatId: String, targetUin: Long) = "call/$chatId/$targetUin"
    fun group(groupId: String) = "group/$groupId"
    fun storyViewer(userId: Long) = "story/$userId"
    fun userProfile(userId: Long) = "profile/$userId"
    fun qr(uin: Long) = "qr/$uin"
}

val bottomNavItems = listOf(
    Screen.Chats,
    Screen.Contacts,
    Screen.AudioRooms,
    Screen.Stories,
    Screen.Settings
)

@Composable
fun RCQApp(initialChatId: String? = null, initialScreen: String? = null) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

    LaunchedEffect(isAuthenticated, initialChatId, initialScreen) {
        if (!isAuthenticated) return@LaunchedEffect
        when {
            initialChatId != null -> navController.navigate(Routes.chat(initialChatId))
            initialScreen == "contacts" -> navController.navigate(Screen.Contacts.route)
            initialScreen == "call" -> { /* handled via call_id extra separately */ }
        }
    }

    if (isAuthenticated) {
        MainScaffold(navController = navController, authViewModel = authViewModel)
    } else {
        AuthNavigation(navController = navController, authViewModel = authViewModel)
    }
}

@Composable
fun MainScaffold(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    items = bottomNavItems,
                    currentRoute = currentDestination?.route,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Chats.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Chats.route) {
                ChatsScreen(
                    onChatClick = { chatId -> navController.navigate(Routes.chat(chatId)) },
                    onCreateGroup = { navController.navigate("create_group") },
                    onNewDirectMessage = { navController.navigate(Screen.Contacts.route) },
                    onBrowseGroups = { navController.navigate("groups") }
                )
            }
            composable(Screen.Contacts.route) {
                ContactsScreen(
                    onContactClick = { userId -> navController.navigate(Routes.userProfile(userId)) },
                    onChatClick = { chatId -> navController.navigate(Routes.chat(chatId)) },
                    onGroupClick = { groupId -> navController.navigate(Routes.chat(groupId)) },
                    onAddContact = { navController.navigate("add_contact") },
                    onPendingRequests = { navController.navigate("pending_requests") },
                    onNearby = { navController.navigate(Routes.NEARBY) }
                )
            }
            composable(Screen.AudioRooms.route) {
                AudioRoomsScreen(
                    onRoomClick = { }
                )
            }
            composable(Screen.Stories.route) {
                StoriesScreen(
                    onStoryClick = { userId -> navController.navigate(Routes.storyViewer(userId)) }
                )
            }
            composable(Screen.Settings.route) {
                val currentUin by authViewModel.currentUin.collectAsState()
                val nickname by authViewModel.nickname.collectAsState()
                val recoveryPhrase by authViewModel.recoveryPhrase.collectAsState()
                val currentStatus by authViewModel.currentStatus.collectAsState()
                SettingsScreen(
                    currentUin = currentUin,
                    nickname = nickname,
                    recoveryPhrase = recoveryPhrase,
                    currentStatus = currentStatus,
                    onSetStatus = { status -> authViewModel.setStatus(status) },
                    onLogout = { authViewModel.logout() },
                    onNavigateToStealth = { navController.navigate("settings/stealth") },
                    onNavigateToPin = { navController.navigate("settings/pin") },
                    onNavigateToDiagnostics = { navController.navigate("settings/diagnostics") },
                    onNavigateToPrivacy = { navController.navigate("settings/privacy") },
                    onNavigateToNotifications = { navController.navigate("settings/notifications") },
                    onNavigateToAbout = { navController.navigate("settings/about") },
                    onNavigateToQr = { uin -> navController.navigate(Routes.qr(uin)) },
                    onNavigateToBlocked = { navController.navigate("settings/blocked") }
                )
            }
            composable(
                route = Routes.QR,
                arguments = listOf(navArgument("uin") { type = NavType.LongType })
            ) { backStackEntry ->
                val ownUin = backStackEntry.arguments?.getLong("uin") ?: 0L
                QRCodeScreen(
                    ownUin = ownUin,
                    onBack = { navController.popBackStack() },
                    onUserScanned = { userId -> navController.navigate(Routes.userProfile(userId)) }
                )
            }
            composable("settings/about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/notifications") {
                NotificationsSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/privacy") {
                PrivacySettingsScreen(
                    onBack = { navController.popBackStack() },
                    onBlockedUsers = { navController.navigate("settings/blocked") }
                )
            }
            composable("settings/blocked") {
                BlockedUsersScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/stealth") {
                StealthSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/pin") {
                PINSettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("settings/diagnostics") {
                ConnectionDiagnosticsScreen(onBack = { navController.popBackStack() })
            }

            // Detail screens
            composable(
                route = Routes.CHAT,
                arguments = listOf(navArgument("chatId") { type = NavType.StringType })
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                ChatScreen(
                    chatId = chatId,
                    onBack = { navController.popBackStack() },
                    onCall = { targetUin -> navController.navigate(Routes.call(chatId, targetUin)) },
                    onGroupInfo = { navController.navigate(Routes.group(chatId)) }
                )
            }
            composable(
                route = Routes.GROUP,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupInfoScreen(
                    groupId = groupId,
                    onBack = { navController.popBackStack() },
                    onMemberClick = { userId -> navController.navigate(Routes.userProfile(userId)) }
                )
            }
            composable("create_group") {
                CreateGroupScreen(onBack = { navController.popBackStack() })
            }
            composable("groups") {
                GroupBrowseScreen(
                    onBack = { navController.popBackStack() },
                    onGroupClick = { chatId -> navController.navigate(Routes.chat(chatId)) },
                    onGroupJoined = { groupId -> navController.navigate(Routes.chat(groupId)) }
                )
            }
            composable("add_contact") {
                AddContactScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(Routes.userProfile(userId)) }
                )
            }
            composable("pending_requests") {
                PendingRequestsScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(Routes.userProfile(userId)) }
                )
            }
            composable(Routes.NEARBY) {
                NearbyScreen(
                    onBack = { navController.popBackStack() },
                    onUserClick = { userId -> navController.navigate(Routes.userProfile(userId)) }
                )
            }
            composable(Routes.USER_PROFILE) { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: return@composable
                ContactInfoScreen(
                    userId = userId,
                    onBack = { navController.popBackStack() },
                    onChat = { chatId ->
                        navController.navigate(Routes.chat(chatId)) {
                            popUpTo(Routes.USER_PROFILE) { inclusive = true }
                        }
                    },
                    onCall = { uin -> navController.navigate(Routes.call("direct_$uin", uin)) }
                )
            }
            composable(
                route = Routes.CALL,
                arguments = listOf(
                    navArgument("chatId") { type = NavType.StringType },
                    navArgument("targetUin") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                val targetUin = backStackEntry.arguments?.getLong("targetUin") ?: 0L
                CallScreen(
                    chatId = chatId,
                    targetUin = targetUin,
                    targetNickname = "User",
                    onBack = { navController.popBackStack() }
                )
            }
            composable("story/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: return@composable
                StoryViewerScreen(userId = userId, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun AuthNavigation(navController: NavHostController, authViewModel: AuthViewModel) {
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val recoveryPhrase by authViewModel.recoveryPhrase.collectAsState()
    val currentUin by authViewModel.currentUin.collectAsState()
    val connectionStatus by authViewModel.connectionStatus.collectAsState()
    var showConnectionSettings by remember { mutableStateOf(false) }

    if (showConnectionSettings) {
        ConnectionSettingsSheet(onDismiss = { showConnectionSettings = false })
    }

    when (authState) {
        is AuthState.Loading -> {
            ConnectionProbeSplash(
                statusText = connectionStatus,
                onConnectionSettings = { showConnectionSettings = true }
            )
            return
        }
        is AuthState.ShowRecoveryPhrase -> {
            RecoveryPhraseScreen(
                phrase = recoveryPhrase,
                onConfirm = { authViewModel.confirmRecoveryPhrase() },
                onCopy = { /* TODO: Copy to clipboard */ }
            )
        }
        else -> {
            NavHost(navController = navController, startDestination = AuthScreen.Welcome.route) {
                composable(AuthScreen.Welcome.route) {
                    WelcomeScreen(
                        onCreateAccount = { authViewModel.startRegistration(it) },
                        onRestoreAccount = { navController.navigate(AuthScreen.RestoreId.route) },
                        onConnectionSettings = { showConnectionSettings = true },
                        isLoading = isLoading,
                        error = error
                    )
                }
                composable(AuthScreen.RestoreId.route) {
                    AccountRecoveryScreen(
                        onBack = { navController.popBackStack() },
                        onRecoveryComplete = { authViewModel.recheckAuth() }
                    )
                }
            }
        }
    }
}
@Composable
fun ConnectionProbeSplash(
    statusText: String,
    onConnectionSettings: () -> Unit = {}
) {
    val rcq = LocalRCQColors.current
    val rotation by rememberInfiniteTransition(label = "spin").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "rot"
    )
    Box(Modifier.fillMaxSize().background(rcq.bgPrimary), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // ICQ flower: 8 petals rotating
            Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                val petalColors = listOf(StatusOnline, StatusAway, StatusBusy, rcq.accent,
                    StatusOnline, StatusAway, StatusBusy, rcq.accent)
                petalColors.forEachIndexed { i, color ->
                    Box(
                        Modifier
                            .rotate(rotation + i * 45f)
                            .offset(y = (-20).dp)
                            .size(14.dp, 22.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.75f + i * 0.03f))
                    )
                }
                Box(Modifier.size(16.dp).clip(CircleShape).background(rcq.accent))
            }
            Spacer(Modifier.height(24.dp))
            Text("RCQ", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = rcq.accent, fontFamily = FontFamily.Monospace)
            Spacer(Modifier.height(12.dp))
            Text(statusText, fontSize = 13.sp, color = rcq.textSecondary)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onConnectionSettings) {
                Text("Подключение", color = rcq.textSecondary)
            }
        }
    }
}
