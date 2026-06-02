# Release Readiness

**Updated:** 2026-05-29 | **Score: 2/16 gates**

## Gate 1 — Code Complete

- [ ] BUG-001 fixed (WS 500)
- [ ] BUG-002 fixed (message ordering)
- [ ] BUG-003 fixed (group filter)
- [ ] Offline outbox implemented
- [ ] No P0 open issues

## Gate 2 — QA Sign-off

- [ ] Manual test matrix passed
- [ ] Cross-platform iOS↔Android test passed
- [ ] No regression vs previous build

## Gate 3 — Stability Metrics

- [ ] Crash-free ≥ 99.5%
- [ ] ANR ≤ 0.5%
- [ ] WS reconnect ≤ 2s

## Gate 4 — Staged Rollout

- [ ] Internal → 1% → 5% → 20% → 100%

## Production SLAs

| Metric | Target |
|---|---|
| Crash-free rate | ≥ 99.5% |
| ANR rate | ≤ 0.5% |
| Cold start | ≤ 1.5s |
| WS reconnect | ≤ 2s |
| Message send lag | ≤ 100ms local, ≤ 500ms server ACK |
| Push delivery | ≥ 95% in 60s |
