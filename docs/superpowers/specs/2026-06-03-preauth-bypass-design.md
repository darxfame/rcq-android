# Pre-Auth Bypass Settings Design

## Goal

Expose connection bypass controls before authentication so a user can recover connectivity when direct RCQ API access is blocked or when embedded sing-box is broken on the current network.

## Reference Behavior

iOS is the behavioral reference. `ProxyURLSheet` exposes manual override and automatic embedded transport before the user depends on online flows. `AppState` only auto-engages sing-box after reachability says the primary API is unavailable, and manual proxy/base URL overrides win.

Android must follow that policy:

- Off means direct RCQ API route only.
- Automatic means direct first, then embedded transport only after probe failures.
- Built-in relay means explicit embedded sing-box opt-in.
- Custom proxy means a user-provided HTTP/SOCKS proxy has priority.

## UX

Add a compact "Подключение" action on the welcome screen and connection probe splash. It opens a bottom sheet with:

- Current connection status.
- Mode selector: Automatic, Built-in relay, Custom proxy, Off.
- Custom proxy text field when Custom proxy is selected.
- Relay count and embedded sing-box status when Built-in relay or Automatic is selected.
- Existing error text from `SingBoxTransport.lastStartError`.

The sheet must be reusable from post-login settings later, but this task only wires it into pre-login screens.

## Architecture

Keep one source of truth:

- `ProxyManager` owns route mode and manual proxy.
- `SingBoxTransport` owns embedded transport enabled/active state.
- A connection settings ViewModel adapts those services to UI state.

Do not place business logic in composables. Do not make `AUTO` call `singBox.setEnabled(true)` directly; explicit built-in relay is the only path that persists `rcq.singbox.enabled=true`.

## Verification

Add JVM tests for route policy. Build `productionDebug`, run targeted unit tests, install on the connected device, open the welcome/splash sheet, and check logcat for removal of sticky SOCKS failures after restart.
