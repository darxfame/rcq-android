# iOS parity notes

This file tracks intentional Android/iOS differences that are easy to mistake
for missing implementation.

## Push previews

Decision: Android does not decrypt message pushes in the UnifiedPush service.

iOS has a Notification Service Extension plus `PushDecryptCache`: the extension
can decrypt a v2 payload for a rich preview, then hand the plaintext to the main
app so the libsignal ratchet is not consumed twice.

Android UnifiedPush runs inside the app process, but decrypting there would
advance the same v2 ratchet before the normal offline-queue ingest path sees the
ciphertext. Recreating the iOS cache safely would require a second durable
plaintext handoff tied to account selection, panic-PIN state, queue ack, and DB
wipe. That is not worth owning while the current path already preserves message
delivery:

- push shows generic server-provided text
- notification taps route to the peer or group when payload routing fields exist
- app startup drains the offline queue
- queue rows are acknowledged only after successful ingest

Skipped: rich push plaintext previews. Add only if users need previews more than
the simpler, safer delivery path.

## Premium media paywall

Decision: Android keeps media upload free (`pay_jetons=0`) and does not expose a
premium photo/video paywall yet.

iOS currently has leftover strings and key-wrap helpers for this flow, but the
active attachment sheet has no premium action and the codebase has no live
`/premium/contents` compose/unlock path. Shipping Android-only paid media now
would invent product behavior instead of matching iOS.

Skipped: paid media compose/unlock. Add only after iOS/backend ship the real
premium content API and UX.
