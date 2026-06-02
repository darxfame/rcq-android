# Test Matrix — Phase 0 Exit Criteria

| Test | Expected | Status |
|---|---|---|
| WS connects | 101 Switching Protocols in logcat | 🔴 |
| Send DM | Message appears, delivered | ⚠️ |
| Receive DM (iOS sends) | Appears in real time | 🔴 |
| Message order | Send order = display order | 🔴 |
| Offline send → reconnect | Message delivered | 🔴 |
| Group filter | Only joined groups shown | 🔴 |
| Registration | New account created | ✅ |
| Push notification | Received in background | ✅ |
| App survives rotation | No crash | ✅ |
| App survives kill+restore | State preserved | ⚠️ |
