# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RCQ Android is a privacy-first messenger with 9-digit IDs, no phone/email requirements, and end-to-end encryption by default. The project is currently in Phase 1 (Core Messaging) with libsignal E2EE integration complete.

**Key Technologies:**
- Kotlin + Jetpack Compose UI
- Hilt dependency injection
- Room database with migrations
- libsignal-android for E2EE (Signal Protocol)
- OkHttp + Retrofit for networking
- WebSocket for real-time messaging
- Kotlinx Serialization for JSON

## Build Commands

### Environment Setup
```bash
# Set Android SDK path (required for builds)
export ANDROID_HOME=/opt/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Accept SDK licenses if needed
yes | sdkmanager --licenses
```

### Build Commands
```bash
# Clean build
./gradlew clean

# Debug build
./gradlew assembleDebug

# Release build  
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Lint check
./gradlew lint

# KSP compilation only (faster for checking Entity/DAO issues)
./gradlew kspDebugKotlin
```

### Development Commands
```bash
# Install debug APK
./gradlew installDebug

# Run specific test class
./gradlew test --tests "com.rcq.messenger.crypto.CryptoServiceTest"

# Check dependencies
./gradlew dependencies

# Generate Room schema
./gradlew kspDebugKotlin --rerun-tasks
```

## Architecture Overview

### Layer Structure
```
ui/           - Jetpack Compose screens and components
├── chat/     - Chat interface, message bubbles, typing indicators
├── auth/     - Registration, login, recovery flows  
├── contacts/ - Contact management, friend requests
└── common/   - Shared UI components and theme

data/         - Data layer with repositories and data sources
├── api/      - Retrofit API interfaces and DTOs
├── db/       - Room database, DAOs, and entities
├── ws/       - WebSocket manager for real-time events
└── repository/ - Repository implementations

domain/       - Business logic and models
├── model/    - Entity classes and data models
└── repository/ - Repository interfaces

crypto/       - E2EE implementation using libsignal
├── CryptoService     - Main encryption/decryption service
├── SignalKeyStore    - libsignal key management
└── SessionManager    - Signal Protocol session handling

di/           - Hilt dependency injection modules
call/         - WebRTC calling functionality
service/      - Background services
util/         - Utility functions and extensions
```

### Key Components

**Database (Room v7):**
- All entities support E2EE with `ciphertext`, `signalType`, `isEncrypted` fields
- Migration v6→v7 adds Signal Protocol support
- Separate DAO files for each entity (UserDao, MessageDao, etc.)

**E2EE Integration:**
- Uses libsignal-android for Signal Protocol (Double Ratchet)
- `CryptoService` handles encryption/decryption with automatic fallback
- `SignalKeyStore` manages identity keys, pre-keys, and sessions
- Full iOS client compatibility via identical Signal Protocol implementation

**Real-time Communication:**
- `WebSocketManager` handles connection lifecycle with exponential backoff
- Event-driven architecture with 40+ typed WebSocket events
- Automatic reconnection and keepalive pings

**API Integration:**
- Base URL: `https://api.rcq.app/`
- JWT authentication with 30-day TTL
- Full RCQ API spec implementation (see `docs/RCQ_API_SPEC.md`)

## Development Guidelines

### Database Changes
- Always create Room migrations for schema changes
- Test migrations with existing data
- Update entity classes in `domain/model/` package
- Ensure DAO imports match entity locations

### E2EE Implementation
- All new message types must support encryption via `CryptoService`
- Use `SessionManager` for Signal Protocol operations
- Test encryption/decryption with iOS client compatibility
- Never store unencrypted sensitive data

### WebSocket Events
- Add new event types to `WebSocketEvent` companion object
- Handle events in appropriate repositories
- Maintain real-time UI updates via Flow/StateFlow

### UI Development
- Use Material 3 design system with custom RCQ theme
- Follow existing Compose patterns in `ui/common/`
- Implement proper state management with ViewModels
- Support both light and dark themes

### Testing Strategy
- Unit tests for crypto components are critical
- Integration tests for database migrations
- UI tests for critical user flows (auth, messaging)
- Test E2EE compatibility with reference implementations

## Common Issues

### Build Failures
- **KSP errors:** Usually indicate missing Entity imports in DAO files
- **Hilt errors:** Check for duplicate `@Provides` functions in DI modules  
- **Room errors:** Verify entity field names match DAO query column names
- **libsignal errors:** Ensure native libraries are properly included

### Development Setup
- Android SDK 34 required with build-tools 34.0.0
- Minimum SDK 26 (Android 8.0)
- Java 17 for compilation
- KSP instead of kapt for annotation processing

### Git Workflow
- Main development on `phase-1-core-messaging` branch
- Commit messages in Russian (project requirement)
- Push changes after each completed task
- Use conventional commit format when possible

## Project Status

**Phase 1 Complete (May 26, 2026):**
- ✅ libsignal E2EE integration
- ✅ Signal Protocol compatibility with iOS
- ✅ Database migration v7 with E2EE fields
- ✅ WebRTC calling infrastructure
- ✅ Real-time messaging with WebSocket
- ✅ UI enhancements (message status, typing indicators)

**Current Focus:**
- Build system stability and error resolution
- UI polish and user experience improvements
- Performance optimization and testing
- Preparation for Phase 2 (Advanced Features)

## API Reference

See `docs/RCQ_API_SPEC.md` for complete API documentation including:
- Authentication endpoints (`/auth/*`)
- User management (`/users/*`)
- Contact operations (`/contacts/*`)
- Cryptographic key exchange (`/keys/*`)
- Message delivery (`/messages/*`)
- Real-time events via WebSocket