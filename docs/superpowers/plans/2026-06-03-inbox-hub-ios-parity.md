# Android Inbox Hub UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a unified Android inbox hub with iOS-compatible contacts/groups behavior, global search, and targeted chat surface fixes for identity, emoji, and media actions.

**Architecture:** Add a focused pure Kotlin inbox UI model/mapper and let `ChatsViewModel` expose `InboxUiState`. Keep Compose rendering thin and avoid moving business logic into Composables. Chat surface changes are scoped to UI metadata and controls, not message transport/encryption.

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Hilt ViewModels, StateFlow, Room-backed repositories, JUnit 4, kotlinx-coroutines-test.

---

## File Structure

- Create `app/src/main/java/com/rcq/messenger/ui/chat/inbox/InboxModels.kt`: sealed row/result/state models.
- Create `app/src/main/java/com/rcq/messenger/ui/chat/inbox/InboxMapper.kt`: pure aggregation and search logic.
- Create `app/src/test/java/com/rcq/messenger/core/InboxMapperTest.kt`: JVM tests for rows/search/empty-state behavior.
- Modify `app/src/main/java/com/rcq/messenger/ui/chat/ChatsViewModel.kt`: collect chats, contacts, groups and expose `InboxUiState`.
- Modify `app/src/main/java/com/rcq/messenger/ui/chat/ChatsScreen.kt`: render unified inbox and grouped search.
- Modify `app/src/main/java/com/rcq/messenger/ui/RCQApp.kt`: ensure inbox contact rows open/create chat instead of profile.
- Modify `app/src/main/java/com/rcq/messenger/ui/chat/ChatScreen.kt`: improve header, group sender labels, emoji affordance, and attachment sheet.
- Modify `docs/ai-context/CURRENT_STATE.md`, `docs/ai-context/KNOWN_ISSUES.md`, `docs/ai-context/NEXT_STEPS.md`: record UI parity progress.

## Task 1: Inbox Mapper Models and Tests

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/ui/chat/inbox/InboxModels.kt`
- Create: `app/src/main/java/com/rcq/messenger/ui/chat/inbox/InboxMapper.kt`
- Test: `app/src/test/java/com/rcq/messenger/core/InboxMapperTest.kt`

- [ ] **Step 1: Write failing mapper tests**

Create tests that assert:

```kotlin
@Test
fun groupsAppearWithoutMessages() {
    val rows = InboxMapper().buildRows(
        chats = emptyList(),
        contacts = emptyList(),
        groups = listOf(group(id = "21", name = "RCQ Beta")),
        devUin = 1_000_000_001L,
        hasLoadedOnce = true
    )

    assertThat(rows.map { it.title }).contains("RCQ Beta")
}

@Test
fun devContactAppearsWithoutMessages() {
    val rows = InboxMapper().buildRows(
        chats = emptyList(),
        contacts = listOf(contact(userId = 1_000_000_001L, nickname = ".Dev")),
        groups = emptyList(),
        devUin = 1_000_000_001L,
        hasLoadedOnce = true
    )

    assertThat(rows.map { it.title }).contains(".Dev")
}

@Test
fun searchFindsChatsContactsAndGroups() {
    val mapper = InboxMapper()
    val rows = mapper.buildRows(
        chats = listOf(chat(id = "peer:42", title = "Alice")),
        contacts = listOf(contact(userId = 77L, nickname = "Bob")),
        groups = listOf(group(id = "21", name = "RCQ Beta")),
        devUin = 1_000_000_001L,
        hasLoadedOnce = true
    )

    val results = mapper.search(rows, "bo")

    assertThat(results.contacts.map { it.title }).contains("Bob")
    assertThat(results.chats.map { it.title }).doesNotContain("Alice")
    assertThat(results.groups.map { it.title }).doesNotContain("RCQ Beta")
}
```

- [ ] **Step 2: Run tests and verify failure**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.InboxMapperTest
```

Expected: fails because `InboxMapper` and models do not exist.

- [ ] **Step 3: Implement minimal models and mapper**

Add `InboxRow`, `InboxSearchResults`, and `InboxMapper` with deterministic ordering:

1. Existing chats with last message first.
2. Groups not already represented by a chat.
3. Contacts not already represented by a chat.

Search matches title, subtitle, and ID/UIN text.

- [ ] **Step 4: Run tests and verify pass**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.InboxMapperTest
```

Expected: all tests pass.

## Task 2: ViewModel State Integration

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/chat/ChatsViewModel.kt`
- Test: extend `app/src/test/java/com/rcq/messenger/core/InboxMapperTest.kt` if model behavior changes

- [ ] **Step 1: Inspect current `ChatsViewModel` flows**

Confirm current chat flow source and sync behavior. Keep existing chat sync behavior that avoids `/chats`.

- [ ] **Step 2: Add `InboxUiState` exposure**

Collect chats, contacts, and groups in `ChatsViewModel`, call `InboxMapper.buildRows`, and expose:

```kotlin
val inboxState: StateFlow<InboxUiState>
fun updateSearchQuery(query: String)
fun refreshInbox()
```

- [ ] **Step 3: Preserve current public API temporarily**

Keep existing `chats` and `isLoading` StateFlows until `ChatsScreen` is migrated, so this task is independently buildable.

- [ ] **Step 4: Run focused tests and compile**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.InboxMapperTest
./gradlew compileProductionDebugKotlin
```

Expected: tests and Kotlin compile pass.

## Task 3: Main Inbox UI

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/chat/ChatsScreen.kt`
- Modify: `app/src/main/java/com/rcq/messenger/ui/RCQApp.kt`

- [ ] **Step 1: Replace chat-only rendering with `InboxUiState`**

Render rows from `inboxState.rows`. Row click dispatches by target type:

```kotlin
when (row.target) {
    is InboxTarget.Chat -> onChatClick(row.target.chatId)
    is InboxTarget.Contact -> onContactChatClick(row.target.userId)
    is InboxTarget.Group -> onGroupClick(row.target.groupId)
}
```

- [ ] **Step 2: Add grouped search results**

When query is non-empty, render sections `Chats`, `Contacts`, `Groups` from `InboxSearchResults`.

- [ ] **Step 3: Add loading/empty rules**

Show empty state only when `hasLoadedOnce == true`, not loading, and rows are empty.

- [ ] **Step 4: Compile**

Run:

```bash
./gradlew compileProductionDebugKotlin
```

Expected: Kotlin compile passes.

## Task 4: Chat Surface Identity, Emoji, Media

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/chat/ChatScreen.kt`
- Possibly modify: `app/src/main/java/com/rcq/messenger/ui/chat/components/MediaMessageBubble.kt`

- [ ] **Step 1: Improve header metadata**

Display title, subtitle, and avatar/status marker from ViewModel data. Use group member count for groups when available.

- [ ] **Step 2: Show sender label for incoming group messages**

For incoming group messages, render sender display name above the bubble. For own messages and direct chats, do not show redundant sender labels.

- [ ] **Step 3: Replace attachment dropdown with bottom sheet**

Use a modal bottom sheet with actions: Photo, Video, File, Location. Keep existing launchers and send methods.

- [ ] **Step 4: Add emoji input affordance**

Add an emoji button beside the text input. Open the existing `EmoticonPicker` or a compact emoji panel and insert selected text into `messageText`.

- [ ] **Step 5: Compile**

Run:

```bash
./gradlew compileProductionDebugKotlin
```

Expected: Kotlin compile passes.

## Task 5: Device Validation and Docs

**Files:**
- Modify: `docs/ai-context/CURRENT_STATE.md`
- Modify: `docs/ai-context/KNOWN_ISSUES.md`
- Modify: `docs/ai-context/NEXT_STEPS.md`

- [ ] **Step 1: Build APK**

Run:

```bash
./gradlew assembleProductionDebug
```

Expected: build succeeds.

- [ ] **Step 2: Install and inspect with logcat**

Run:

```bash
adb install -r app/build/outputs/apk/production/debug/app-production-debug.apk
adb logcat -c
adb shell monkey -p com.rcq.messenger 1
adb logcat -d
```

Expected: no fatal crash; logs show contacts/groups sync; UI should expose `RCQ Beta` and `.Dev` in inbox/search when synced.

- [ ] **Step 3: Update AI context docs**

Record implemented UI parity and any remaining issues.

- [ ] **Step 4: CodeGraph refresh**

Run:

```bash
codegraph clean .
codegraph init .
codegraph index .
codegraph sync .
codegraph status .
```

Expected: status reports an up-to-date index.

- [ ] **Step 5: Commit and push**

Stage only source/tests/docs/required assets, then:

```bash
git commit -m "Android: обновить главный экран чатов"
git push origin ios-parity-transport-build
```

Expected: remote branch advances to the new commit.

## Self-Review

- Spec coverage: Task 1 covers pure inbox behavior; Task 2 wires state; Task 3 implements main screen/search; Task 4 implements chat identity/emoji/media; Task 5 verifies and updates docs.
- Placeholder scan: no `TBD`/`TODO` requirements are used as plan steps.
- Type consistency: model names are consistent across tasks: `InboxRow`, `InboxTarget`, `InboxSearchResults`, `InboxUiState`, `InboxMapper`.
