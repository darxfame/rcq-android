# Runbook: WebSocket Debug

## Symptoms

- Chat shows "offline" despite internet
- Messages not arriving in real time
- Logs show RECONNECTING loop

## Diagnosis

```bash
# ADB logcat filtering:
adb logcat | grep -E "WebSocket|OkHttp|RCQ_WS|WsEvent"

# Look for:
# "101 Switching Protocols" = success
# "500" = BUG-001 (auth header issue)
# "401" = token expired or wrong format
# CLOSE code = check close reason
```

## Common Causes

### HTTP 500 on connect (BUG-001)
- Check: `Authorization` header format — should be `Bearer <jwt>`
- Check: URL format — `wss://api.rcq.app/ws/{uin}`
- Compare with iOS ref WS connection headers

### Reconnect loop (connects → drops every few seconds)
- Check: `STALE_WATCHDOG_MS` in `WebSocketService.kt`
- Check: server-side idle timeout vs client ping interval (should be ping every 25s, server timeout 60s)

### Auth expired mid-session
- Check: JWT expiry claim (should be 30 days)
- Fix: implement token refresh before reconnect

## Files

- `data/websocket/WebSocketService.kt` — connection, backoff, watchdog
- `di/AppModule.kt` — OkHttpClient timeouts
- `di/AuthInterceptor.kt` — auth header injection
