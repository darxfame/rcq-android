# Delivery States

## State Machine

```
PENDING → SENT → DELIVERED → READ
Any state → FAILED (max retries)
```

## Display (matching iOS)

| State | Indicator | Color |
|---|---|---|
| PENDING | ✓ gray | Gray |
| SENT | ✓ | Gray |
| DELIVERED | ✓✓ | Gray |
| READ | ✓✓ | Blue |
| FAILED | ⚠️ + retry | Red |

## WS Events

- `message_delivered` → DELIVERED
- `message_read` → READ

## Status: ⚠️ Partial

SENT works. DELIVERED/READ require WS (BUG-001 blocks).
