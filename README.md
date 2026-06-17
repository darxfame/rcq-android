# RCQ Android

Native Android client for **RCQ**, a privacy-first messenger with numeric IDs,
no phone number, no email, and end-to-end encrypted messaging by default.

This repository is the active Android codebase aligned with the iOS reference
client. The old prototype branch has been replaced by the upstream-compatible
Compose app under `app.rcq.android`.

## Status

Active alpha. The app is distributed as a sideloaded APK, not through the Play
Store.

Current baseline:
- encrypted 1:1 and group chats with offline queue history drain
- account recovery, multi-account storage, QR/deep links, and local encrypted DB
- UnifiedPush background delivery through distributors such as ntfy, without FCM
- push tap routing to chats when the payload exposes a peer or group target
- WebRTC calls, audio rooms, local relay transport, panic PIN, and reproducible
  release builds

Intentional non-scope for the Android baseline: games, marketplace, and pets.

## Links

- Main site: <https://rcq.app>
- iOS reference client: <https://github.com/rcq-messenger/rcq-ios>
- Upstream Android repo: <https://github.com/rcq-messenger/rcq-android>
- Reproducible builds: [docs/REPRODUCIBLE-BUILDS.md](docs/REPRODUCIBLE-BUILDS.md)

## Build

Use the committed Gradle wrapper and JDK 21.

```bash
./gradlew compileDebugKotlin
./gradlew test
./gradlew assembleDebug
```

The app expects `app/libs/rcqbox.aar` for the embedded relay transport. Release
builds are configured for reproducibility and ABI-split APKs.

## Project Layout

- `app/src/main/java/app/rcq/android` - Android app source
- `app/src/test` - unit tests
- `docs/REPRODUCIBLE-BUILDS.md` - release verification notes
- `tools/verify-apk.sh` - APK content comparison helper

## License

Source code is licensed under [GNU AGPL-3.0](LICENSE). Any network-facing
service built from this code must publish its modifications under the same
license. See [NOTICE](NOTICE) for third-party attributions.

## Security

If you find a security vulnerability, please disclose responsibly via
`security@rcq.app` before filing a public issue.
