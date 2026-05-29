# Runbook: Crypto Session Reset

## When

- "Decryption failed" errors in logs
- Signal session corrupted after reinstall
- ECIES key not found

## Symptoms

```
E/RCQ_CRYPTO: ECIES decrypt failed for uin=123456789
E/RCQ_CRYPTO: No session found for address uin:1
```

## Recovery

### ECIES Key Missing
Keys in DataStore. If missing → re-registration or account recovery flow.

### Signal Session Corrupted
Session stored in `signal_keys` Room table.
Delete row WHERE address = 'uin:{target}' AND keyType = 'SESSION'.
Next message triggers new session via pre-key bundle fetch.

### Pre-key Exhaustion
Server returns null pre-key → cannot establish new ECIES session.
Fix: generate + upload new pre-keys via `AuthViewModel.uploadPreKeys()`.

## Prevention

- Log decrypt errors with uin + msgId (never log key material)
- Monitor decrypt failure rate in production
- Pre-key refresh when count drops below 10
