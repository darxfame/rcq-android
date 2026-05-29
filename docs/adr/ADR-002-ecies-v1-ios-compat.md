# ADR-002: ECIES v=1 Wire Format

**Date:** 2026-05-22 | **Status:** Accepted

## Decision

Use ECIES v=1 (ChaCha20-Poly1305 + Curve25519 + Ed25519) via BouncyCastle full provider.

## Why

iOS requires this exact format. Different format = cannot decrypt cross-platform.

## Critical Constraints

- bcprov-jdk18on:1.78.1 pinned — do not upgrade without iOS validation
- 32-byte Curve25519 key format locked
- Envelope structure (version + ephemeral key + ciphertext + mac) locked

## Alternatives Rejected

- AES-256-GCM: iOS uses ChaCha20 — incompatible
- Android Keystore for ECIES private key: doesn't expose raw bytes for ECDH
