# Account Recovery

## Status: Client designed + partial impl, backend TODO

## Solution
24-word BIP39 mnemonic encrypts private key (PBKDF2 + AES-256-GCM).
Recovery = enter mnemonic → derive key → Ed25519 challenge-response.

## Files
- `crypto/MnemonicHelper.kt` — mnemonic + key derivation ✅
- `ui/auth/AccountRecoveryScreen.kt` — UI ✅
- Backend: challenge-response endpoint ❌ not implemented
