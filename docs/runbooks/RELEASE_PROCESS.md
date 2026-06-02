# Release Process

1. Verify all release gates (RELEASE_READINESS.md)
2. `git checkout -b release/X.Y.Z`
3. Bump versionCode + versionName in app/build.gradle.kts
4. `./gradlew assembleRelease` (signed)
5. Internal track → 24h test
6. Alpha: 5% → 24h
7. Beta: 20% → 48h
8. Production: 1% → 5% → 20% → 50% → 100% (24h each)

## Stop Criteria
- Crash rate spike > 1% above baseline → halt
- ANR > 1% → halt

## Hotfix
1. Branch from release: `hotfix/description`
2. Fix + stage test
3. PR → release AND main
4. Emergency release to 100% if critical
