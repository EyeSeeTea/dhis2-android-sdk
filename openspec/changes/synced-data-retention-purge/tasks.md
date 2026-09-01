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

- [x] 2.1 Add a test that simulates a write failure partway through a
      `DataValue` purge (mock only at the true system boundary — e.g. the
      underlying write call — per design.md/config.yaml rule; do not mock the
      class under test's own collaborators to observe its delete calls) and
      asserts the table is byte-for-byte unchanged from before the attempt.
      Verify: test fails without transactional wrapping.
- [x] 2.2 Wrap the purge entry point in the existing
      `d2CallExecutor.executeD2CallTransactionally` pattern (same as
      `WipeModuleImpl`). Verify: the 2.1 test passes.

**Commit: 2.1 + 2.2 together** (red test, then the wrapping that turns it
green).

**Superseded by section 8** (see design.md "Transactionality" — revised after
verifying no `ModuleWiper` nests `executeD2CallTransactionally`, only
`WipeModuleImpl` calls it once from the outside): the per-purger transaction
wrapping added here and in every following module section was removed from
each individual `XxxRetentionPurger`; the single transaction now wraps the
section 8 composed entry point only. The 2.1 test (write-failure rollback)
was removed from `DataValueRetentionPurgerIntegrationShould` and its
equivalent now belongs to task 8.3, at the composed-entry-point level.

## 3. Tree-aware purge — TrackedEntityInstance module

- [x] 3.1 Add a behavior test: a fully synced TEI (aggregatedSyncState =
      SYNCED) beyond the retention limit is purged, and its attribute values
      are purged with it, when re-querying both tables afterward. Verify: test
      fails.
- [x] 3.2 Implement TEI purge candidate selection ordered by `lastUpdated`
      using `aggregatedSyncState`, plus cascade delete of
      `TrackedEntityAttributeValue`. Verify: the 3.1 test passes.

**Commit: 3.1 + 3.2 together.**

- [x] 3.3 Add a behavior test: a TEI whose own `aggregatedSyncState` is not
      `SYNCED` is not purged, even when older than the retention limit
      boundary. Revised from the original plan: `TrackedEntityAttributeValue`
      (the only child modeled so far) does not participate in
      `aggregatedSyncState` computation (see `DataStatePropagatorImpl` —
      only Enrollment/Event/Relationship states feed into it), so it cannot
      simulate a non-synced descendant. This test asserts directly against
      `aggregatedSyncState` (the actual column the purger filters on) instead
      of via an inserted child; the real descendant-propagation scenario is
      covered once a real Enrollment/Event exists (task 5.3). Verify: test
      passes, confirms the purger respects `aggregatedSyncState != SYNCED`.

**Commit: 3.3 alone** (test-only, against the 3.1/3.2 implementation).

## 4. Tree-aware purge — Enrollment module

- [x] 4.1 Add a behavior test: an eligible TEI's enrollments (and their notes)
      are purged together with the TEI when the TEI is purged. Verify: test
      fails.
- [x] 4.2 Implement Enrollment (+ Note) cascade delete keyed off the same
      selected TEI id set from section 3. Verify: the 4.1 test passes.

**Commit: 4.1 + 4.2 together.**

- [x] 4.3 Add a behavior test: an Enrollment belonging to a TEI that is NOT
      eligible (non-synced descendant elsewhere in the tree) is never purged on
      its own, independent of the Enrollment's own state. Verify: test passes.

**Commit: 4.3 alone** (test-only).

## 5. Tree-aware purge — Event module (tracker-rooted)

- [x] 5.1 Add a behavior test: a purged TEI's events, their
      TrackedEntityDataValues, and their notes are all removed together with
      the TEI/Enrollment tree. Verify: test fails.
- [x] 5.2 Implement Event (+ TrackedEntityDataValue + Note) cascade delete
      keyed off the same selected TEI id set. Verify: the 5.1 test passes.

**Commit: 5.1 + 5.2 together.**

- [x] 5.3 Re-run the 3.3 test (partial-tree protection) now with a real
      non-synced Event as the protecting descendant instead of the placeholder
      used in 3.3, and confirm it still passes end-to-end through the full
      TEI → Enrollment → Event tree. Verify: test passes; remove the 3.3
      placeholder assertion if it becomes redundant with this one.
      Implemented by extending the 4.3 test (renamed
      `keep_the_whole_tree_of_a_tei_that_is_not_eligible_...`) with a real
      Enrollment + Event under the protected TEI, still marking
      `aggregatedSyncState` by hand rather than driving it through
      `DataStatePropagatorImpl` — using the real propagator here would couple
      this purger's tests to another component's responsibility, already
      covered by `DataStatePropagatorIntegrationShould.kt` (see design.md).
      The 3.3 test was kept, not removed: it additionally covers a second,
      `SYNCED` TEI coexisting with the protected one (distinguishing eligible
      vs. protected), which this tree-shaped test does not.

**Commit: 5.3 alone** (test-only; may include deleting the now-redundant 3.3
placeholder assertion as part of the same commit, since that's tidying the
test just added, not new production code).

## 6. Independent leaf purge — TEI-less events

- [x] 6.1 Add a behavior test: events with no tracked entity instance are
      purged independently under their own retention limit, ordered by
      `lastUpdated`, using their own `aggregatedSyncState` (event + its
      TrackedEntityDataValues + notes), without affecting tracker-rooted event
      purge from section 5 or being affected by it. Verify: test fails.
- [x] 6.2 Implement TEI-less event purge candidate selection and cascade,
      reusing the selection building block from section 1/3 rather than
      duplicating it. Verify: the 6.1 test passes.

**Commit: 6.1 + 6.2 together.**

## 7. FileResource purge

- [x] 7.1 Add a behavior test: eligible `FileResource` rows beyond the
      retention limit are removed from the table and their physical file (via
      the row's `path`) is deleted from disk, when re-checking both the table
      and the filesystem afterward. Verify: test fails.
- [x] 7.2 Implement row-by-row `FileResource` purge (select eligible rows
      ordered by `lastUpdated` over the limit, delete row + physical file per
      row) — do not reuse the existing directory-recursive delete from
      `wipeData()`. Verify: the 7.1 test passes.

**Commit: 7.1 + 7.2 together.**

- [x] 7.3 Add a behavior test: an eligible `FileResource` row whose physical
      file is already missing from disk is still purged from the table, and
      the call does not raise an error to the caller. Verify: test fails
      without the missing-file tolerance, passes once added.
      In practice the 7.2 implementation already wrapped the physical-file
      delete in `runCatching`, so this test passed green on first run —
      committed together with 7.1/7.2 rather than as a separate fix commit.

## 8. Public entry point and cross-module transactionality

- [x] 8.1 Add a behavior test: purging with limits set for multiple data types
      in one call (e.g. TEI limit and DataValue limit together) purges each
      independently and correctly in a single invocation. Verify: test fails
      without a combined entry point.
- [x] 8.2 Implement the public purge entry point that accepts per-data-type
      limits and invokes each module's purge implementation inside a single
      `executeD2CallTransactionally` block. Verify: the 8.1 test passes.
      Implemented as `SyncedDataRetentionPurger` (+ `RetentionLimits` data
      class), constructor-typed against the `RetentionPurger` interface for
      each of the 4 module purgers, so tests can substitute a failing fake
      per module without needing a concrete subclass.

**Commit: 8.1 + 8.2 together.**

- [x] 8.3 Add a behavior test: a failure during one data type's purge (e.g.
      FileResource) inside a multi-type call leaves ALL data types' tables
      unchanged, not just the failing one. Verify: test fails without proper
      transactional scope, passes once 8.2's wrapping covers every module in
      one transaction.
      Passed on first run, confirming in practice that the single
      transaction added in 8.2 (after the per-purger-transaction refactor
      done earlier in this section) rolls back every module's writes, not
      just the failing one — committed together with 8.1/8.2 rather than as
      a separate commit.

**Commit: 8.3 alone** if 8.2 already covers it (test-only); otherwise 8.3 plus
the minimal fix to `8.2`'s transaction scope, as one red-green commit.

## 9. Full spec verification

- [x] 9.1 Walk every `#### Scenario:` in
      `specs/synced-data-retention-purge/spec.md` and confirm each one maps to
      at least one test added in sections 1-8; add any scenario found without
      a corresponding test. Verify: one-to-one mapping documented (a checklist
      comment in this task or a follow-up commit), no scenario left
      unverified.

      Mapping (scenario → test):
      - Fully synced tree is eligible →
        `TrackedEntityRetentionPurgerIntegrationShould.purge_a_fully_synced_tracked_entity_instance_together_with_its_attribute_values`
      - One pending descendant protects the whole tree →
        `TrackedEntityRetentionPurgerIntegrationShould.keep_the_whole_tree_of_a_tei_that_is_not_eligible_even_if_its_enrollment_and_event_are_synced`
        (+ `keep_a_tracked_entity_instance_whose_aggregated_sync_state_is_not_synced`)
      - Leaf data without children uses its own sync state →
        `DataValueRetentionPurgerIntegrationShould` (all 3 tests filter by own
        `syncState`, no aggregate) +
        `EventRetentionPurgerIntegrationShould.keep_a_tei_less_event_whose_own_aggregated_sync_state_is_not_synced`
      - Only the oldest excess records are purged →
        `DataValueRetentionPurgerIntegrationShould.keep_only_the_most_recently_updated_synced_data_values_up_to_the_limit`
        + `EventRetentionPurgerIntegrationShould.purge_the_oldest_synced_tei_less_events_beyond_the_limit_with_their_data_values_and_notes`
        + `FileResourceRetentionPurgerIntegrationShould.purge_an_eligible_file_resource_beyond_the_limit_together_with_its_physical_file`
      - Non-eligible records are never counted toward the limit →
        `DataValueRetentionPurgerIntegrationShould.keep_only_the_most_recently_updated_synced_data_values_up_to_the_limit`
        (the older `pending` row survives and is excluded from the count)
      - A limit of zero purges everything eligible →
        `DataValueRetentionPurgerIntegrationShould.purge_every_synced_data_value_when_limit_is_zero`
      - Fewer eligible records than the limit purges nothing →
        `DataValueRetentionPurgerIntegrationShould.keep_every_synced_data_value_when_eligible_rows_are_at_or_below_the_limit`
      - Purging a tracked entity instance removes its full tree →
        `TrackedEntityRetentionPurgerIntegrationShould.purge_the_enrollments_and_their_notes_of_a_purged_tracked_entity_instance`
        + `purge_the_events_their_data_values_and_their_notes_of_a_purged_tracked_entity_instance`
      - Trimming leaf data does not affect tracked entity data → **gap found,
        no existing test isolated this.** Added
        `SyncedDataRetentionPurgerIntegrationShould.trim_data_values_under_their_limit_without_affecting_an_eligible_tracked_entity_instance`.
      - A failure partway through leaves data unchanged →
        `SyncedDataRetentionPurgerIntegrationShould.leave_every_data_type_unchanged_when_one_type_fails_partway_through_a_multi_type_purge`
      - Purging a file resource with an existing physical file →
        `FileResourceRetentionPurgerIntegrationShould.purge_an_eligible_file_resource_beyond_the_limit_together_with_its_physical_file`
      - Purging a file resource whose physical file is already missing →
        `FileResourceRetentionPurgerIntegrationShould.purge_an_eligible_file_resource_whose_physical_file_is_already_missing_without_raising_an_error`

**Commit: 9.1 alone**, only if it adds a missing test; if every scenario is
already covered, no commit is needed — record the mapping in the PR
description instead of an empty commit.

Adds a missing test (see mapping above) — committed alone.

- [x] 9.2 Run the full `core` test suite (unit + androidTest) and confirm
      green, with no pre-existing wiper/test behavior changed. Verify: CI or
      local run passes.

      Verified locally: `:core:testDebugUnitTest` green;
      `:core:connectedDebugAndroidTest` on `Pixel_9a` (real emulator, `android`
      CLI) — 5524 tests run, 0 failed, 158 skipped (pre-existing skip set, not
      new).

**Commit: none** — verification only, no file changes expected.

## 10. FileResource cascade eligibility (fixes the risk found in 9.1's coverage pass)

See design.md "FileResource cascade" for the full rationale: a `FileResource`
referenced by a value must inherit that value's tree eligibility instead of
being evaluated by its own `syncState`, and orphaned `FileResource`s (no live
reference at all) are purged independently. This does NOT change
`RetentionPurger`'s signature (an earlier design that did was reverted — see
design.md's "Two designs were considered and rejected" for why); instead, a
new injected collaborator (`ValueFileResourcePurger`) is called inline, in the
same loop, by the three value purgers. Section 1-9 code is already merged, so
treat every sub-task here as a refactor of tested code, re-running the full
existing `retention` package test suite after each step, not just the new
test.

- [x] 10.1 Add a new class `ValueFileResourcePurger` (constructor:
      `DataElementStore`, `TrackedEntityAttributeStore`, `FileResourceStore`)
      with two methods, `purgeIfDataElementReferencesFile(dataElementUid:
      String?, value: String?)` and `purgeIfAttributeReferencesFile
      (attributeUid: String?, value: String?)`. Each resolves whether the
      given uid is a file-typed (`ValueType.FILE_RESOURCE`/`IMAGE`)
      `DataElement`/`TrackedEntityAttribute` — via a lazily-populated,
      per-instance `Set<String>` cache (one `SELECT ... WHERE valueType IN
      (...)` per table, on first use, no `prepare()`/init-time call) — and if
      so, purges the `FileResource` named by `value` (row + physical file,
      `runCatching` around the file delete, same as the existing row-by-row
      FileResource purge). Add a unit/integration test per method: a file-type
      `dataElement`/`trackedEntityAttribute` with a `value` naming an existing
      `FileResource` purges it; a non-file-type one does not purge anything
      even if `value` happens to name an existing `FileResource`; `value =
      null` or naming no existing `FileResource` is a no-op, not an error.
      Verify: tests fail (no implementation yet), then pass.

      Implemented with a per-instance lazy cache (`Set<String>?` populated on
      first use, no `prepare()`). Reused `ValueType.isFile` (already public)
      instead of duplicating the `FILE_RESOURCE`/`IMAGE` set. 6 tests added in
      `ValueFileResourcePurgerIntegrationShould`, all passing on `Pixel_9a`
      (real emulator) on first run.

**Commit: 10.1 alone** (new, self-contained class — not yet wired into any
existing purger).

- [x] 10.2 Switch `TrackedEntityDataValue` deletion in both
      `TrackedEntityRetentionPurger` (event cascade) and `EventRetentionPurger`
      (TEI-less path) from `TrackedEntityDataValueStore.deleteByEvent(uid)` (a
      batch delete that never reads what it removes) to read-then-delete
      (`getForEvent` followed by row-level `deleteWhere`), matching the shape
      `TrackedEntityRetentionPurger` already uses for
      `TrackedEntityAttributeValueStore`. This is a pure refactor with no new
      behavior yet (no `ValueFileResourcePurger` call added in this step).
      Verify: existing cascade tests in both files still pass unchanged.

      Verified: full `retention` package suite (21 tests, including the 6 new
      `ValueFileResourcePurger` tests from 10.1) passes unchanged on
      `Pixel_9a` (real emulator).

**Commit: 10.2 alone** (mechanical refactor, existing tests are the safety
net — no new test expected).

- [x] 10.3 Add a behavior test: a `TrackedEntityAttributeValue` of a file type
      references a `FileResource` that is itself `SYNCED`, but the owning
      TEI's `aggregatedSyncState` is not `SYNCED` (e.g. one of its events is
      pending) — the file resource is NOT purged when
      `TrackedEntityRetentionPurger` runs (the TEI is not eligible, so its
      attribute value, and therefore its file resource, is never reached).
      Verify: test fails (no `ValueFileResourcePurger` call wired in yet, but
      also passes vacuously without one — assert it actually exercises the
      cascade path by first proving the positive case, see 10.4, or write both
      together).
- [x] 10.4 Wire `ValueFileResourcePurger` into `DataValueRetentionPurger`,
      `TrackedEntityRetentionPurger` (both the attribute-value loop, via
      `purgeIfAttributeReferencesFile`, and the event data-value loop, via
      `purgeIfDataElementReferencesFile`), and `EventRetentionPurger` (its
      data-value loop): call the matching method right after `deleteWhere` for
      each purged row, inside the existing loop — no new loop, no batching.
      Verify: the 10.3 test passes (the file resource survives because the
      TEI's attribute value is never deleted, so `ValueFileResourcePurger` is
      never called for it); also add the symmetric positive-case test per
      purger (purging an eligible `DataValue`/`TrackedEntityAttributeValue`/
      `TrackedEntityDataValue` that references a `FileResource` purges that
      file resource, row + physical file, in the same call) — one per module,
      three tests total. Re-run the full `retention` package suite (per Group
      10 preamble).

**Commit: 10.3 + 10.4 together** (red test, then the wiring that turns it
green, plus the three symmetric positive-case tests).

Implemented as 5 tests total (not exactly the "three" sketched above): the
10.3 protection test, plus one positive case each for `DataValueRetentionPurger`,
`EventRetentionPurger`, and two for `TrackedEntityRetentionPurger` (one for its
`TrackedEntityAttributeValue` loop, one for its event `TrackedEntityDataValue`
loop, since that purger wires `ValueFileResourcePurger` at two separate call
sites). Needed a `CategoryCombo` fixture in the 3 files that build a file-typed
`DataElement` for these tests — `DataElement.categoryCombo()` is `@NonNull` in
its builder (confirmed against `FileResourceRoutineSamples.kt`, an existing
test fixture using the same pattern). Revisit later whether this fixture setup
should be shared instead of repeated per file (noted, not addressed now).
Verified: full `retention` package suite (26 tests) green on `Pixel_9a` (real
emulator).

- [x] 10.5 Rename `FileResourceRetentionPurger` to
      `OrphanFileResourceRetentionPurger` and add a `NOT IN` filter (via each
      of `DataValue`/`TrackedEntityAttributeValue`/`TrackedEntityDataValue`'s
      `value` column, same resolution approach as
      `FileResourceDownloadCallHelper`) excluding any `FileResource` uid
      referenced by a live row in any of those three tables — on top of the
      existing by-limit/`lastUpdated`/`syncState` selection, unchanged. Add a
      behavior test: a `FileResource` with no live reference at all, beyond
      the limit and `SYNCED`, is purged; a `FileResource` still referenced by
      a live value is never purged by this class regardless of its own
      state/limit (even if it would otherwise be eligible by limit/syncState).
      Verify: new test passes; existing
      `FileResourceRetentionPurgerIntegrationShould` tests (now
      `OrphanFileResourceRetentionPurgerIntegrationShould`) still pass with
      only the rename applied, since the pre-existing tests never insert a
      live reference to the file resources they purge.

      Implemented the `NOT IN` filter by unioning
      `selectStringColumnsWhereClause(VALUE, "1")` across the 3 value stores
      into one `Set<String>`, then a single `appendNotInKeyStringValues` on
      `FileResource.uid` (same "1" always-true clause idiom already used
      elsewhere in the SDK, e.g. `CustomIconModuleDownloader`).
      `SyncedDataRetentionPurger`'s constructor parameter renamed
      `fileResourcePurger` → `orphanFileResourcePurger` (same `RetentionPurger`
      type, no structural change). Verified: full `retention` package suite
      (27 tests) green on `Pixel_9a` (real emulator).

**Commit: 10.5 alone.**

- [ ] 10.6 Full spec re-verification (same method as 9.1): walk every
      `#### Scenario:` added to `spec.md` by this group (FileResource cascade
      eligibility + orphan purge requirements) and confirm each maps to a test
      added in 10.1-10.5; then re-run the full `core` test suite (unit +
      androidTest), same as 9.2, to confirm no regression outside this
      change. Verify: mapping documented here, full suite green.

**Commit: 10.6 alone**, only if it adds a missing test; otherwise document the
mapping here with no commit, same rule as 9.1.
