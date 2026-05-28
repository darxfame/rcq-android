package com.rcq.messenger.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.rcq.messenger.ui.common.BottomNavBar
import com.rcq.messenger.ui.stories.*
import com.rcq.messenger.ui.calls.*
import com.rcq.messenger.ui.audio.AudioRoomsScreen
import com.rcq.messenger.ui.games.GamesScreen
import com.rcq.messenger.ui.market.MarketplaceScreen
import com.rcq.messenger.ui.settings.SettingsScreen
import com.rcq.messenger.ui.profile.ProfileScreen
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
    data object Games : Screen("games", "Games", Icons.Filled.SportsEsports, Icons.Outlined.SportsEsports)
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
    const val CALL = "call/{callId}"
    const val GROUP = "group/{groupId}"
    const val STORY_VIEWER = "story/{userId}"
    const val MARKETPLACE_ITEM = "marketplace/{itemId}"
    const val GAME = "game/{gameType}"
    const val USER_PROFILE = "profile/{userId}"

    fun chat(chatId: String) = "chat/$chatId"
    fun call(callId: String) = "call/$callId"
    fun group(groupId: String) = "group/$groupId"
    fun storyViewer(userId: Long) = "story/$userId"
    fun marketplaceItem(itemId: String) = "marketplace/$itemId"
    fun game(gameType: String) = "game/$gameType"
    fun userProfile(userId: Long) = "profile/$userId"
}

val bottomNavItems = listOf(
    Screen.Chats, Screen.Contacts, Screen.Settings
)

@Composable
fun RCQApp() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()

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
                    onCreateGroup = { navController.navigate("create_group") }
                )
            }
            composable(Screen.Contacts.route) {
                ContactsScreen(
                    onContactClick = { userId -> navController.navigate(Routes.userProfile(userId)) },
                    onAddContact = { navController.navigate("add_contact") },
                    onPendingRequests = { navController.navigate("pending_requests") }
                )
            }
            composable(Screen.AudioRooms.route) {
                AudioRoomsScreen(
                    onRoomClick = { roomId -> /* Join room */ }
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
                SettingsScreen(
                    currentUin = currentUin,
                    nickname = nickname,
                    recoveryPhrase = recoveryPhrase,
                    onLogout = { authViewModel.logout() }
                )
            }

            // Detail screens
            composable(Routes.CHAT) { backStackEntry ->
                val chatId = backStackEntry.arguments?.getString("chatId") ?: return@composable
                ChatScreen(chatId = chatId, onBack = { navController.popBackStack() })
            }
            composable("create_group") {
                CreateGroupScreen(onBack = { navController.popBackStack() })
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
            composable("profile/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: return@composable
                ProfileScreen(userId = userId, onBack = { navController.popBackStack() })
            }
            composable(Routes.CALL) { backStackEntry ->
                val callId = backStackEntry.arguments?.getString("callId") ?: return@composable
                CallScreen(chatId = callId, targetNickname = "User", onBack = { navController.popBackStack() })
            }
            composable("story/{userId}") { backStackEntry ->
                val userId = backStackEntry.arguments?.getString("userId")?.toLongOrNull() ?: return@composable
                StoryViewerScreen(userId = userId, onBack = { navController.popBackStack() })
            }
            composable("games") {
                GamesScreen(
                    onGameClick = { gameType: String -> navController.navigate("game/$gameType") }
                )
            }
            composable("marketplace") {
                MarketplaceScreen(
                    onItemClick = { itemId -> navController.navigate(Routes.marketplaceItem(itemId)) }
                )
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

    when (authState) {
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