# Pre-Auth Bypass Settings Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add pre-login connection bypass controls and stop Android from persisting broken auto sing-box routing.

**Architecture:** Use `ProxyManager` and `SingBoxTransport` as the only state owners. Add reusable UI state/adapters for a bottom sheet that can be opened from `WelcomeScreen` and `ConnectionProbeSplash`.

**Tech Stack:** Kotlin, Jetpack Compose, Hilt ViewModel, StateFlow, JUnit 4, Gradle productionDebug checks.

---

### Task 1: Stabilize Auto Bypass Policy

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/service/ProxyManager.kt`
- Test: `app/src/test/java/com/rcq/messenger/core/AutoBypassPolicyTest.kt`

- [x] **Step 1: Write the failing test**

```kotlin
assertFalse(
    AutoBypassPolicy.shouldRestoreEmbeddedTransport(
        bypassModeIsAuto = true,
        embeddedTransportWasActive = true,
        embeddedTransportExplicitlyEnabled = false
    )
)
```

- [x] **Step 2: Run test to verify it fails**

Run: `./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.AutoBypassPolicyTest`
Expected: FAIL with unresolved `AutoBypassPolicy`.

- [x] **Step 3: Implement minimal policy**

Add `AutoBypassPolicy.shouldRestoreEmbeddedTransport(...)` and use it in `ProxyManager.init`.

- [x] **Step 4: Run test to verify it passes**

Run: `./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.AutoBypassPolicyTest`
Expected: PASS.

### Task 2: Split Automatic vs Explicit Built-In Relay

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/ui/settings/StealthSettingsScreen.kt`
- Test: `app/src/test/java/com/rcq/messenger/core/AutoBypassPolicyTest.kt`

- [ ] **Step 1: Extend the policy test**

```kotlin
@Test
fun `automatic mode is not explicit embedded relay opt-in`() {
    assertFalse(AutoBypassPolicy.shouldPersistEmbeddedTransportForMode("AUTO"))
    assertTrue(AutoBypassPolicy.shouldPersistEmbeddedTransportForMode("BUILT_IN"))
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.AutoBypassPolicyTest`
Expected: FAIL with unresolved `shouldPersistEmbeddedTransportForMode`.

- [ ] **Step 3: Implement mode policy and update settings behavior**

`StealthViewModel.setBypassMode(BypassMode.AUTO)` must only set `proxyManager.bypassMode = AUTO`; it must not call `singBox.setEnabled(true)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.AutoBypassPolicyTest`
Expected: PASS.

### Task 3: Add Pre-Auth Bottom Sheet UI

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/ui/settings/ConnectionSettingsSheet.kt`
- Modify: `app/src/main/java/com/rcq/messenger/ui/auth/WelcomeScreen.kt`
- Modify: `app/src/main/java/com/rcq/messenger/ui/RCQApp.kt`

- [ ] **Step 1: Create reusable sheet composable**

Create `ConnectionSettingsSheet` with mode selector, custom proxy field, relay count, active/error status.

- [ ] **Step 2: Wire WelcomeScreen**

Add `onConnectionSettings: () -> Unit` to `WelcomeScreen` and render a compact `TextButton("Подключение")`.

- [ ] **Step 3: Wire ConnectionProbeSplash**

Add `onConnectionSettings: () -> Unit` to `ConnectionProbeSplash` and render the same action under the status line.

- [ ] **Step 4: Wire AuthNavigation modal state**

Own `showConnectionSettings` in `AuthNavigation`, open the sheet from welcome/splash, and use `hiltViewModel<StealthViewModel>()` for state.

### Task 4: Verify Build, CodeGraph, Device

**Files:**
- Modify: `docs/ai-context/CURRENT_STATE.md`
- Modify: `docs/ai-context/NEXT_STEPS.md`

- [ ] **Step 1: Run unit tests**

Run: `./gradlew testProductionDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Build APK**

Run: `./gradlew assembleProductionDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Sync CodeGraph**

Run: `codegraph sync .` then `codegraph status .`
Expected: `Index is up to date`.

- [ ] **Step 4: Install and smoke test**

Run: `adb install -r app/build/outputs/apk/production/debug/app-production-debug.apk`, launch app, open pre-login "Подключение", and capture relevant logcat.
