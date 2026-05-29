# Tech Debt Register

Score = Severity(1-5) × Frequency(1-5) × Ease(5=easy, 1=hard)

| ID | Item | Sev | Freq | Ease | Score | Status |
|---|---|---|---|---|---|---|
| TD-001 | Duplicate WS implementations | 5 | 5 | 5 | 125 | ✅ FIXED (Phase 0) |
| TD-002 | Message ordering (localtime vs serverTime) | 5 | 5 | 5 | 125 | 🔴 Phase 0 |
| TD-003 | No offline outbox queue | 5 | 5 | 3 | 75 | 🔴 Phase 0 |
| TD-004 | Dead code: Games/Pets/Marketplace | 3 | 5 | 5 | 75 | ✅ FIXED (Phase 0) |
| TD-005 | Group membership filter missing | 5 | 5 | 4 | 100 | 🔴 Phase 0 |
| TD-006 | No crash reporting | 4 | 2 | 5 | 40 | 🟡 Phase 1 |
| TD-007 | Fat ChatViewModel (no MVI) | 3 | 4 | 2 | 24 | 🟡 Phase 1 |
| TD-008 | No unit tests | 4 | 5 | 1 | 20 | 🟡 Ongoing |
| TD-009 | No design tokens (hardcoded values) | 3 | 3 | 3 | 27 | 🟡 Phase 1 |
| TD-010 | android.util.Log everywhere | 2 | 5 | 5 | 50 | 🟡 Phase 1 |
| TD-011 | No build variants (staging/prod) | 3 | 3 | 5 | 45 | 🟡 Phase 1 |
| TD-012 | No CI/CD pipeline | 4 | 2 | 4 | 32 | 🟡 Phase 1 |
| TD-013 | No screenshot tests | 3 | 2 | 3 | 18 | 🟡 Phase 2 |
| TD-014 | No feature flags | 3 | 2 | 4 | 24 | 🟡 Phase 1 |
| TD-015 | Certificate pinning missing | 4 | 1 | 4 | 16 | 🟡 Phase 1 |
