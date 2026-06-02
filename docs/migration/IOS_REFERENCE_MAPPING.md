# iOS → Android File Mapping

## Crypto
| iOS | Android | Status |
|---|---|---|
| EciesCrypto.swift | crypto/EciesCrypto.kt | ✅ |
| SignalSessionManager.swift | crypto/SessionManager.kt | ✅ |
| MnemonicHelper.swift | crypto/MnemonicHelper.kt | ✅ |

## Network
| iOS | Android | Status |
|---|---|---|
| WebSocketService.swift | data/websocket/WebSocketService.kt | ⚠️ |
| APIClient.swift | data/api/RCQApiService.kt | ✅ most |

## UI (behavior match, not clone)
| iOS | Android | Status |
|---|---|---|
| ChatsViewController | ui/chat/ChatsScreen.kt | ⚠️ |
| ChatViewController | ui/chat/ChatScreen.kt | ⚠️ |
| ContactsViewController | ui/contacts/ContactsScreen.kt | ⚠️ |
