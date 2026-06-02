# ADR-003: Unified WebSocket Engine

**Date:** 2026-05-29 | **Status:** Accepted

## Context

Two parallel WS implementations existed:
- `WebSocketManager` — untyped `WebSocketEvent(type: String, data: String?)`
- `WebSocketService` — typed `WsEvent` sealed class

Both active → state divergence → missed events.

## Decision

Delete `WebSocketManager.kt` + `WebSocketEvent.kt`. Migrate `CallManager` to `WebSocketService`/`WsEvent`.

## Consequences

Single event stream, compiler-checked event handling, smaller DI graph.

## Alternatives Rejected

- Keep both + deprecate: would persist problem indefinitely
