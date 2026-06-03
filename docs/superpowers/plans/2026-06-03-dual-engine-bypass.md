# Dual-Engine Embedded Bypass Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an Android embedded bypass architecture that supports both sing-box-compatible relays and Xray-native VLESS+Reality+xHTTP relays.

**Architecture:** Introduce a small transport abstraction above native cores. Keep sing-box for existing relays, add Xray/libXray for xHTTP, and centralize relay capability selection before native config generation.

**Tech Stack:** Kotlin, Hilt, OkHttp SOCKS validation, gomobile AARs (`libbox.aar`, Xray/libXray AAR), JUnit 4, adb logcat.

---

## File Structure

- Modify `app/src/main/java/com/rcq/messenger/service/SingBoxTransport.kt`: keep sing-box engine, remove xHTTP responsibility.
- Create `app/src/main/java/com/rcq/messenger/service/EmbeddedTransport.kt`: common interface and result types.
- Create `app/src/main/java/com/rcq/messenger/service/EmbeddedTransportCoordinator.kt`: chooses Xray vs sing-box and exposes one local SOCKS proxy state.
- Create `app/src/main/java/com/rcq/messenger/service/XrayTransport.kt`: Xray engine wrapper through reflection so app can compile when AAR is absent during intermediate work.
- Modify `app/src/main/java/com/rcq/messenger/service/RelayConfigRepository.kt`: keep relay parsing; do not hide xHTTP relays globally.
- Modify `app/src/main/java/com/rcq/messenger/service/ProxyManager.kt`: depend on coordinator instead of starting sing-box directly.
- Modify `app/src/main/java/com/rcq/messenger/ui/settings/StealthSettingsScreen.kt` and `ConnectionSettingsSheet.kt`: show active engine and xHTTP availability.
- Modify `app/build.gradle.kts`: add Xray AAR dependency when available.
- Add `app/src/test/java/com/rcq/messenger/core/RelayEngineSelectionPolicyTest.kt`.
- Add `app/src/test/java/com/rcq/messenger/core/EmbeddedTransportCoordinatorTest.kt`.
- Add `docs/runbooks/android-xray-aar.md`: reproducible build/source notes for the native library.

## Task 1: Verify Native Engine Requirement

**Files:**
- Modify: `docs/runbooks/android-xray-aar.md`

- [ ] **Step 1: Record the current failure evidence**

Create `docs/runbooks/android-xray-aar.md` with:

```markdown
# Android Xray AAR Runbook

## Current Failure

The current embedded sing-box AAR cannot parse the user-provided relay:

```text
decode config: outbounds[1].transport: unknown transport type: xhttp
```

## Required Capability

The Android embedded bypass must support VLESS + REALITY + XHTTP as used by:

```text
vless://63bbedb0-2e27-4a15-9aca-b0856d5f9b3a@80.209.243.23:443?...&security=reality&type=xhttp&xhttpMode=auto
```

Mainline sing-box should remain available for sing-box-compatible relays. XHTTP relays require an Xray-family engine.
```

- [ ] **Step 2: Check available local native artifacts**

Run:

```bash
find app/libs -maxdepth 2 -type f -printf "%p %s\n"
unzip -l app/libs/libbox.aar
```

Expected: `libbox.aar` is present. Xray AAR is absent unless a previous task already added it.

- [ ] **Step 3: Fetch or build Xray AAR**

Preferred source:

```bash
git clone https://github.com/XTLS/libXray /tmp/libXray
cd /tmp/libXray
python3 build/main.py android
```

If the local toolchain is missing Go, gomobile, Android SDK, or NDK, document the missing component in `docs/runbooks/android-xray-aar.md` and use a source-verifiable release artifact only after recording URL, version, checksum, and license.

- [ ] **Step 4: Copy the verified AAR**

Copy the built or verified AAR to:

```text
app/libs/libxray.aar
```

Do not remove `app/libs/libbox.aar`.

## Task 2: Add Relay Engine Selection Policy

**Files:**
- Test: `app/src/test/java/com/rcq/messenger/core/RelayEngineSelectionPolicyTest.kt`
- Modify: `app/src/main/java/com/rcq/messenger/service/SingBoxTransport.kt`

- [ ] **Step 1: Write failing tests**

Add tests:

```kotlin
package com.rcq.messenger.core

import com.rcq.messenger.service.RelayEntry
import com.rcq.messenger.service.RelaySelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class RelayEngineSelectionPolicyTest {
    @Test
    fun `xhttp relay is assigned to xray when xray is available`() {
        val relay = RelayEntry(
            tag = "relay-usa-amd-xhttp",
            proto = "vless",
            server = "80.209.243.23",
            port = 443,
            sni = "amd.com",
            uuid = "63bbedb0-2e27-4a15-9aca-b0856d5f9b3a",
            public_key = "YQL5CMcuLgjJwH-2f10LlWx79ZDMnRzl8oZAFPPUqmk",
            short_id = "2a3f5c8d",
            transport_type = "xhttp",
            transport_path = "/telemetry",
            xhttp_mode = "auto",
            priority = -100
        )

        val selected = RelaySelectionPolicy.selectForEmbeddedTransport(
            listOf(relay),
            lastGoodTag = null,
            xrayAvailable = true
        )

        assertEquals("xray", selected.engine)
        assertEquals("relay-usa-amd-xhttp", selected.relays.first().tag)
    }

    @Test
    fun `xhttp relay is not assigned to sing-box when xray is unavailable`() {
        val relays = listOf(
            RelayEntry(
                tag = "xhttp",
                proto = "vless",
                server = "80.209.243.23",
                port = 443,
                sni = "amd.com",
                uuid = "uuid",
                public_key = "public",
                short_id = "sid",
                transport_type = "xhttp",
                priority = -100
            ),
            RelayEntry(
                tag = "vless",
                proto = "vless",
                server = "165.22.90.214",
                port = 443,
                sni = "www.yandex.ru",
                uuid = "uuid",
                public_key = "public",
                short_id = "sid",
                priority = 1
            )
        )

        val selected = RelaySelectionPolicy.selectForEmbeddedTransport(
            relays,
            lastGoodTag = null,
            xrayAvailable = false
        )

        assertEquals("sing-box", selected.engine)
        assertEquals("vless", selected.relays.first().tag)
    }
}
```

- [ ] **Step 2: Run tests to verify RED**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.RelayEngineSelectionPolicyTest
```

Expected: compile failure for missing `selectForEmbeddedTransport`.

- [ ] **Step 3: Implement minimal policy**

Add to `RelaySelectionPolicy`:

```kotlin
data class EmbeddedRelaySelection(
    val engine: String,
    val relays: List<RelayEntry>
)

fun selectForEmbeddedTransport(
    base: List<RelayEntry>,
    lastGoodTag: String?,
    xrayAvailable: Boolean
): EmbeddedRelaySelection {
    val xrayRelays = base
        .filter { it.transport_type.equals("xhttp", ignoreCase = true) }
        .sortedBy { it.priority }
    if (xrayAvailable && xrayRelays.isNotEmpty()) {
        return EmbeddedRelaySelection("xray", promoteLastGood(xrayRelays, lastGoodTag))
    }
    return EmbeddedRelaySelection(
        "sing-box",
        orderForAndroid(base, lastGoodTag, supportsXhttp = false)
    )
}
```

- [ ] **Step 4: Run tests to verify GREEN**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.RelayEngineSelectionPolicyTest
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.RelaySelectionPolicyTest
```

Expected: both pass.

## Task 3: Add Transport Abstraction

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/service/EmbeddedTransport.kt`
- Create: `app/src/main/java/com/rcq/messenger/service/EmbeddedTransportCoordinator.kt`
- Modify: `app/src/main/java/com/rcq/messenger/service/SingBoxTransport.kt`
- Create: `app/src/test/java/com/rcq/messenger/core/EmbeddedTransportCoordinatorTest.kt`

- [ ] **Step 1: Write failing coordinator test**

Add:

```kotlin
package com.rcq.messenger.core

import com.rcq.messenger.service.EmbeddedTransport
import com.rcq.messenger.service.EmbeddedTransportCoordinator
import com.rcq.messenger.service.RelayEntry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddedTransportCoordinatorTest {
    @Test
    fun `coordinator starts xray for xhttp relay when available`() = runBlocking {
        val xray = FakeTransport(engine = "xray", available = true)
        val singBox = FakeTransport(engine = "sing-box", available = true)
        val coordinator = EmbeddedTransportCoordinator(
            singBox = singBox,
            xray = xray
        )

        coordinator.startBest(
            relays = listOf(
                RelayEntry(
                    tag = "xhttp",
                    proto = "vless",
                    server = "80.209.243.23",
                    port = 443,
                    sni = "amd.com",
                    uuid = "uuid",
                    public_key = "public",
                    short_id = "sid",
                    transport_type = "xhttp",
                    priority = -100
                )
            ),
            lastGoodTag = null
        )

        assertEquals("xray", coordinator.activeEngine)
        assertEquals(1, xray.startCount)
        assertEquals(0, singBox.startCount)
    }

    private class FakeTransport(
        override val engine: String,
        override val available: Boolean
    ) : EmbeddedTransport {
        var startCount = 0
        override val isActive: Boolean get() = startCount > 0
        override val lastStartError: String? = null
        override suspend fun start(relays: List<RelayEntry>): Boolean {
            startCount += 1
            return true
        }
        override fun stop() {}
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.EmbeddedTransportCoordinatorTest
```

Expected: compile failure for missing interface/coordinator.

- [ ] **Step 3: Implement interface and coordinator**

Create `EmbeddedTransport.kt`:

```kotlin
package com.rcq.messenger.service

interface EmbeddedTransport {
    val engine: String
    val available: Boolean
    val isActive: Boolean
    val lastStartError: String?
    suspend fun start(relays: List<RelayEntry>): Boolean
    fun stop()
}
```

Create `EmbeddedTransportCoordinator.kt`:

```kotlin
package com.rcq.messenger.service

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddedTransportCoordinator @Inject constructor(
    private val singBox: SingBoxTransport,
    private val xray: XrayTransport
) {
    var activeEngine: String? = null
        private set

    val isActive: Boolean get() = singBox.isActive || xray.isActive
    val lastStartError: String? get() = xray.lastStartError ?: singBox.lastStartError

    suspend fun startBest(relays: List<RelayEntry>, lastGoodTag: String?): Boolean {
        val selected = RelaySelectionPolicy.selectForEmbeddedTransport(
            base = relays,
            lastGoodTag = lastGoodTag,
            xrayAvailable = xray.available
        )
        val transport = if (selected.engine == "xray") xray else singBox
        val other = if (selected.engine == "xray") singBox else xray
        other.stop()
        val ok = transport.start(selected.relays)
        activeEngine = if (ok) transport.engine else null
        return ok
    }

    fun stop() {
        singBox.stop()
        xray.stop()
        activeEngine = null
    }
}
```

- [ ] **Step 4: Adapt `SingBoxTransport`**

Make `SingBoxTransport : EmbeddedTransport` and add:

```kotlin
override val engine: String get() = "sing-box"
override val available: Boolean get() = isEngineAvailable
override suspend fun start(relays: List<RelayEntry>): Boolean {
    startWithRelays(relays)
    return isActive
}
```

Move existing no-argument `start()` to call `startWithRelays(relayConfigRepository.currentRelays())`.

- [ ] **Step 5: Run coordinator test**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.EmbeddedTransportCoordinatorTest
```

Expected: pass.

## Task 4: Add Xray Transport Wrapper

**Files:**
- Create: `app/src/main/java/com/rcq/messenger/service/XrayTransport.kt`
- Modify: `app/build.gradle.kts`
- Test: `app/src/test/java/com/rcq/messenger/core/XrayConfigBuilderTest.kt`

- [ ] **Step 1: Add Xray AAR dependency**

Modify `app/build.gradle.kts`:

```kotlin
implementation(files("libs/libxray.aar"))
```

Keep `implementation(files("libs/libbox.aar"))`.

- [ ] **Step 2: Write config builder test**

Create a pure Kotlin config builder inside `XrayTransport.kt` and test that xHTTP fields are emitted:

```kotlin
assert(config.contains("\"network\":\"xhttp\""))
assert(config.contains("\"security\":\"reality\""))
assert(config.contains("\"path\":\"/telemetry\""))
```

- [ ] **Step 3: Implement Xray config shape**

Generate Xray JSON with:

```json
{
  "inbounds": [{
    "listen": "127.0.0.1",
    "port": 1089,
    "protocol": "socks",
    "settings": { "udp": true }
  }],
  "outbounds": [{
    "protocol": "vless",
    "settings": {
      "vnext": [{
        "address": "80.209.243.23",
        "port": 443,
        "users": [{ "id": "63bbedb0-2e27-4a15-9aca-b0856d5f9b3a", "encryption": "none" }]
      }]
    },
    "streamSettings": {
      "network": "xhttp",
      "security": "reality",
      "realitySettings": {
        "serverName": "amd.com",
        "fingerprint": "random",
        "publicKey": "YQL5CMcuLgjJwH-2f10LlWx79ZDMnRzl8oZAFPPUqmk",
        "shortId": "2a3f5c8d"
      },
      "xhttpSettings": {
        "path": "/telemetry",
        "mode": "auto"
      }
    }
  }]
}
```

- [ ] **Step 4: Implement reflection startup**

Use reflection so compilation can report a clear `available=false` if class names differ. Start with the class names documented by the chosen AAR. If the AAR API differs, update `docs/runbooks/android-xray-aar.md` with exact methods.

- [ ] **Step 5: Validate through SOCKS**

After Xray starts, reuse the same `/health` through `127.0.0.1:1089` validation used by sing-box. If validation fails, stop Xray and persist `lastStartError`.

## Task 5: Wire Coordinator Into ProxyManager and UI

**Files:**
- Modify: `app/src/main/java/com/rcq/messenger/service/ProxyManager.kt`
- Modify: `app/src/main/java/com/rcq/messenger/ui/settings/StealthSettingsScreen.kt`
- Modify: `app/src/main/java/com/rcq/messenger/ui/settings/ConnectionSettingsSheet.kt`

- [ ] **Step 1: Replace direct sing-box start**

In `ProxyManager.reportFailure`, replace:

```kotlin
singBoxTransport.start()
```

with:

```kotlin
embeddedTransportCoordinator.startBest(
    relays = relayConfigRepository.currentRelays(),
    lastGoodTag = null
)
```

- [ ] **Step 2: Preserve proxy address behavior**

Keep:

```kotlin
Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", SingBoxTransport.LOCAL_PORT))
```

but base active state on `embeddedTransportCoordinator.isActive`.

- [ ] **Step 3: Surface active engine**

Settings should display:

```text
Relay engine: Xray
```

or:

```text
Relay engine: sing-box
```

when embedded bypass is active.

## Task 6: Build, Install, and Verify

**Files:**
- Modify if needed: `docs/ai-context/CURRENT_STATE.md`
- Modify if needed: `docs/ai-context/KNOWN_ISSUES.md`
- Modify if needed: `docs/ai-context/NEXT_STEPS.md`

- [ ] **Step 1: Build**

Run:

```bash
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.RelayEngineSelectionPolicyTest
./gradlew testProductionDebugUnitTest --tests com.rcq.messenger.core.EmbeddedTransportCoordinatorTest
./gradlew assembleProductionDebug
```

Expected: all pass.

- [ ] **Step 2: Update CodeGraph**

Run:

```bash
codegraph sync .
codegraph status .
```

Expected: index is up to date.

- [ ] **Step 3: Install and clear logcat**

Run:

```bash
adb install -r app/build/outputs/apk/production/debug/app-production-debug.apk
adb logcat -c
adb shell am start -n com.rcq.messenger/.ui.MainActivity
```

- [ ] **Step 4: Check logcat**

Run:

```bash
adb logcat -d | rg -n "XrayTransport|SingBoxTransport|ProxyManager|unknown transport type|/messages/queue|/contacts|/groups|HTTP FAILED|SOCKS|FATAL EXCEPTION" -C 2
```

Expected:

- No `unknown transport type: xhttp`.
- No `FATAL EXCEPTION`.
- If bypass is needed, log shows `XrayTransport` selected for the USA xHTTP relay.
- `/messages/queue` returns HTTP 200 or a clearly diagnosed server/network error.

## Self-Review

- Spec coverage: xHTTP support, sing-box preservation, capability selection, validation, diagnostics, tests, and device QA are covered.
- Placeholder scan: no unresolved TBD/TODO placeholders.
- Type consistency: `EmbeddedTransport`, `EmbeddedTransportCoordinator`, `RelaySelectionPolicy`, and `XrayTransport` names are consistent across tasks.
