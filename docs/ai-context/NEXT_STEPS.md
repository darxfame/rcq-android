# Next Steps

> Immediate tasks. Updated at end of each session.

## iOS Parity — Active

### Done ✅
- [x] Delete WebSocketManager + WebSocketEvent, migrate CallManager to WsEvent
- [x] Delete Games/Marketplace/Pets code and DB entities
- [x] Create docs structure
- [x] Write AI context files
- [x] Fix GitHub `assembleProductionDebug` duplicate AAR failure
- [x] Create `IOS_ANDROID_PARITY.md`
- [x] Align HTTP/WS transport base URL and `X-RCQ-Auth` support with iOS

### This Session
- [ ] Audit WebSocket typed event parity against iOS `WebSocketService.swift`
- [ ] Add focused tests for WebSocket event parsing
- [ ] Validate `assembleProductionDebug` after each parity fix
- [ ] Run online ADB validation on connected Android device

### This Week
- [ ] Crypto/message envelope parity audit and tests
- [ ] Auth/account/recovery parity audit
- [ ] Message ordering: use server envelope `serverTime` everywhere
- [ ] Sing-box runtime validation on device

### Next Week
- [ ] MVI refactor: ChatsViewModel + ChatViewModel
- [ ] Design tokens: SpacingTokens, ColorTokens, TypographyTokens
- [ ] GitHub Actions CI

## Priority Rule

1. iOS behavioral parity for active Android surfaces first
2. Security-sensitive parity: crypto, auth, WebSocket, message ordering
3. Device validation via ADB before broad feature expansion
