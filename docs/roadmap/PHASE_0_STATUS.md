# Phase 0 — Emergency Stabilization

**Started:** 2026-05-29 | **Target:** 2026-06-05

## Tasks

| Task | Status | Notes |
|---|---|---|
| Delete WebSocketManager.kt | ✅ Done | CallManager migrated to WsEvent |
| Delete WebSocketEvent.kt | ✅ Done | |
| Delete GameRepository.kt | ✅ Done | |
| Delete GamesScreen.kt | ✅ Done | |
| Delete MarketplaceScreen.kt | ✅ Done | |
| Delete Game.kt, Marketplace.kt | ✅ Done | |
| Delete PetEntity.kt, PetDao.kt | ✅ Done | |
| DB migration 11→12 (drop pets) | ✅ Done | |
| Update AppModule.kt | ✅ Done | Remove WebSocketManager, PetDao |
| Update RCQDatabase.kt | ✅ Done | Remove PetEntity, version→12 |
| Update RCQApp.kt | ✅ Done | Remove Games/Marketplace routes |
| Create docs structure | ✅ Done | |
| Fix BUG-001: WS 500 | 🔴 Pending | |
| Fix BUG-002: Message ordering | 🔴 Pending | |
| Fix BUG-003: Group filter | 🔴 Pending | |
| Offline outbox table + processor | 🔴 Pending | |

## Exit Criteria

- [ ] WS connects (no 500)
- [ ] Messages in server_time order
- [ ] Groups show only member groups
- [ ] Offline messages delivered on reconnect
- [ ] Build passing, 0 dead code
