# Signal Protocol + ECIES

## Files
- `crypto/EciesCrypto.kt` — encrypt/decrypt
- `crypto/EciesKeyStore.kt` — key storage
- `crypto/CryptoService.kt` — facade (use this, not internals)
- `crypto/SessionManager.kt` — Signal sessions

## Critical Constraints
- bcprov-jdk18on:1.78.1 pinned — no upgrade without iOS validation
- Envelope: version byte + ephemeral key + ciphertext + mac (LOCKED)
- Key format: 32-byte compressed Curve25519 (LOCKED)

## Key Lifecycle
1. Register → generate Curve25519 + Ed25519 keys
2. Upload to server /keys/upload
3. Per-message: fetch /keys/prekey/{uin} → ECDH → derive → ChaCha20-Poly1305
