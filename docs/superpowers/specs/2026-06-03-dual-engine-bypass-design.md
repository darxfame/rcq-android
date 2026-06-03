# Dual-Engine Embedded Bypass Design

## Goal

Make Android embedded bypass support modern relay links, especially `vless://...type=xhttp...security=reality`, without breaking existing sing-box-compatible relays or direct/manual proxy behavior.

## Current Finding

The current APK bundles `app/libs/libbox.aar` and starts sing-box through `SingBoxTransport`. Device logcat shows sing-box fails before startup when the priority USA relay is included:

```text
decode config: outbounds[1].transport: unknown transport type: xhttp
```

This is not only an old-binary problem. Mainline sing-box documents VLESS with a generic V2Ray transport block, but its transport set does not include XHTTP/SplitHTTP. XHTTP is implemented in Xray-core family clients. Therefore a newer mainline `libbox.aar` can improve sing-box features, but it is not the correct engine for the user-provided xHTTP relay.

## Target Architecture

Android should use a dual-engine transport layer:

- `EmbeddedTransport`: small interface used by `ProxyManager` and UI.
- `SingBoxEmbeddedTransport`: current sing-box implementation, used for sing-box-compatible relays such as plain VLESS+Reality and Hysteria2.
- `XrayEmbeddedTransport`: Xray-core process wrapper, used for VLESS+Reality+xHTTP and future Xray-native transports.
- `RelaySelectionPolicy`: pure Kotlin policy that chooses supported relays by engine capability, never by string order alone.

The public behavior remains a local SOCKS proxy at `127.0.0.1:1089`. App networking does not need to know which engine is active.

## Engine Choice

Use Xray-core for xHTTP. A gomobile AAR was rejected for the first integration because both Xray AARs and `libbox.aar` contain the same `go.*` runtime classes while loading different native libraries. The first supported Android implementation therefore packages the official Xray Android executable as a native ABI artifact and runs it as a child process.

Do not rely on random APK-extracted native libraries.

## Data Flow

1. `RelayConfigRepository` returns relay entries with protocol, security, transport type, and priority.
2. `RelaySelectionPolicy` partitions relays by engine support:
   - Xray supports VLESS+Reality+xHTTP.
   - sing-box supports current sing-box-compatible relays.
3. `ProxyManager` asks the embedded transport coordinator to start the best available route.
4. Active engine starts a local SOCKS inbound.
5. Android validates `BuildConfig.API_BASE_URL/health` through the local SOCKS proxy before reporting `isActive=true`.
6. OkHttp/WebSocket use the same `ProxyManager.currentProxy()` path as today.

## Error Handling

- If an engine cannot parse a relay config, that relay is marked unsupported for that engine before startup.
- If an engine starts but `/health` through SOCKS fails, stop the engine and persist a user-visible `lastStartError`.
- If xHTTP is configured but Xray is unavailable, show a clear diagnostics message instead of silently falling back to an incompatible engine.

## Testing

Add JVM tests for:

- Relay capability routing picks Xray for xHTTP.
- Relay capability routing excludes xHTTP from sing-box.
- Last-good relay cannot promote an unsupported relay above a supported one.
- Coordinator reports inactive when selected engine fails `/health`.

Add device verification:

- Build and install `productionDebug`.
- Start app with direct DNS blocked or after failure threshold.
- Confirm logcat has no `unknown transport type: xhttp`.
- Confirm local SOCKS validation either passes with the Xray engine or reports the exact Xray startup error.

## Scope Boundaries

This spec does not change message, contacts, or group algorithms. It only restores a correct embedded bypass foundation so those flows can be validated against iOS afterward.
