## 1. Purge candidate selection (leaf case, no cascade)

- [x] 1.1 Add a `core/src/androidTest` behavior test on `DataValue`: given rows
      with a mix of `SYNCED` and non-`SYNCED` state and distinct `lastUpdated`
      values, and a retention limit smaller than the number of `SYNCED` rows,
      calling the new purge entry point leaves exactly the most-recently-updated
      `SYNCED` rows up to the limit, and every non-`SYNCED` row untouched, when
      the table is re-queried afterward. Verify: test fails (no implementation
      yet).
- [x] 1.2 Implement the minimal candidate-selection + delete logic for
      `DataValue` (new interface + module implementation, per design.md
      Decisions — do not touch `ModuleWiper`). Verify: the 1.1 test passes.

**Commit: 1.1 + 1.2 together** (red test, then the implementation that turns it
green).

- [x] 1.3 Add a test: limit of zero purges every `SYNCED` `DataValue` row,
      leaving none behind (limit=0 degenerate-case scenario from the spec).
      Verify: test passes against the same implementation, no special-casing
      needed.

**Commit: 1.3 alone** (test-only — asserts an existing behavior of the 1.1/1.2
implementation, no production code expected to change).

- [x] 1.4 Add a test: number of eligible rows at or below the limit purges
      nothing — table is unchanged after the call. Verify: test passes.

**Commit: 1.4 alone** (test-only, same reason as 1.3).

## 2. Transactional guarantee (leaf case)

- [ ] 2.1 Add a test that simulates a write failure partway through a
      `DataValue` purge (mock only at the true system boundary — e.g. the
      underlying write call — per design.md/config.yaml rule; do not mock the
      class under test's own collaborators to observe its delete calls) and
      asserts the table is byte-for-byte unchanged from before the attempt.
      Verify: test fails without transactional wrapping.
- [ ] 2.2 Wrap the purge entry point in the existing
      `d2CallExecutor.executeD2CallTransactionally` pattern (same as
      `WipeModuleImpl`). Verify: the 2.1 test passes.

**Commit: 2.1 + 2.2 together** (red test, then the wrapping that turns it
green).

## 3. Tree-aware purge — TrackedEntityInstance module

- [ ] 3.1 Add a behavior test: a fully synced TEI (aggregatedSyncState =
      SYNCED) beyond the retention limit is purged, and its attribute values
      are purged with it, when re-querying both tables afterward. Verify: test
      fails.
- [ ] 3.2 Implement TEI purge candidate selection ordered by `lastUpdated`
      using `aggregatedSyncState`, plus cascade delete of
      `TrackedEntityAttributeValue`. Verify: the 3.1 test passes.

**Commit: 3.1 + 3.2 together.**

- [ ] 3.3 Add a behavior test: a TEI that is itself fully synced but has one
      non-synced descendant anywhere in its tree (add the descendant once
      Enrollment/Event purge exists in section 4-5; until then, assert via a
      directly-inserted non-SYNCED child row at the deepest currently-modeled
      level) is not purged, even when older than the retention limit boundary.
      Verify: test passes, confirms partial-tree protection at the TEI level.

**Commit: 3.3 alone** (test-only, against the 3.1/3.2 implementation).

## 4. Tree-aware purge — Enrollment module

- [ ] 4.1 Add a behavior test: an eligible TEI's enrollments (and their notes)
      are purged together with the TEI when the TEI is purged. Verify: test
      fails.
- [ ] 4.2 Implement Enrollment (+ Note) cascade delete keyed off the same
      selected TEI id set from section 3. Verify: the 4.1 test passes.

**Commit: 4.1 + 4.2 together.**

- [ ] 4.3 Add a behavior test: an Enrollment belonging to a TEI that is NOT
      eligible (non-synced descendant elsewhere in the tree) is never purged on
      its own, independent of the Enrollment's own state. Verify: test passes.

**Commit: 4.3 alone** (test-only).

## 5. Tree-aware purge — Event module (tracker-rooted)

- [ ] 5.1 Add a behavior test: a purged TEI's events, their
      TrackedEntityDataValues, and their notes are all removed together with
      the TEI/Enrollment tree. Verify: test fails.
- [ ] 5.2 Implement Event (+ TrackedEntityDataValue + Note) cascade delete
      keyed off the same selected TEI id set. Verify: the 5.1 test passes.

**Commit: 5.1 + 5.2 together.**

- [ ] 5.3 Re-run the 3.3 test (partial-tree protection) now with a real
      non-synced Event as the protecting descendant instead of the placeholder
      used in 3.3, and confirm it still passes end-to-end through the full
      TEI → Enrollment → Event tree. Verify: test passes; remove the 3.3
      placeholder assertion if it becomes redundant with this one.

**Commit: 5.3 alone** (test-only; may include deleting the now-redundant 3.3
placeholder assertion as part of the same commit, since that's tidying the
test just added, not new production code).

## 6. Independent leaf purge — TEI-less events

- [ ] 6.1 Add a behavior test: events with no tracked entity instance are
      purged independently under their own retention limit, ordered by
      `lastUpdated`, using their own `aggregatedSyncState` (event + its
      TrackedEntityDataValues + notes), without affecting tracker-rooted event
      purge from section 5 or being affected by it. Verify: test fails.
- [ ] 6.2 Implement TEI-less event purge candidate selection and cascade,
      reusing the selection building block from section 1/3 rather than
      duplicating it. Verify: the 6.1 test passes.

**Commit: 6.1 + 6.2 together.**

## 7. FileResource purge

- [ ] 7.1 Add a behavior test: eligible `FileResource` rows beyond the
      retention limit are removed from the table and their physical file (via
      the row's `path`) is deleted from disk, when re-checking both the table
      and the filesystem afterward. Verify: test fails.
- [ ] 7.2 Implement row-by-row `FileResource` purge (select eligible rows
      ordered by `lastUpdated` over the limit, delete row + physical file per
      row) — do not reuse the existing directory-recursive delete from
      `wipeData()`. Verify: the 7.1 test passes.

**Commit: 7.1 + 7.2 together.**

- [ ] 7.3 Add a behavior test: an eligible `FileResource` row whose physical
      file is already missing from disk is still purged from the table, and
      the call does not raise an error to the caller. Verify: test fails
      without the missing-file tolerance, passes once added.

**Commit: 7.3 alone** — unless the 7.2 implementation did not yet tolerate a
missing file, in which case this commit also includes the small fix (still one
commit: red test + the minimal fix that turns it green, same red-green rule as
any other pair in this file).

## 8. Public entry point and cross-module transactionality

- [ ] 8.1 Add a behavior test: purging with limits set for multiple data types
      in one call (e.g. TEI limit and DataValue limit together) purges each
      independently and correctly in a single invocation. Verify: test fails
      without a combined entry point.
- [ ] 8.2 Implement the public purge entry point that accepts per-data-type
      limits and invokes each module's purge implementation inside a single
      `executeD2CallTransactionally` block. Verify: the 8.1 test passes.

**Commit: 8.1 + 8.2 together.**

- [ ] 8.3 Add a behavior test: a failure during one data type's purge (e.g.
      FileResource) inside a multi-type call leaves ALL data types' tables
      unchanged, not just the failing one. Verify: test fails without proper
      transactional scope, passes once 8.2's wrapping covers every module in
      one transaction.

**Commit: 8.3 alone** if 8.2 already covers it (test-only); otherwise 8.3 plus
the minimal fix to `8.2`'s transaction scope, as one red-green commit.

## 9. Full spec verification

- [ ] 9.1 Walk every `#### Scenario:` in
      `specs/synced-data-retention-purge/spec.md` and confirm each one maps to
      at least one test added in sections 1-8; add any scenario found without
      a corresponding test. Verify: one-to-one mapping documented (a checklist
      comment in this task or a follow-up commit), no scenario left
      unverified.

**Commit: 9.1 alone**, only if it adds a missing test; if every scenario is
already covered, no commit is needed — record the mapping in the PR
description instead of an empty commit.

- [ ] 9.2 Run the full `core` test suite (unit + androidTest) and confirm
      green, with no pre-existing wiper/test behavior changed. Verify: CI or
      local run passes.

**Commit: none** — verification only, no file changes expected.
