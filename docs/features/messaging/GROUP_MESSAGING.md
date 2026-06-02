# Group Messaging

## Architecture
ECIES fan-out: message encrypted separately for each member's public key.

## Current Bugs
- BUG-003: GroupDao returns all server groups — needs membership filter
- Incoming blocked by BUG-001 (WS 500)

## Fix BUG-003
```sql
SELECT g.* FROM groups g
WHERE g.id IN (SELECT group_id FROM group_members WHERE user_uin = :ownUin)
```

## Edge Cases
- New member: cannot decrypt historical messages (by design, correct)
- Member removed: further sends fail gracefully
