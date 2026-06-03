# Android Inbox Hub UI Design

## Goal

Replace the current split "Chats vs Contacts" first-screen experience with an Android-native inbox hub that behaves like the iOS reference while feeling closer to modern messengers such as Telegram.

## Scope

This first UI release covers:

- Main chats screen as a unified inbox.
- Global local search across chats, contacts, and groups.
- Starter rows for synced groups and system/default contacts even when no messages exist yet.
- Chat header and message surface fixes for peer/group identity, emoji access, and media actions.

This release does not implement unrelated iOS-only surfaces such as games, marketplace, pets, full story parity, or full settings parity.

## iOS Reference Behavior

iOS `ContactListView` is the home navigation hub. It refreshes contacts, groups, audio rooms, stories, and news on entry. It opens contacts and groups directly into `ChatView`, keeps default rows visible after sync, and gates the empty state on a completed first refresh so the screen does not flash as empty during cold launch.

Android should translate that behavior into an Android-native inbox:

- Contacts and groups are first-class navigation targets.
- Groups such as `RCQ Beta` appear even before they have messages.
- The `.Dev` default contact appears as a normal row when available.
- Empty state appears only after first sync completes and there are no chats, contacts, or groups to show.

## Target UX

The main screen has:

- Compact top bar with title, connection/bypass status affordance, search, and compose actions.
- Search field that can expand without leaving the screen.
- Unified list rows for direct chats, group chats, contacts, and system/default contacts.
- Sections only when helpful: recent chats first, then contacts/groups that can start a conversation.
- Telegram-like row density: avatar/status, title, subtitle/preview, timestamp, unread badge.
- Pull-to-refresh for contacts/groups/chats sync.

Search results are grouped as:

- Chats
- Contacts
- Groups

Selecting a contact opens or creates a peer chat. Selecting a group opens the group chat.

## Chat Screen Fixes

The chat surface should improve without a full rewrite:

- Header displays the real peer/group title, avatar/status marker, and group member count/status subtitle when available.
- Incoming group messages show the sender display name above the bubble.
- Emoji picker is available next to the message input.
- Attachments use a bottom sheet/grid action layout instead of a dropdown menu.
- Photo/video/file/voice bubbles have clear fallback labels and loading/error states.

## Architecture

Add a focused inbox UI model layer rather than embedding aggregation logic in Composables.

Recommended types:

- `InboxRow`: sealed model for chat, contact, group, and system rows.
- `InboxSearchResult`: grouped search result model.
- `InboxUiState`: loading, loaded rows, search query/results, error.
- `InboxMapper` or small use case that combines chats, contacts, and groups into display rows.

`ChatsViewModel` should own the state and orchestration. Composables render state and send events only.

## Testing

Add JVM tests for pure model/mapper behavior:

- Groups appear in inbox even with no messages.
- `.Dev`/default contact appears in inbox even with no messages.
- Search returns matching chats, contacts, and groups.
- Empty state is false until first sync/load has completed.
- Group incoming message UI metadata includes sender display name.

Run at least:

- `./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.InboxMapperTest`
- `./gradlew assembleProductionDebug`

## Risks

- Existing `ChatsViewModel` currently derives rows from chat-only data; adding contacts/groups can expose stale Room rows if sync errors are swallowed.
- Navigation currently routes contact click through profile in some places; inbox contact selection must open/create chat.
- Media bubble improvements must avoid changing message encryption/send behavior.
- Generated/build files are dirty locally; implementation commits must stage only source, tests, docs, and required assets.
