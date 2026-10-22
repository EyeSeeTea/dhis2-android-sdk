# SDK Conflict Resolution Rules

Operational guide for resolving merge conflicts when bringing a tagged Oslo
release (`dhis2/dhis2-android-sdk`) into this fork.

This file is meant to be reusable by any agent or model in future sessions.

## Purpose

This document is **not** the customization inventory.

Its purpose is:
- define how conflicts should be resolved
- record which files are conflict-prone and why
- stay reusable across upgrades

## Document split

- `openspec/specs/<capability>/spec.md`
  What each customization does. Its top-level `# <Title>` is the customization
  title.
- `customization-files.md`
  Final-state technical inventory. Only confirmed customizations that still
  exist on top of the Oslo version this fork tracks.
- `conflict-rules.md` (this file)
  Working merge guide. Reusable rules only.
- `upgrade/upgrade-<version>-notes.md`
  Temporary upgrade notes: progress, decisions and open questions of one
  upgrade. Deleted when the upgrade closes. Copy from
  `upgrade/template/upgrade-notes-template.md`.
- `upgrade/validation-checklist.md`
  Validation per customization.

## Model: one fork, no client layer

`develop-eyeseetea` is the fork itself: it is updated by merging a tagged Oslo
release (see `upgrade-plan.md`) and nothing is layered on top of it. Every
customization is shared by every consumer of the published artifact, so there
is no per-client inventory or checklist here.

All conflict decisions should answer this question:

> What is the minimum EyeSeeTea-specific logic that must survive on top of
> the Oslo version being merged?

## Core principles

1. Prefer upstream Oslo by default.
   If a change is not clearly an EyeSeeTea customization, keep the upstream
   version.

2. Reapply minimal deltas, not whole old files.
   When a file conflicts, start from the upstream version and reinsert only
   the EyeSeeTea-specific behavior.

3. Do not use conflict resolution as documentation.
   Temporary merge notes go to `upgrade/upgrade-<version>-notes.md`; confirmed
   customizations go to their spec and `customization-files.md`.

4. If a customization is already absorbed by upstream Oslo, do not keep a
   duplicate fork copy of it. Mark it `absorbed` and remove the code.

5. A future agent must be able to continue from the docs alone.
   Keep decisions explicit, short, and file-based.

6. When possible, isolate custom code at the end of the file.
   If a customization can be extracted into helper functions, constants,
   mappers, or callbacks without hurting readability, place that block near
   the end of the file so it is easier to identify in future upgrades.

7. Do not force end-of-file extraction when the logic must stay inline.
   If the customization is naturally tied to a builder chain, model property,
   constructor parameter, or control-flow branch, keep it where it executes
   and add the required nearby comment there.

## Placement hierarchy

Every line changed in an Oslo file is a future merge conflict. Pick the first
option that works (see `customization-techniques.md` for known mechanisms):

1. **New EyeSeeTea-owned package or class** — e.g. `retention/`. Zero
   conflict risk.
2. **New file in an existing Oslo package** — no Oslo file touched.
3. **Block appended at the end of an existing Oslo file** — low conflict risk.
4. **Inline edit inside Oslo code** — last resort, keep it minimal.

## What an agent should do automatically

- inspect git status, current branch, and the two-dot diff against the
  upstream tag being merged (`git diff origin/upstream/<version>..HEAD`) —
  three-dot misses deletions on the upstream side
- classify files into: easy conflicts, manual conflicts, post-merge review
  files
- resolve obvious `accept_theirs` files when they are clearly upstream
  changes with no EyeSeeTea logic
- update `customization-files.md` only when a customization is confirmed to
  survive on top of the new Oslo version

## What an agent should not do automatically

- rewrite large groups of files just because `theirs` compiles
- remove custom code unless it is clearly obsolete or absorbed by upstream
- assume every conflict, or every non-conflict diff, is a real customization
- invent a customization title or create a spec without developer confirmation
- treat temporary merge progress as final customization documentation

## Comment convention for surviving customizations

```kotlin
// EyeSeeTea customization - <Title>
// Base behavior: ...    // only when the customization changes base behavior
```

Rules:

- `<Title>` is the exact top-level `# <Title>` of the matching
  `openspec/specs/<capability>/spec.md` — no capability slug, no suffix
- place the comment right above the customized block, not above the
  containing scope; for a new file in an EyeSeeTea-owned package, put it
  below the package declaration
- never on import lines
- add `Base behavior:` only when the customization replaces, restricts, or
  overrides behavior from upstream Oslo
- in non-Kotlin files use the file's own comment syntax (e.g. `#` in the
  migration `.sql` assets, `<!-- -->` in XML)
- generated files (`core/api/core.api`) carry no comments; they are
  regenerated with `./gradlew :core:apiDump` (Oslo `1.15.0` renames it to
  `:core:releaseApiDump`)

## Comment convention for Oslo bug fixes

When patching an Oslo bug or regression, use:

```kotlin
// EyeSeeTea fix - <short description> (Oslo <ticket>, introduced <version>)
// Remove when Oslo ships the upstream fix.
```

Rules:

- use `// EyeSeeTea fix` only for bugs in Oslo code, not for EyeSeeTea
  behavior additions (those use `// EyeSeeTea customization`)
- remove the comment and the fix when Oslo ships the upstream correction

## Automerge verification rule

Git automerge resolves hunks without conflicts silently. It can drop
customization code that is not in a conflicting hunk — for example, a
parameter added at the end of a function call when only the beginning of the
file conflicts. Code comments may also be missing from some insertion points,
so they are not a reliable check on their own.

**This rule applies to every file listed in `customization-files.md` after
any merge of upstream Oslo — not only files that git marked as conflicted.**
If upstream contains commits that removed code a customization depends on,
git can apply them as a clean automerge with no conflict markers.

For every file in the inventory, verify the full delta against the upstream
tag branch created in `upgrade-plan.md` Phase 1 (`origin/upstream/<version>`):

```bash
git diff origin/upstream/<version>..HEAD -- path/to/file
```

- the diff must contain ALL the customization lines for that file, whether
  or not git reported a conflict
- compare the diff against `customization-files.md` to check that every
  documented insertion point survived
- if the diff is smaller than expected, the automerge silently dropped
  code — recover it before staging

**Inventory completeness is load-bearing.** This rule only catches files
listed in `customization-files.md`. Keep the inventory complete by deriving
it from the feature commits listed in its section 4:

```bash
git show <feat-commit-sha> --stat
```

Every code file in that output must appear under the matching section 2
entry. `scripts/check_upgrade_docs.py` checks this automatically.

**A customization can live inside an Oslo file that nothing else references.**
Checking "does the customization reference this file?" is not enough. Always
grep the file's own contents for the customization before accepting `theirs`.

## Post-merge check hierarchy

marker scan < symbol scan < two-dot diff with semantic filter < automated
tests < consumer app check. Each level catches what the previous one misses.
For behavior only observable from the app (login flows, purge results on real
data), building the app against the local SDK (composite build, see
`EyeSeeTea.md`) is the last-line safety net.

## File migration rule: Java to Kotlin

This fork carries both Java and Kotlin source. When the old customization was
made in a `.java` file but the new Oslo version replaced it with a `.kt` file,
do not assume the old Java conflict is the right place to keep working.

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
- this may be done automatically only when the Kotlin destination is clearly
  identified and the mapping is low-risk
- if the Kotlin replacement does not exist with a clear one-to-one mapping,
  or the new implementation is very different, classify the Java conflict as
  manual review and identify the likely replacement area
- update the paths in `customization-files.md` to the new file

## Resolution categories

### A. `accept_theirs`

The file has no EyeSeeTea logic and the conflict comes from upstream Oslo
evolution (refactor, API migration, formatting).

### B. `manual_reapply_on_theirs`

The most common class for customized files.

Expected action:
- start from the upstream version
- port only the EyeSeeTea-specific logic
- add/update `// EyeSeeTea customization - <Title>` if the code remains

### C. `defer_after_build_verification`

The customization may already be obsolete or absorbed, but confidence is low.

Expected action:
- tentatively keep the upstream version
- verify with compilation/tests
- only reintroduce the customization if behavior is missing

## Merge algorithm

For each conflicted file:

1. Decide one category: `accept_theirs`, `manual_reapply_on_theirs`, or
   `defer_after_build_verification`.
2. If `manual_reapply_on_theirs`: keep upstream structure/API, reinsert the
   smallest customization logic, add/update the comment, and do not stage
   until the resulting diff matches the expected delta.
3. After resolving: if the customization is confirmed and still needed, keep
   its spec and update `customization-files.md`. If not needed anymore, mark
   it `absorbed` or `removed`. If the business meaning is unclear, mark it
   `needs_validation` in the upgrade notes and keep it out of the final
   inventory until resolved.

## Upgrade workflow

The phase-by-phase runbook lives in `upgrade-plan.md`; this file only defines
the rules used within its conflict-resolution phases.
