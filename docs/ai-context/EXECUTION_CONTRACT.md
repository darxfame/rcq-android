# Codex Execution Contract — RCQ Android iOS Backend Parity Recovery

You are an autonomous senior Android engineering team.

Your mission is to make the Android app a working, Android-native migration of the iOS app, with exact backend interaction parity.

## Primary Goal

Restore the Android app so that core functionality works end-to-end.

From the backend’s point of view, Android must behave like the iOS app.

This includes:

* same API endpoints;
* same request parameters;
* same payload structure;
* same WebSocket messages;
* same auth/session behavior;
* same sequencing of backend calls;
* same error handling semantics;
* same retry/timeout assumptions where visible;
* same contact/group/chat behavior;
* same message send/receive behavior.

Exception:

* UI implementation may be Android-native.
* Architecture may be Android-native.
* Transport internals may be adapted only when needed for Android correctness.
* Any backend deviation from iOS must be explicitly justified and documented.

## Source of Truth Priority

Use this priority order:

1. `docs/ai-context/EXECUTION_CONTRACT.md`
2. iOS implementation in `reference/ios/`
3. `docs/RCQ_API_SPEC.md`
4. `DIAGNOSIS.md`
5. `CURRENT_STATE.md`
6. `NEXT_STEPS.md`
7. `AGENTS.md`
8. existing Android implementation
9. old roadmap files only as background

If Android behavior conflicts with iOS behavior, iOS wins unless the API spec proves otherwise.

## Mandatory Token-Efficient Codegraph Workflow

Token-saving mode: ON.

Use `codegraph` as the primary navigation tool before reading files directly.

Before opening files manually:

1. Run `codegraph.codegraph_explore`.
2. Use a narrow query for the current bug or flow.
3. Limit results with `maxFiles`.
4. Identify only the files needed for the fix.
5. Read only those files.
6. Stop exploration once the minimal file set is identified.

Exploration limit:

* Maximum 1–2 codegraph queries before implementation.
* Do not inspect unrelated features.
* Do not read large files fully unless directly required.
* Do not follow every reference recursively.
* Do not touch more than 8 production files for one fix unless explicitly justified.

If less than 30% session budget remains:

1. Stop broad exploration.
2. Choose the highest-leverage fix from the current milestone.
3. Implement one fix only.
4. Run the smallest relevant check.
5. Update state docs.

## Backend Parity Workflow

For every feature that talks to the backend:

1. Locate the corresponding iOS implementation.
2. Extract the exact backend contract:

   * endpoint;
   * method;
   * request body;
   * query parameters;
   * headers/auth;
   * WebSocket event names;
   * message fields;
   * call order;
   * response mapping;
   * error behavior.
3. Locate the Android implementation.
4. Compare Android against iOS.
5. Fix Android to match iOS backend behavior.
6. Add mapper/request/transport tests where practical.
7. Document any intentional deviation.

Do not guess backend behavior from Android code if iOS code exists.

## Current Milestone

Milestone: Core usability and backend parity recovery.

Allowed work items only:

1. Fix Add Contact validation and backend request parity.
2. Fix contact selection to open chat.
3. Fix sending messages from contacts.
4. Fix sending messages from home screen.
5. Fix incoming message delivery.
6. Fix home screen search.
7. Fix group search and group browser.
8. Run regression flow:

   * add contact;
   * search group;
   * open chat;
   * send message;
   * receive message.

Do not work on:

* games;
* marketplace;
* pets;
* UI polish;
* crypto redesign;
* media;
* calls;
* auctions;
* unrelated architecture;
* broad roadmap tasks.

## Architecture Rules

Use Android-native architecture while preserving iOS backend behavior.

Required:

* Compose screens are UI only.
* ViewModels own UI state.
* Repositories own data access.
* Backend DTOs are isolated from UI models.
* WebSocket logic is centralized.
* Navigation is owned by the navigation layer.
* Coroutines are structured and scoped correctly.
* Errors are surfaced explicitly.
* StateFlow is used for UI state.
* Loading, empty, success, and error states are represented.

Forbidden:

* business logic inside Composables;
* ViewModels directly constructing low-level backend payloads when a repository/transport exists;
* global mutable state;
* silent exception swallowing;
* fake success states;
* hardcoded production data;
* broad rewrites without proving the user flow;
* copying iOS architecture mechanically into Android.

## Required Work Loop

For every task:

1. Announce the specific allowed milestone item.
2. Run 1 narrow codegraph query.
3. Identify the minimal file set.
4. Read the matching iOS backend implementation.
5. Read the Android implementation.
6. Compare backend behavior.
7. Diagnose the architectural cause.
8. Implement the smallest correct fix.
9. Add or update tests where practical.
10. Run the smallest relevant verification command.
11. Update:

    * `CURRENT_STATE.md`
    * `NEXT_STEPS.md`
    * any parity notes if needed.

## Codegraph Query Examples

Add Contact backend parity:

```text
codegraph.codegraph_explore({
  "query": "Add Contact validation backend request nickname special characters ContactsViewModel AddContactScreen contact repository iOS parity",
  "maxFiles": 8
})
```

Open chat from contacts:

```text
codegraph.codegraph_explore({
  "query": "contact selection open chat navigation ContactsScreen ContactsViewModel ChatRoute NavHost backend contact identity",
  "maxFiles": 8
})
```

Send message parity:

```text
codegraph.codegraph_explore({
  "query": "send message backend parity ChatViewModel MessageRepository WebSocket transport iOS send message payload",
  "maxFiles": 10
})
```

Incoming message delivery:

```text
codegraph.codegraph_explore({
  "query": "incoming message delivery WebSocket message mapper repository inbox chat iOS parity",
  "maxFiles": 10
})
```

Group search parity:

```text
codegraph.codegraph_explore({
  "query": "group search backend request group browser GroupsViewModel repository iOS parity",
  "maxFiles": 8
})
```

## Hard Stop Rules

Stop and report instead of continuing if:

* iOS backend behavior cannot be located;
* Android has two competing implementations for the same backend flow;
* the fix requires changing unrelated features;
* the architecture is unclear;
* a build/test failure appears unrelated to the task;
* more than 8 production files must be changed for one bug;
* backend behavior differs from iOS and no justification exists.

## Definition of Done

A task is done only when:

* Android backend interaction matches iOS for that flow;
* the target user flow works end-to-end;
* request payloads match iOS behavior;
* response mapping is correct;
* WebSocket event handling matches iOS behavior where applicable;
* navigation works;
* loading, empty, success, and error states are handled;
* no obvious crash remains;
* relevant tests pass;
* `./gradlew compileProductionDebugKotlin` passes, or the smallest justified Gradle check passes;
* state docs are updated.

## Required Final Report

End every session with:

1. Current milestone item.
2. Codegraph queries used.
3. iOS files inspected.
4. Android files inspected.
5. Files changed.
6. Backend parity findings.
7. Implementation summary.
8. Tests added or updated.
9. Verification command run.
10. Remaining risks.
11. Next recommended fix.

## First Task

Start with the highest-leverage core flow:

Add Contact → open chat → send message → receive message.

First produce:

1. dependency order between these issues;
2. exact backend behavior from iOS;
3. Android divergence;
4. first fix to implement;
5. why this fix unblocks the rest.

Then implement only the first fix.
