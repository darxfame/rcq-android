# ADR-001: Single Module Architecture

**Date:** 2026-05-20 | **Status:** Accepted

## Context

Project is pre-1.0. Multi-module Gradle requires significant upfront investment.

## Decision

All code in `:app`. No multi-module setup yet.

## Planned Evolution

Modularize after Phase 0:
1. `:core:crypto` — already logically isolated
2. `:core:network` — WS engine + Retrofit
3. `:feature:auth` — least coupled

## Alternatives Rejected

- Multi-module from day one: overhead not justified pre-1.0
- KMP: too early, no shared contracts yet
