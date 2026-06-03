# Repository Guidelines

## Project Structure & Module Organization

This is a single-module Android project. Application code lives in `app/src/main/java/com/rcq/messenger`, with packages for `crypto`, `data`, `domain`, `di`, `service`, `ui`, and `util`. Compose screens and theme code are under `ui/`; Room, repositories, DTOs, and WebSocket code are under `data/`. Resources are in `app/src/main/res`, assets are in `app/src/main/assets`, and bundled AAR/JAR files are loaded from `app/libs`. Unit tests live in `app/src/test`; instrumented and Compose UI tests live in `app/src/androidTest`. Documentation is under `docs/`.

## Build, Test, and Development Commands

- `./gradlew assembleDebug` builds the debug APK for local validation.
- `./gradlew assembleProductionDebug` builds the production debug APK used by CI and ADB validation.
- `./gradlew compileProductionDebugKotlin` runs a faster Kotlin compile check for the production flavor.
- `./gradlew kspProductionDebugKotlin` runs Room and Hilt annotation processing for the production flavor.
- `./gradlew testProductionDebugUnitTest` runs local JVM unit tests for the production flavor.
- `./gradlew connectedAndroidTest` runs instrumented tests on a connected emulator or device.
- `./gradlew lint` runs Android lint checks.

Use `stagingDebug` or `productionDebug` variants when a task needs an explicit flavor, for example `./gradlew assembleStagingDebug`.

## Coding Style & Naming Conventions

Use Kotlin with 4-space indentation and Java 17 compatibility. Follow package boundaries: keep UI state and composables in `ui`, persistence and network concerns in `data`, services in `service`, and dependency wiring in `di`. Name composables with PascalCase nouns or actions, such as `ChatScreen` or `MessageBubble`; name view models and managers with explicit suffixes, such as `SettingsViewModel` or `ProxyManager`. Prefer immutable data classes, coroutine-friendly APIs, and Hilt constructor injection.

## Testing Guidelines

Use JUnit 4 for local tests and AndroidX Test, Espresso, and Compose UI Test for instrumented coverage. Test files should end in `Test.kt`, for example `MessageOrderingTest` or `SignalSessionTest`. Add JVM tests for pure Kotlin, crypto, ordering, repository, and retry logic. Use instrumented tests for Room, Android security APIs, Compose UI behavior, and device-dependent flows.

## Commit & Pull Request Guidelines

Recent commits use short, imperative summaries, often prefixed by scope, for example `Android: group media albums` or `Add large binaries to gitignore`. Keep the first line specific and under 72 characters. Pull requests should include a description, test results, linked issue or roadmap item, and screenshots or recordings for UI changes. Call out database migrations, security-sensitive changes, and bundled binary updates explicitly.

## Security & Configuration Tips

Do not commit local secrets, keystores, or machine-specific SDK paths. Keep `local.properties` local. Treat E2EE, account recovery, relay, and panic/PIN code as security-sensitive; include targeted tests and update relevant docs in `docs/features`, `docs/runbooks`, or `docs/adr` when behavior changes.

## Agent-Specific Instructions

Read `docs/ai-context/AI_CONTEXT.md`, `CURRENT_STATE.md`, `KNOWN_ISSUES.md`, `NEXT_STEPS.md`, and `DECISIONS_LOG.md` before architectural or feature work. Use `docs/RCQ_API_SPEC.md` as the backend contract and `reference/ios/` as the behavioral source of truth when available. Do not implement Games, Marketplace, Pets, or features absent from the iOS reference. Keep WebSocket work centralized in `data/websocket/WebSocketService.kt`; do not reintroduce `WebSocketManager`. For message ordering, use server envelope time instead of local wall-clock time. For Room schema changes, add explicit migrations and never use destructive fallback. At the end of substantive sessions, update the relevant `docs/ai-context` files. Current development should proceed from `main`, aligned to `origin/phase-1-core-messaging-clean`.

## Autonomous Migration Recovery Workflow

Use `superpowers`/multi-agent collaboration for substantive migration, architecture, feature, and regression work. Treat the active Android app as a partially migrated product whose behavior must be checked against the iOS reference, then translated into idiomatic Android rather than copied mechanically.

Before broad code changes, perform a short audit of module structure, Gradle setup, app entry point, navigation, screen/ViewModel architecture, state management, dependency injection, networking, local storage, business logic, iOS migration artifacts, tests, TODOs, known crashes, and broken flows. Produce a concise migration health report with broken areas, architecture problems, risky files, first fixes, and items to defer.

Target architecture:
- Kotlin-first single-module Android app until a module split is clearly justified.
- MVVM for screens, Compose + Material 3 for UI, Navigation Compose for navigation.
- Hilt for dependency injection, repositories for data access, Retrofit/OkHttp for network, Room/DataStore for persistence.
- Use cases only for non-trivial business logic.
- UI state exposed as immutable `StateFlow`; no business logic in Composables or Activities.
- Clear package boundaries: `ui`, `domain`, `data`, `di`, `service`, `crypto`, `util`.

Avoid global mutable state, mixed navigation ownership, ViewModels directly calling low-level APIs when a repository belongs there, silent error swallowing, unstructured coroutine launches, massive god classes, and hardcoded production test data. Remove or move unused migration artifacts out of active Gradle source roots once confirmed unused; do not hide stale app code with custom source-set hacks.

For each iOS-backed feature, identify the iOS behavior, current Android behavior, missing or incorrect behavior, the Android-native implementation, and tests that prove it works. Work incrementally: choose the smallest coherent fix, refactor only as much as needed, add or update tests, run checks, review the result, and document changes.

Definition of done for a recovery task:
- End-to-end behavior works for loading, success, empty, and error states.
- Navigation and state handling are correct.
- Coroutine scopes and data flow are understandable.
- No obvious crashes remain.
- Critical behavior has tests where practical.
- `assembleProductionDebug` and relevant unit/KSP/compile checks pass.
- Existing behavior is not knowingly broken.

Testing expectations: add JVM tests for pure Kotlin, mappers, repository retry/error behavior, crypto, ordering, and ViewModel state when practical. Use instrumented or Compose tests for Room, Android security APIs, navigation, and critical UI flows. Prefer JUnit, kotlinx-coroutines-test, and Flow testing helpers when available; add minimal test dependencies only when the existing stack cannot cover the behavior.

Every substantive autonomous session should report: current findings, architecture diagnosis, implementation plan, files changed, risks, changes made, tests added or updated, checks run, remaining issues, and the next recommended step.
