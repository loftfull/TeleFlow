# TeleFlow Preview-Only Policy Change Plan

Date: 2026-08-24

## Context receipt and gate

- Project: `loftfull/TeleFlow` recovery checkout (not registered in the canonical project registry)
- Canonical recovery root: `C:\Users\Vladimir\Documents\Codex\2026-08-24\referenced-chatgpt-conversation-this-is-an-3\work\teleflow-audit`
- Branch / starting commit: `teleflow/bootstrap` / `e77cb7b13071f665edac8575b2adea0f912c5ed2`
- Mode: continuation of the explicitly authorized recovery
- Approved outcome: continue fixing without new features and use independent reviewers
- Must preserve: buildable read-only Smart Folders preview, existing UI/API/data formats, disabled mutations, exact Telegram upstream pin
- Must not do: enable mutations, alter visible design, merge PR #1, or represent debug artifacts as releases
- Need: proven
- Timing: now
- Reuse: existing Python `unittest` suite and GitHub Actions
- Design fit: pass; no UI change
- Critic: `GO_WITH_CONDITIONS`; conditions 1-5 are included below, branch ruleset remains an external activation condition
- Approval: covered by the owner's instruction to continue corrective work; no product, design, architecture, or data-contract change is introduced
- Rollback: one isolated commit; no migration or runtime state

## Problem and outcome

The current preview-only guarantee is file-local. A new Java overlay file or Java embedded in the patcher can call a Telegram write sink without passing through `TelegramFolderGateway`; the existing 43 tests would remain green and the patcher would ship that file in the APK.

This slice adds a repository-wide, secret-free policy gate for a conservative denylist of known Telegram folder-mutation sinks. It does not claim to detect arbitrary dynamically constructed Java and does not change runtime application code. The policy fails closed on known sinks and runs on pull requests targeting `English` or `teleflow/bootstrap`, and on pushes to `teleflow/bootstrap`.

## Six audits

### Product

Live folder mutation is outside the approved preview scope. A preventive gate protects the current product truth without adding behavior.

### Architecture

The runtime kill switch remains defense in depth, while the new repository boundary prevents alternative mutation transports from being packaged unnoticed. No public interface or data flow changes.

### Structure

The scanner belongs in `scripts/`, its self-tests in `tests/`, and the enforcement entry point in a dedicated workflow. It scans copied Java overlays and the patcher source for known sink identifiers. Raw-text denylisting intentionally accepts conservative false positives and cannot prove the absence of dynamically assembled identifiers.

### Technical

Regex matching is whitespace-tolerant and reports exact file, line, and rule. The gateway is an explicit narrow allowlist: one disabled constant and exactly two guarded `sendRequest` call sites. Its normalized content is additionally sealed with SHA-256 so control-flow or comment tricks cannot preserve a false green result. Any gateway change therefore requires a separate reviewed policy update. No dependency is added.

### Design

No visual, localization, navigation, theme, or accessibility behavior changes.

### Quality

Acceptance requires observed RED before policy implementation, scanner self-tests for known bypasses, full local suite, exact overlay application, policy workflow CI, and independent post-change review. Branch protection must require the workflow before the barrier can be called merge-enforced.

## Reuse research and options

| Option | Evidence / status | Decision |
|---|---|---|
| Keep file-local tests | Existing tests only inspect the UI and gateway | Reject: bypass remains |
| Reuse Python `unittest` plus a lightweight workflow | Existing stack; no dependency or product change | Select |
| Delete dormant mutation stack now | Stronger runtime reduction but changes a broad internal architecture surface | Defer to a separately approved refactor |
| Add an external SAST product | Adds supply-chain/configuration cost and still needs project-specific rules | Defer; no-fit for this small invariant |
| Build a Java parser from scratch | Excessive cost for a temporary fail-closed policy | Reject |

Official GitHub guidance supports least-privilege secret-free PR checks, concurrency cancellation, and full-SHA Action pins:

- https://docs.github.com/en/actions/reference/security/secure-use
- https://docs.github.com/en/actions/concepts/security/compromised-runners
- https://docs.github.com/en/actions/concepts/workflows-and-actions/concurrency

## Critic conditions and resolutions

1. Use whitespace-tolerant regexes rather than literal sink strings — included.
2. Require one disabled constant, exactly two gateway calls, and a guard before every call — included.
3. Cover `overlay/**/*.java` and `scripts/apply_teleflow.py` — included.
4. The dedicated workflow must run scanner and workflow contract tests — included.
5. Add synthetic bypass tests and a clean-repository baseline test — included.
6. Confirm this workflow as a required check for `teleflow/bootstrap` — external repository setting; until confirmed, merge/release remain `NO_GO`.

## TDD milestones

1. RED: add scanner/workflow contract tests while their implementation files do not exist.
2. GREEN: implement the minimal scanner and secret-free workflow.
3. Run focused tests, full suite, Python compile check, YAML/static checks, and `git diff --check`.
4. Apply the overlay to the exact pinned Telegram checkout and run the policy on the applied Java output where feasible.
5. Push one isolated commit and require a green policy workflow.
6. Request independent post-change review.

## Acceptance and rollback

- Whitespace variants, static/unqualified `saveFilterToServer`, a sink in a new Java file, and a third gateway call are rejected.
- Existing repository baseline passes.
- Workflow contains no secrets or artifact upload and uses read-only permissions, concurrency, and a full-SHA checkout pin.
- Existing smoke and manual full-debug workflows remain unchanged.
- Rollback: revert the single policy commit and remove the required-check rule if it was configured.
