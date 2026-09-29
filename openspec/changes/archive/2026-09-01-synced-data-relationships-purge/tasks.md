## 1. `RelationshipRetentionPurger` — purge relationships of an entity being purged

Building block used by every purger below (mirrors `ValueFileResourcePurger`'s
role for the FileResource cascade — a plain injected collaborator, not a
`RetentionPurger`, no limit of its own, called inline).

- [x] 1.1 Add a behavior test: given a `Relationship` linking two tracked entity
      instances, both fully synced (`aggregatedSyncState = SYNCED`), calling
      `purgeForEntity(uid)` for one of them (with the other row already deleted
      by the caller, matching how it will actually be invoked — see task 2)
      purges the `Relationship` and its two `RelationshipItem` rows. Verify:
      test fails (no implementation yet).
- [x] 1.2 Implement `RelationshipRetentionPurger.purgeForEntity(entityUid:
      String)`: find every `RelationshipItem` referencing `entityUid`
      (`RelationshipItemStore.getByEntityUid`), and for each, delete the
      `Relationship` row and both its `RelationshipItem` rows. Verify: the 1.1
      test passes.

      Implemented using `deleteWhereIfExists`/`deleteIfExists` throughout, so
      the class already tolerates a missing counterpart from the start (see
      1.3). Verified: 1 test green on `Pixel_9a` (real emulator).

**Commit: 1.1 + 1.2 together** (red test, then the implementation that turns it
green).

- [x] 1.3 Add a behavior test: `purgeForEntity(uid)` for an entity whose
      relationship counterpart no longer exists in any store at all (already
      orphaned) still purges the `Relationship`/`RelationshipItem` rows without
      raising an error. Verify: test fails without explicit handling, passes
      once added — this is the self-healing case from design.md's Risks
      section, not a hypothetical.

**Commit: 1.3 alone** if 1.2's delete-by-relationship-uid approach already
tolerates a missing counterpart row (test-only); otherwise 1.3 plus the minimal
fix, as one red-green commit.

In practice 1.2 already tolerated it from the start (no fix needed), so 1.3 was
committed together with 1.1/1.2 rather than as a separate commit — same
"passed green on first run" pattern already used repeatedly in the prior
change (e.g. its task 7.3). Verified: 2 tests green on `Pixel_9a`.

## 2. Cross-tree eligibility check — read side

Building block used by every purger's selection query below: given a candidate
entity uid, determine whether every relationship it participates in has a fully
synced counterpart.

- [x] 2.1 Add a behavior test: given a tracked entity instance with a
      relationship to another tracked entity instance whose own
      `aggregatedSyncState` is not `SYNCED`, an eligibility check for the first
      TEI's uid returns "not eligible". Verify: test fails (no implementation
      yet).
- [x] 2.2 Implement the eligibility check: given an entity uid, resolve every
      `RelationshipItem` referencing it, resolve the other item of the same
      `Relationship` via `RelationshipItemStore.getForRelationshipUid`, resolve
      that other item's own `elementType()`/`elementUid()` to a
      TrackedEntityInstance/Enrollment/Event and read its `aggregatedSyncState`.
      Returns eligible only if every counterpart found is `SYNCED`; a
      counterpart uid that resolves to no row at all counts as eligible (see
      design.md Risks — self-healing for pre-existing orphans). Verify: the 2.1
      test passes.

**Commit: 2.1 + 2.2 together.**

Implemented as `RelationshipEligibilityChecker`, a separate class from
`RelationshipRetentionPurger` (read-only check vs. delete side-effect — kept as
two collaborators sharing the same `RelationshipItemStore` dependency, not
merged into one class). Verified: green on `Pixel_9a` (real emulator).

- [x] 2.3 Add a behavior test: the same check, but the relationship's
      counterpart is an Enrollment (not a TrackedEntityInstance) whose own
      `aggregatedSyncState` is not `SYNCED` — confirms the check resolves
      `elementType()` correctly across all three possible counterpart kinds, not
      just TrackedEntityInstance. Verify: test passes against the 2.2
      implementation with no changes needed (or the minimal fix, as one
      red-green commit, if a gap is found).

**Commit: 2.3 alone** (test-only), unless it finds a real gap in 2.2's handling
of non-TEI counterparts — then bundle the fix with it as one commit.

Passed on first run against 2.2's implementation (already resolves
`elementType()` uniformly via `selectByUid` on all three root stores) — one
unrelated fixture bug found and fixed while writing it (a test `Enrollment`
missing the required `trackedEntityInstance` field, unrelated to the
eligibility logic itself). Committed together with 2.1/2.2 rather than
separately, since both tests live in the same file added in this group.
Verified: 2 tests green on `Pixel_9a`.

## 3. Wire eligibility check + cascade into `TrackedEntityRetentionPurger`

- [x] 3.1 Add a behavior test: a fully synced tracked entity instance with a
      relationship to another fully synced tracked entity instance — both are
      purged, and the relationship linking them is purged too, in the same
      call. Verify: test fails (current purger has no relationship awareness).
- [x] 3.2 Wire the eligibility check from Group 2 into
      `TrackedEntityRetentionPurger`'s selection query (excluding a
      candidate whose relationship counterpart is not eligible, on top of the
      existing `aggregatedSyncState = SYNCED` filter), and call
      `RelationshipRetentionPurger.purgeForEntity(tei.uid())` inline when a TEI
      is purged. Verify: the 3.1 test passes.

      The eligibility filter was added to the `eligible` pipeline
      (`.filter { relationshipEligibilityChecker.isEligible(it.uid()) }`
      before `.sortedByDescending`), not inside the `toPurge.forEach` body —
      keeps `detekt`'s `NestedBlockDepth` (already at its 4/4 limit after the
      prior change's fix) from being exceeded again.

**Commit: 3.1 + 3.2 together.**

- [x] 3.3 Add a behavior test: a fully synced tracked entity instance that has a
      relationship to another tracked entity instance whose own tree is NOT
      fully synced is NOT purged, even though its own tree is otherwise
      eligible — the cross-tree protection scenario from spec.md. Verify: test
      passes against the 3.2 implementation (should already be green — this
      asserts the negative case symmetric to 3.1's positive one).

      Passed on first run. Committed together with 3.1/3.2 rather than
      separately, since both tests were added to the same file in the same
      pass. Verified: full `retention` package suite (33 tests) green on
      `Pixel_9a`; `:core:detekt` green (no `NestedBlockDepth` regression).

**Commit: 3.3 alone** (test-only, against the 3.2 implementation).

## 4. Wire eligibility check + cascade into the Enrollment cascade step

`TrackedEntityRetentionPurger` already cascades into Enrollment as part of
purging a TEI (no standalone `EnrollmentRetentionPurger` exists — see the prior
change's design.md, Enrollment is purged only as part of its TEI's cascade, never
independently). This group extends that same cascade step.

- [x] 4.1 Add a behavior test: an eligible tracked entity instance's enrollment
      has a relationship to an eligible event (belonging to a different,
      unrelated tracked entity instance) — purging the first TEI purges its
      enrollment, and the relationship linking the enrollment to that event is
      purged too. Verify: test fails.
- [x] 4.2 Wire `RelationshipRetentionPurger.purgeForEntity(enrollment.uid())`
      inline into the enrollment-cascade step of `TrackedEntityRetentionPurger`.
      Verify: the 4.1 test passes.

      Also introduced `isTreeRelationshipEligible(teiUid)`, replacing the plain
      `relationshipEligibilityChecker.isEligible(it.uid())` filter from Group 3 —
      it now checks the TEI's own relationships AND every one of its
      enrollments' relationships before the TEI counts as eligible, since a
      protected relationship can attach at any tree level (this is also what
      makes 4.3 pass without further changes).

**Commit: 4.1 + 4.2 together.**

- [x] 4.3 Add a behavior test: an otherwise-eligible tracked entity instance is
      NOT purged because one of its enrollments has a relationship to a
      non-eligible counterpart — confirms the eligibility check from Group 2 is
      also applied at the enrollment level, not just the root TEI level, since a
      protected relationship can attach to any node in the tree being purged.
      Verify: test fails if the 3.2 selection query only checked the TEI's own
      relationships and ignored its enrollments'; passes once the enrollment
      check is added to the same selection query (extending 3.2, not a second
      independent check).

      Committed together with 4.1/4.2 (both were needed at once —
      `isTreeRelationshipEligible` already covers the enrollment level from the
      first implementation, so there was no separate red step for 4.3 alone).
      One test fixture bug found and fixed while writing 4.1: two TEIs shared
      the same `lastUpdated`, making which one `limit = 1` kept
      non-deterministic — gave them distinct timestamps. Verified: full
      `retention` package suite (35 tests) green on `Pixel_9a`; `ktlintCheck`
      and `:core:detekt` green.

**Commit: 4.3 alone** if 3.2/4.2 already cover it structurally (test-only);
otherwise 4.3 plus the minimal fix to extend the eligibility check to
enrollment-level relationships, as one red-green commit.

## 5. Wire eligibility check + cascade into `EventRetentionPurger` (TEI-less events)

- [x] 5.1 Add a behavior test: an eligible TEI-less event with a relationship to
      an eligible tracked entity instance — purging the event purges the
      relationship linking it too. Verify: test fails.
- [x] 5.2 Wire the eligibility check from Group 2 into `EventRetentionPurger`'s
      selection query, and call
      `RelationshipRetentionPurger.purgeForEntity(event.uid())` inline when a
      TEI-less event is purged. Verify: the 5.1 test passes.

**Commit: 5.1 + 5.2 together.**

- [x] 5.3 Add a behavior test: an otherwise-eligible TEI-less event is NOT
      purged because it has a relationship to a non-eligible counterpart.
      Verify: test passes against the 5.2 implementation (should already be
      green — negative case symmetric to 5.1's positive one).

      Committed together with 5.1/5.2 (both tests added to the same file in
      the same pass, both green on first run). Verified: full `retention`
      package suite (37 tests) green on `Pixel_9a`; `ktlintCheck` and
      `:core:detekt` green.

**Commit: 5.3 alone** (test-only).

## 6. Wire eligibility check + cascade into the tracker-rooted Event cascade step

Same reasoning as Group 4: events cascaded from an eligible TEI (via its
enrollments) also need both the eligibility check and the relationship cascade,
independently of the TEI-less path in Group 5.

- [x] 6.1 Add a behavior test: an eligible tracked entity instance's event (via
      an eligible enrollment) has a relationship to an eligible counterpart —
      purging the TEI cascades to purge the event, and the relationship linking
      it is purged too. Verify: test fails.
- [x] 6.2 Wire `RelationshipRetentionPurger.purgeForEntity(event.uid())` inline
      into the event-cascade step of `TrackedEntityRetentionPurger`. Verify: the
      6.1 test passes.

      Also extended `isTreeRelationshipEligible` with a new
      `isEnrollmentsEventsRelationshipEligible(enrollmentUid)` step, checking
      every event of every enrollment before the TEI counts as eligible — same
      reasoning as the Group 4 enrollment-level extension, one tree level
      deeper. One test fixture bug found while writing 6.1: two TEIs shared the
      same `lastUpdated`, making `limit = 1`'s outcome non-deterministic — same
      class of bug as Group 4's, fixed the same way (distinct timestamps).

**Commit: 6.1 + 6.2 together.**

- [x] 6.3 Add a behavior test: an otherwise-eligible tracked entity instance is
      NOT purged because one of its cascaded events has a relationship to a
      non-eligible counterpart — same reasoning as 4.3, one level deeper in the
      tree. Verify: test fails if the eligibility check only covers TEI +
      enrollment level and misses event level; passes once extended.

      Committed together with 6.1/6.2 (both tests added to the same file in
      the same pass; both green after the `isTreeRelationshipEligible`
      extension). Verified: full `retention` package suite (39 tests) green on
      `Pixel_9a`; `ktlintCheck` and `:core:detekt` green.

**Commit: 6.3 alone** if already covered structurally (test-only); otherwise 6.3
plus the minimal fix, as one red-green commit.

## 7. Full spec verification

- [x] 7.1 Walk every `#### Scenario:` added or modified by this change (both
      `specs/synced-data-retention-purge/spec.md`'s delta and
      `specs/synced-data-relationships-purge/spec.md`) and confirm each maps to
      at least one test added in Groups 1-6; add any scenario found without a
      corresponding test. Verify: one-to-one mapping documented in this task,
      no scenario left unverified.

      Mapping (scenario → test):
      - A relationship to a non-eligible counterpart protects the eligible
        side →
        `TrackedEntityRetentionPurgerIntegrationShould.keep_a_tei_that_has_a_relationship_to_a_non_eligible_counterpart`
      - A relationship between two fully synced trees does not block purge →
        `TrackedEntityRetentionPurgerIntegrationShould.purge_a_relationship_when_purging_both_of_its_fully_synced_teis`
      - Purging a tracked entity instance purges its relationships →
        `RelationshipRetentionPurgerIntegrationShould.purge_a_relationship_and_its_items_when_purging_one_of_its_two_teis`
        + `TrackedEntityRetentionPurgerIntegrationShould.purge_a_relationship_when_purging_both_of_its_fully_synced_teis`
      - A relationship between different record types is purged the same way →
        `TrackedEntityRetentionPurgerIntegrationShould.purge_a_relationship_when_purging_an_enrollment_related_to_an_eligible_event`
        + `TrackedEntityRetentionPurgerIntegrationShould.purge_a_relationship_when_purging_a_cascaded_event_related_to_an_eligible_counterpart`
        + `EventRetentionPurgerIntegrationShould.purge_a_relationship_when_purging_a_tei_less_event_related_to_an_eligible_tei`
      - Relationship purge does not depend on relationship directionality →
        **gap found, no existing test isolated this.** Added
        `RelationshipRetentionPurgerIntegrationShould.purge_a_relationship_the_same_way_regardless_of_which_side_triggers_the_purge`
        (verified `purgeForEntity` produces the identical result whether
        triggered from the `FROM` or the `TO` side — confirmed no
        `retention/internal` code reads `RelationshipType.bidirectional()` at
        all, unlike `DataStatePropagatorImpl`, so the behavior was already
        directionality-independent by construction; this closes the coverage
        gap).
      - A relationship whose counterpart record is already missing is purged →
        `RelationshipRetentionPurgerIntegrationShould.purge_a_relationship_without_error_when_its_relationship_row_is_already_missing`

**Commit: 7.1 alone**, only if it adds a missing test; if every scenario is
already covered, no commit is needed — record the mapping in the PR description
instead of an empty commit.

Adds a missing test (see mapping above) — committed alone.

- [x] 7.2 Run the full `core` test suite (unit + androidTest) and confirm green,
      with no pre-existing purger/wiper/test behavior changed. Verify: CI or
      local run passes.

      Verified locally: `:core:testDebugUnitTest` green;
      `:core:connectedDebugAndroidTest` on `Pixel_9a` (real emulator) — 5549
      tests run, 0 failed, 158 skipped (same pre-existing skip set as the
      prior change's 9.2/10.6 checkpoints).

**Commit: none** — verification only, no file changes expected.
