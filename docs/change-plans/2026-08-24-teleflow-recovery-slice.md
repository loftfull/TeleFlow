# TeleFlow Recovery Slice Change Plan

Date: 2026-08-24

## Decision status

- Need: proven
- Timing: now
- Reuse: adapt the pinned official Telegram `LocaleController.getString` pattern
- Design fit: pass for preview-only bootstrap scope
- Critic: `GO_WITH_CONDITIONS`; all pre-edit conditions below are recorded and accepted
- Approval: the owner explicitly directed the team to attempt repair and to use independent agents
- Rollback: one isolated remediation commit; no data migration

## Authority and exact working root

This recovery iteration is authorized only for the exact Git checkout at:

`C:\Users\Vladimir\Documents\Codex\2026-08-24\referenced-chatgpt-conversation-this-is-an-3\work\teleflow-audit`

Repository identity: `https://github.com/loftfull/TeleFlow.git`

Branch: `teleflow/bootstrap`

Starting commit: `49703400b80e8a6fcd7bfdcab93eb8249d0b599e`

TeleFlow is not currently registered in `D:\MOZG\Projects\project-knowledge-registry.json`. This checkout is therefore an explicit recovery worktree for the named GitHub repository and branch, not a silent registration or relocation of the product. No Obsidian registry, project folder, remote branch, or PR will be changed in this slice.

## Problem and outcome

The pinned Android build fails with 26 `cannot find symbol getString(int)` errors across all three TeleFlow `BaseFragment` screens. Separately, Smart Folders exposes a server mutation path that can remove folder members when the local dialog cache is incomplete.

The outcome of this slice is deliberately narrow:

1. Make all TeleFlow `BaseFragment` screens use the official pinned Telegram localization API.
2. Keep Smart Folders as read-only preview.
3. Remove the UI-to-mutation call path.
4. Add a second fail-closed gateway kill switch so dormant code cannot send folder mutation RPCs.
5. Prove the slice with RED/GREEN regression tests and a pinned-upstream Android build.

## Non-goals

- No new product features.
- No mutation redesign or re-enablement.
- No data-contract or persisted-state migration.
- No branding, signing, release, dependency, or CI architecture expansion.
- No claim that PR #1 is ready to merge or release.

## Six audits

### Product

The compile repair is required immediately. Preview-only behavior matches the original bootstrap specification; live Apply does not. The smallest useful safe outcome is a buildable internal preview, not a partially repaired mutation feature.

### Architecture

The pinned upstream `BaseFragment` is not an Android `Fragment` or `Context` and has no `getString`. The existing upstream pattern is the static `LocaleController.getString` API. Mutation code remains dormant and unapproved; UI reachability plus a gateway kill switch establish defense in depth without inventing a transaction architecture.

### Structure

Localization compatibility belongs in the three TeleFlow UI files. Safety containment belongs at the UI boundary and the two RPC dispatch points in `TelegramFolderGateway`. No upstream Telegram source is modified beyond the existing deterministic overlay process.

### Technical

No dependency or helper is required. The fix reuses an API present in the exact pinned upstream commit. The gateway guard must execute before either `sendRequest`. A real Gradle build remains the authoritative compile gate.

### Design

The active Apply control is removed rather than cosmetically disabled. The screen keeps a localized passive read-only status. Other visual debt remains explicitly out of scope.

### Quality

Tests are written and observed RED before production edits. Acceptance requires the focused tests, full bootstrap suite, patch application to the pinned upstream, and the real arm64 debug build. The existing Windows portability failures are recorded separately and are not hidden.

## Reuse and alternatives

| Option | Decision | Evidence |
|---|---|---|
| Keep current implementation | Reject | It does not compile and exposes unsafe Apply. |
| Adapt official Telegram pattern | Select | Pinned upstream `CallLogActivity` uses `import static org.telegram.messenger.LocaleController.getString;`. |
| Qualify all 26 calls | Reject | Correct but creates unnecessary call-site churn compared with the upstream static-import pattern. |
| Add `getString` to upstream `BaseFragment` | Reject | Broad upstream modification and wrong ownership boundary. |
| Add-only mutation | Reject for this slice | Full-filter RPC can still overwrite concurrent changes and keeps identity/recovery risks. |
| Full transaction redesign | Defer | Requires separate architecture, threat-model, migration, and rollback approval. |
| Replace Telegram client or build from scratch | Reject | No-fit evidence is absent; it increases protocol, security, and maintenance risk. |

## Independent reviews

- Android compile specialist: `GO_WITH_CONDITIONS`; official static import in all three screens, test first, full build required.
- Mutation-safety specialist: current mutation path `NO_GO`; preview-only UI plus gateway kill switch `GO_WITH_CONDITIONS`.
- Independent governance/red-team critic: `GO_WITH_CONDITIONS`; scope must remain compile compatibility plus fail-closed Apply removal, and the build must be green before any merge discussion.

## TDD milestones

1. RED: localization contract covers every TeleFlow `BaseFragment` screen and rejects a missing official static import.
2. GREEN: add only the official static import to the three screens.
3. RED: UI contract rejects every Apply/callback path and gateway contract requires a compile-time disabled guard before both RPC sends.
4. GREEN: remove Apply UI/callback code, update passive status text, and add the gateway guard.
5. Run the focused tests, then all bootstrap tests.
6. Apply the overlay to a clean exact pinned upstream and run `:TMessagesProj_App:assembleAfatDebug` for arm64.
7. Request an independent post-change review before any completion claim.

## Acceptance criteria

- No unqualified `getString` call lacks the official static import in any TeleFlow `BaseFragment` screen.
- `TeleFlowSmartFoldersActivity` has no `SmartFoldersBatchApply`, Apply button, confirmation, or mutation callback.
- Both folder mutation RPC paths return `mutations-disabled` before `sendRequest`.
- Read-only preview still renders.
- Tests and build results are reported exactly; failures are not relabeled as success.
- PR #1 remains unmerged and no artifact is represented as release-ready.

## Rollback

Revert the single remediation commit. No persisted data migration is introduced. Re-enabling mutation is prohibited until a separate approved mutation design closes incomplete-input, RPC outcome, account identity, crash recovery, concurrency, and multi-device risks.
