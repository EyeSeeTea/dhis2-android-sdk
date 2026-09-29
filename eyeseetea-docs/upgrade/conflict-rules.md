# SDK Conflict Resolution Rules

Operational guide for resolving merge conflicts when bringing changes from Oslo
upstream (`dhis2/dhis2-android-sdk`) into this fork.

This file is meant to be reusable by any agent or model in future sessions.

## Purpose

This document is **not** the canonical customization inventory.

Its purpose is:
- define how conflicts should be resolved
- record which files are conflict-prone and why
- stay reusable across upgrades

The canonical customization inventory lives in `eyeseetea-docs/customizations.md`.

## Document split

- `customizations.md`
  Final-state inventory. Only keep confirmed EyeSeeTea customizations that
  still exist on top of the Oslo baseline this fork tracks.

- `conflict-rules.md` (this file)
  Working merge guide. Keep reusable rules here only.

- `upgrade/upgrade-<version>-notes.md`
  Temporary upgrade notes. Keep version-specific progress, decisions, and
  unresolved questions there while an Oslo upgrade is active. Delete once
  the upgrade is stabilized. Copy from `upgrade/template/upgrade-notes-template.md`.

- `upgrade/validation-checklist.md`
  Manual validation flows per confirmed customization, for the parts the
  automated suite alone doesn't cover with enough confidence.

## Model: one fork, no client layer

This repository does have a branch named `develop-eyeseetea` (the EyeSeeTea
fork branch, periodically updated by merging a tagged Oslo release — see
`upgrade-plan.md` — never `upstream/develop` directly), but — unlike
`dhis2-android-capture-app-extra`, where `develop-eyeseetea` is a *shared
baseline* with per-client branches on top of it — here `develop-eyeseetea`
is the fork itself, with nothing further layered on it. **One layer: Oslo
upstream → this fork.** There is no per-client branch and no per-client
inventory here — every customization in this repo is shared by every
consumer of the published artifact (`com.github.EyeSeeTea:dhis2-android-sdk`).

Do not import the app's client/flavor machinery into this repo: no
`AGENTS-<client>.md`, no `customizations/<client>/`, no per-client
validation checklist. If a rule here happens to read like it could apply
"per client," it doesn't — there is only one fork.

All conflict decisions should answer this question:

> What is the minimum EyeSeeTea-specific logic that must survive on top of
> the upstream Oslo version being merged?

## Core principles

1. Prefer upstream Oslo by default.
   If a change is not clearly an EyeSeeTea customization, keep the upstream
   version.

2. Reapply minimal deltas, not whole old files.
   When a shared file conflicts, start from the upstream version and
   reinsert only the EyeSeeTea-specific behavior.

3. Do not use conflict resolution as documentation.
   Temporary merge notes go to `upgrade/upgrade-<version>-notes.md`; stable
   confirmed customizations go to `customizations.md`.

4. If a customization is already absorbed by upstream Oslo, do not keep a
   duplicate fork copy of it.

5. A future agent must be able to continue from the docs alone.
   Keep decisions explicit, short, and file-based.

6. When possible, isolate custom code at the end of the file.
   If a customization can be extracted into helper functions, constants,
   mappers, or callbacks without hurting readability, place that custom
   block near the end of the file so it is easier to identify in future
   upgrades.

7. Do not force end-of-file extraction when the logic must stay inline.
   If the customization is naturally tied to a builder chain, model
   property, constructor parameter, or control-flow branch, keep it where
   it executes and add the required nearby comment there.

## What an agent should do automatically

- inspect git status, current branch, and diff against the upstream Oslo
  version being merged
- classify files into: easy conflicts, manual conflicts, post-merge review
  files
- resolve obvious `accept_theirs` files when they are clearly upstream
  changes with no EyeSeeTea logic
- update `customizations.md` only when a customization is confirmed to
  survive on top of the new Oslo baseline

## What an agent should not do automatically

- rewrite large groups of shared files just because `theirs` compiles
- remove custom code unless it is clearly obsolete or absorbed by upstream
- assume every conflict means a real customization
- assume every non-conflict diff is a real customization
- treat temporary merge progress as final customization documentation

## Comment convention for surviving customizations

```kotlin
// EyeSeeTea customization - [title]
// Base behavior: ...    // only when the customization changes base behavior
```

Rules:

- the first line with `EyeSeeTea customization - [title]` is mandatory
- add `Base behavior:` only when the customization replaces, restricts, or
  overrides behavior from upstream Oslo
- do not add `Base behavior:` when the customization only adds support and
  does not change the base behavior
- do not copy the original code verbatim into comments unless there is a
  temporary merge reason

## Comment convention for Oslo bug fixes

When patching an Oslo regression, use:

```kotlin
// EyeSeeTea fix - [short description] (Oslo [ticket], introduced [version])
// Remove when Oslo ships the upstream fix.
```

Rules:

- use `// EyeSeeTea fix` only for regressions or bugs in Oslo code, not for
  EyeSeeTea-specific behavior additions (those use `// EyeSeeTea customization`)
- remove the comment and the fix when Oslo ships the upstream correction

## Automerge verification rule

Git automerge resolves hunks without conflicts silently. It can drop
customization code that is not in a conflicting hunk — for example, a
parameter added at the end of a function call when only the beginning of the
file conflicts. Code comments (`// EyeSeeTea customization`) may also be
missing from some insertion points, so they are not a reliable check on
their own.

**This rule applies to every file listed in `customizations.md` after any
merge of upstream Oslo — not only files that git marked as conflicted.** If
upstream contains commits that removed code this fork's customization
depends on, git can apply those changes as a clean automerge with no
conflict markers, dropping customization wiring silently.

For every file in the customization inventory, verify the full delta — not
just the conflicted hunks — against the upstream tag branch created in
`upgrade-plan.md` Phase 1 (`origin/upstream/<version>`, e.g.
`origin/upstream/1.14.3`), not `upstream/develop`:

```bash
git diff origin/upstream/<version> -- path/to/file
```

- the diff must contain ALL the customization lines for that file, whether
  or not git reported a conflict
- compare the diff against `customizations.md` to check that every
  documented insertion point for that customization survived
- if the diff is smaller than expected, the automerge silently dropped
  code — recover it before staging

**Inventory completeness is load-bearing.** This rule only catches files
that are listed in `customizations.md`. Keep the inventory complete by
deriving it from the feature commits:

```bash
# for each customization, list all files the original feature commit touched
git show <feat-commit-sha> --stat
```

Every file in that output must appear in `customizations.md` under the
corresponding entry.

## File migration rule: Java to Kotlin

This fork carries both Java and Kotlin source (`core/src/main/java/**/*.java`
and `**/*.kt`). When the old customization was made in a `.java` file but the
new upstream Oslo version replaced it with a `.kt` file, do not assume the
old Java conflict is the right place to keep working.

Required process:

- locate the current active replacement file in the new upstream version
- verify whether the old Java file is still used, deprecated, duplicated, or
  superseded by the Kotlin file
- reimplement the surviving customization in the active Kotlin file if that
  is now the real implementation
- only then decide whether the old Java file should keep custom code, be
  resolved as `theirs`, or remain for manual review

Rules:

- if the active Kotlin replacement is clearly located and matches the same
  responsibility, reimplement the customization there and accept `theirs`
  in the old Java file
- this may be done automatically only when the Kotlin destination is
  clearly identified and the mapping is low-risk
- if the Kotlin replacement does not exist with a clear one-to-one mapping,
  or the new implementation is very different, do not resolve it
  automatically — classify the Java conflict as manual/supervised review
  and identify the likely Kotlin or replacement area where the
  customization must be reimplemented
- do not classify a Java-only conflict as a confirmed customization until
  the Kotlin replacement has been reviewed
- do not force a functional customization title onto a technical migration
  if no documented behavior matches it — if the migration is technical
  rather than functional, keep it in `customizations.md` section 2 (Oslo
  bug fixes) or leave it out of the inventory entirely, not in section 1

## Resolution categories

### A. `accept_theirs`

Use when the file has no EyeSeeTea business rule and the conflict comes from
upstream Oslo evolution (refactor, API migration, formatting).

### B. `manual_reapply_on_theirs`

The most common class for customized files.

Expected action:
- start from the upstream version
- port only the EyeSeeTea-specific logic
- add/update `// EyeSeeTea customization - ...` if the code remains

### C. `defer_after_build_verification`

Use when the customization may already be obsolete or absorbed, but
confidence is low.

Expected action:
- tentatively keep the upstream version
- verify with compilation/tests
- only reintroduce the customization if behavior is missing

## Merge algorithm

For each conflicted file:

1. Compare both sides and decide one category: `accept_theirs`,
   `manual_reapply_on_theirs`, or `defer_after_build_verification`.
2. If `manual_reapply_on_theirs`: keep upstream structure/API, reinsert the
   smallest customization logic, add/update the `// EyeSeeTea customization`
   comment, do not stage until the resulting diff matches the expected
   delta.
3. After resolving: if the customization is confirmed and still needed,
   update `customizations.md`. If not needed anymore, remove it from there.
   If the file still differs but the business meaning is unclear, mark it
   `needs_validation` and keep it out of the final inventory until resolved.

## Upgrade workflow

The full phase-by-phase runbook (how to fetch the target Oslo tag, create
`upstream/<version>`, branch, merge, validate, and close) lives in
`upgrade-plan.md` — this file only defines the rules used *within* that
workflow's conflict-resolution phases.
