## 1. Program-scoped limit resolver (TEI/Event)

**Commit: 1.1 + 1.2 together** (red -> green).

- [x] 1.1 Add a failing test for a new `ProgramRetentionLimitResolver`
  collaborator (`core/src/test/java/org/hisp/dhis/android/core/retention/internal/ProgramRetentionLimitResolverShould.kt`,
  mockito-kotlin + `runTest`, mocking `ProgramSettingsObjectRepository`)
  covering: (a) a program with its own `teiDBTrimming`/`eventsDBTrimming`
  and `settingDBTrimming` uses that program's own value and scope, (b) a
  program with no specific setting falls back to `globalSettings()`, (c) no
  global or specific setting at all falls back to the existing hardcoded
  default, (d) no `settingDBTrimming` scope set falls back to `GLOBAL`.
- [x] 1.2 Implement `ProgramRetentionLimitResolver` (wraps
  `ProgramSettingsObjectRepository.blockingGet()` via
  `withContext(Dispatchers.IO)`, per design.md) exposing, for a given
  programUid and a field selector (`teiDBTrimming` vs `eventsDBTrimming`),
  the resolved `(limit: Int, scope: LimitScope)` pair. Verify: 1.1 passes.

## 2. Multi-program tracked entity instance grouping

**Commit: 2.1 + 2.2 together** (red -> green).

- [x] 2.1 Add a failing test asserting that, given a TEI's set of enrolled
  program uids, the effective limit used for grouping is the minimum of
  each program's resolved limit (design.md decision: most-restrictive-wins).
  Include the edge case of a TEI enrolled in only one program (limit equals
  that program's own resolved limit, unchanged from single-program
  behavior).
- [x] 2.2 Implement the resolution as a small function/extension taking a
  list of programUids and returning the effective `(limit, scope)` pair,
  reusing `ProgramRetentionLimitResolver` from Group 1. Verify: 2.1 passes.

## 3. `RetentionPurger` splits into read/write ports; `RetentionSelector` domain service; `TrackedEntityRetentionPurger` adapts

**Commit: 3.1 + 3.2 together** (red -> green). **Commit: 3.3 alone**
(added coverage, no new implementation).

> Supersedes an earlier version of this group that gave `RetentionPurger` a
> single `purge(resolveLimit: (groupKey) -> Int)` method. That was
> implemented, found to push grouping policy into the persistence adapter
> (broke the `GLOBAL`/single-pool regression test), and replaced by this
> read/write split — see design.md "`RetentionPurger` splits into a read
> port and a write port" for the full rationale.

- [x] 3.1 Add a failing unit test for a new `RetentionSelector` domain
  service (`core/src/test/.../RetentionSelectorShould.kt`, plain
  `RetentionCandidate` fixtures, no Room/androidTest needed) covering: (a)
  `GLOBAL` scope selects from one combined pool, dropping the resolved
  limit from the whole candidate list sorted by `lastUpdated` descending —
  regression-equivalent to today's single-pool behavior; (b) `PER_PROGRAM`
  scope groups candidates by `RetentionGroupKey.Program` and trims each
  group independently to its own resolved limit, so an over-limit
  program's excess is purged while another program's candidates, within
  their own separate limit, survive even though combining both pools would
  have exceeded a single global limit; (c) a multi-program `RetentionCandidate`
  (representing a TEI enrolled in more than one program) resolves to the
  most-restrictive of its programs' limits (reusing
  `TrackedEntityInstanceRetentionLimitResolver` from Group 2).
- [x] 3.2 Implement `RetentionCandidate` (uid, lastUpdated, programUids,
  organisationUnitUid — per design.md), `RetentionSelector` (the pure
  grouping/sorting/trimming service), and split `RetentionPurger` into
  `eligibleCandidates(): List<RetentionCandidate>` +
  `purge(uids: List<String>)`. Update `TrackedEntityRetentionPurger`:
  `eligibleCandidates()` returns its existing eligible-TEI query mapped to
  `RetentionCandidate` (programUids from that TEI's enrollments' distinct
  `Enrollment.program()` values, per design.md's multi-program decision);
  `purge(uids)` runs the existing cascade delete unchanged, keyed by the
  given uids instead of a freshly computed drop-list. Add a failing
  `core/src/androidTest/.../TrackedEntityRetentionPurgerIntegrationShould.kt`
  test exercising the full flow (selector + purger together) for the same
  two-programs-different-limits scenario as 3.1(b). Verify: 3.1 and the new
  androidTest pass, plus all pre-existing
  `TrackedEntityRetentionPurgerIntegrationShould` tests remain green
  (update their direct `.purge(limit = N)` call sites to go through
  `RetentionSelector.select(...)` + `purge(uids)`, or an equivalent
  test-only helper — `GLOBAL` scope must reduce to exactly today's
  single-pool behavior).
> Groups 3.3 onward rebuild grouping (`RetentionGroupKey`, per-group limits)
> incrementally, one commit at a time, each adding only what its own
> red -> green test requires — no scope/key is introduced ahead of a test
> that exercises it. This replaces the all-at-once `RetentionGroupKey`
> (5 variants) + `scope: LimitScope` design from the superseded version of
> Group 3: that shape is rebuilt piece by piece across 3.3-3.6 instead of
> landing in one step.

- [x] 3.3 Add a failing `RetentionSelectorShould` test: given two
  `RetentionCandidate`s tagged with different `programUid`s and a
  `limitByProgram: Map<String, Int>` giving a different limit per program,
  each program's excess is purged independently of the other's eligible
  count (same scenario previously covered by the superseded 3.1(b),
  rebuilt against the new API). Implement the minimum to pass: add
  `programUid: String?` to `RetentionCandidate` (single value, not a list —
  the multi-program TEI case is 3.4, not this task), and a
  `RetentionSelector.selectByProgram(candidates, limitByProgram: Map<String,
  Int>): List<String>` overload that groups by `programUid` directly (no
  `RetentionGroupKey` wrapper — nothing yet needs to distinguish grouping
  *kinds* at the type level, only to key a map by programUid) and drops
  each group's resolved limit, read from the map, not resolved by the
  selector itself: limits are resolved by the use case ahead of the call,
  since by the time `select`/`selectByProgram` runs the candidates
  (and therefore every distinct group key) are already known — see
  design.md's "`suspend (RetentionGroupKey) -> Int` callback" rejection.
  The existing `select(candidates, limit: Int)` overload (`GLOBAL`, one
  pool) stays unchanged, unaffected, still used by every existing caller.
- [x] 3.4 Add a failing `RetentionSelectorShould` test: a TEI candidate
  enrolled in two programs with different resolved limits is grouped under
  the most-restrictive one (mirrors design.md's most-restrictive-wins
  decision, using `TrackedEntityInstanceRetentionLimitResolver` from Group
  2 as the resolution rule inside the grouping step). Implement the
  minimum to pass: change `RetentionCandidate.programUid: String?` to
  `programUids: List<String>` (breaking change local to this internal
  package — no external caller yet, per design.md's existing risk note),
  update `TrackedEntityRetentionPurger.eligibleCandidates()` to populate it
  from that TEI's distinct `Enrollment.program()` values (query already
  exists from Group 3.2, currently unused since 3.2/3.3's YAGNI pass —
  reintroduce it here where a test needs it), update `selectByProgram` to
  group each candidate under whichever of its `programUids` has the
  smallest limit in `limitByProgram` (`programUids.minBy {
  limitByProgram.getValue(it) }`).
- [x] 3.5 Add two `TrackedEntityRetentionPurgerIntegrationShould` tests
  exercising both sides of Group 3.4's change end to end against Room: (a)
  `eligibleCandidates()` populates `programUids` from each TEI's distinct
  enrollment programs (single-program and multi-program TEIs); (b)
  `purge(uids)`'s cascade delete (TEI + all its enrollments) is unaffected
  by a TEI having more than one enrollment/program — same assertion shape
  as every other cascade test in this class, uids fixed by hand.
  > Superseded an earlier version of this task that additionally called
  > `RetentionSelector.selectByProgram(...)` inside the test and asserted
  > on its purge output — an Ugly Mirror: the test re-executed the exact
  > selection pipeline `SyncedDataRetentionPurger` will run in production,
  > so a bug in `selectByProgram`'s grouping/most-restrictive-wins logic
  > would reproduce in the test's own expected result and stay green, and
  > an internal, behavior-preserving change to `selectByProgram` (e.g. a
  > different tie-breaking implementation) would break this integration
  > test even though the purger's actual observable contract (given fixed
  > `uids`, purge exactly those) never changed. `RetentionSelectorShould`
  > (3.3/3.4) already covers grouping/most-restrictive-wins correctness in
  > isolation, without Room; this task now only covers what only Room can
  > verify — that `eligibleCandidates()` reads real enrollments correctly
  > — and purges by explicit `uids`, same as every other test in this
  > class.
- [x] 3.6 Add a `RetentionSelectorShould` unit test for `PER_ORG_UNIT`
  scope (two org units, each with its own eligible candidates beyond a
  shared per-org-unit limit) verifying each org unit's excess is trimmed
  independently of the other's eligible count. Implemented the minimum:
  added `organisationUnitUid: String?` to `RetentionCandidate` and a
  `RetentionSelector.selectByOrgUnit(candidates, limitByOrgUnit:
  Map<String, Int>): List<String>` overload — a sibling of
  `selectByProgram`, same group/sort/trim shape, no shared abstraction
  between them.
  > Superseded an earlier version of this task that generalized
  > `selectByProgram`/`selectByOrgUnit` into a single `selectGrouped(
  > candidates, limitByGroup: Map<RetentionGroupKey, Int>, groupKeyFor:
  > (RetentionCandidate) -> RetentionGroupKey)`, adding back both
  > `RetentionGroupKey` and a `groupKeyFor` callback. Rejected on review:
  > (a) `groupKeyFor` is the same callback shape already rejected for
  > `limitFor` in design.md's "`RetentionPurger` splits into a read port
  > and a write port" section, for the same reason — no caller exists yet
  > (`SyncedDataRetentionPurger` doesn't call any of these methods until
  > Group 6) to decide between `Program`/`OrgUnit`, so there was nothing
  > real forcing the generalization; (b) it was written to anticipate what
  > Group 6 might need rather than what this task's own red test required,
  > violating the "no scope/key introduced ahead of a test that exercises
  > it" rule this same tasks.md states above. The two-sibling-methods
  > duplication this leaves is intentional and expected until Group 6
  > brings a real caller that needs to choose between them polymorphically
  > — only then does generalizing have a consumer to justify it.

## 4. `EventRetentionPurger` adapts to program-scoped grouping

**Commit: 4.1 + 4.2 together** (red -> green).

- [x] 4.1 + 4.2 Update `EventRetentionPurger.eligibleCandidates()` to
  populate `programUids` (single-element, from `Event.program()` directly
  — TEI-less events carry their own program field, no multi-program
  ambiguity like TEIs) and `organisationUnitUid` (from
  `Event.organisationUnit()`); `purge(uids)` stays unchanged (already keyed
  by uids since Group 3.2). Add an `EventRetentionPurgerIntegrationShould`
  test asserting `eligibleCandidates()` populates both fields correctly
  from Room, same shape as 3.5's rebuilt test — no call to
  `RetentionSelector` from this test: nothing in production composes
  `EventRetentionPurger` with `RetentionSelector.selectByProgram`/
  `selectByOrgUnit` yet (that wiring is Group 6), so asserting on that
  composition here would be the same Ugly Mirror rejected in 3.5.
  `RetentionSelectorShould` (3.3-3.6) already covers grouping correctness
  in isolation; pre-existing `purge(uids)` tests remain green, untouched.

## 5. Dataset-scoped limit resolver and `DataValueRetentionPurger` grouping

**Commit: 5.1 + 5.2 together** (red -> green).

- [x] 5.1 Add a failing test for a new `DataSetRetentionLimitResolver`
  (mirrors Group 1 but for `DataSetSettingsObjectRepository` /
  `DataSetSetting.periodDSDBTrimming()` — no `LimitScope` field on
  `DataSetSetting`, so this resolver returns only a limit, grouped by
  datasetUid, never further split by org unit per design.md) plus a failing
  `RetentionSelectorShould` test for `selectByDataset` grouping/most-
  restrictive-wins (same shape as `selectByProgram`'s tests).
- [x] 5.2 Implement `DataSetRetentionLimitResolver`. `DataValue` has no
  `dataSet` field at all (neither locally nor server-side — see design.md
  "DataValue → dataset resolution is ambiguous"), so instead of a single
  grouping key from a non-existent field: added
  `DataSetElementStore.getDataSetsForDataElement(dataElementUid)` (new
  `// EyeSeeTea customization`, resolves via `DataSetDataElementLink`) and
  `RetentionCandidate.ByDataset(dataSetUids: List<String>)` — a sealed-class
  variant, not a `RetentionGroupKey` (that abstraction was rejected twice
  already in Group 3 for having no real caller; see design.md). Updated
  `DataValueRetentionPurger.eligibleCandidates()` to populate `dataSetUids`
  via the new store method; `selectByDataset` applies most-restrictive-wins
  over the list exactly like `selectByProgram` does for `programUids` — no
  separate "combined resolver" needed. `purge(uids)` stays unchanged.
  Verify: 5.1 passes, and a `GLOBAL`/no-specific-setting case reduces to
  today's single-pool behavior (regression check against existing tests
  using the ungrouped `select` overload).
- [x] 5.3 Turn `RetentionCandidate` into a sealed class
  (`ByProgramAndOrgUnit`, `ByDataset`) instead of one data class with every
  field nullable/defaulted — a flat shape let `selectByDataset` be called
  with `Event`/TEI candidates (wrong field populated) and fail only at
  runtime; the sealed class makes that a compile error. Discovered while
  wiring 5.2: `TrackedEntityInstance` has its own `organisationUnit()`
  (registration org unit, not derived from enrollments) that
  `TrackedEntityRetentionPurger.eligibleCandidates()` was not populating —
  now populated, so TEI and Event share the `ByProgramAndOrgUnit` shape.
  `organisationUnitUid`/`programUids` elements are non-nullable: confirmed
  `Event`/`TrackedEntityInstance` local creation requires them, and
  `EventHandler.deleteIfCondition` deletes any downloaded event with a null
  `organisationUnit` before it can be marked `SYNCED` — see design.md.

## 6. Wire resolvers and grouped selection into `SyncedDataRetentionPurger`

**Commit: 6.1 + 6.2 together** (red -> green).

- [ ] 6.1 Add a failing test asserting `SyncedDataRetentionPurger` no longer
  requires a caller-supplied `RetentionLimits` for TEI/Event/DataValue —
  it now orchestrates, per entity type: `purger.eligibleCandidates()` ->
  resolve the run's `LimitScope` via Groups 1/5's resolvers ->
  `GLOBAL` calls `RetentionSelector.select(...)`, any other resolved scope
  calls the matching grouped-select from Groups 3-5 -> `purger.purge(uids)`;
  `OrphanFileResourceRetentionPurger` keeps receiving an explicit limit via
  its own unchanged `purge(limit: Int)` (no corresponding setting per
  design.md).
- [ ] 6.2 Update `RetentionLimits`/`SyncedDataRetentionPurger.purge(...)`
  signature accordingly (exact shape per design.md — likely `RetentionLimits`
  shrinks to just the file resource limit, or is removed entirely in favor
  of a single explicit file-resource-limit parameter). Verify: 6.1 passes,
  full `:core` unit + androidTest suite green (`./gradlew testDebugUnitTest`
  plus instrumented retention tests on `Pixel_9a`).

## 7. `PER_OU_AND_PROGRAM` and `ALL_ORG_UNITS` scopes

**Commit: 7.1 + 7.2 together** (red -> green).

- [x] 7.1 Added failing `RetentionSelectorShould` tests for
  `selectByOrgUnitAndProgram` (a candidate's group is the combination of
  its org unit and most-restrictive program; org units of the same
  program are kept as separate groups). `ALL_ORG_UNITS`'s equivalence to
  `PER_ORG_UNIT` is instead covered end to end in
  `SyncedDataRetentionPurgerIntegrationShould` (`apply_all_org_units_scope_the_same_way_as_per_org_unit`),
  since the equivalence is a property of `SyncedDataRetentionPurger`'s
  `when (scope)` branch (both scopes already routed to `selectByOrgUnit`
  in Group 6), not of `RetentionSelector` itself, which has no notion of
  `LimitScope`.
- [x] 7.2 Implemented `RetentionSelector.selectByOrgUnitAndProgram(candidates:
  List<RetentionCandidate.ByProgramAndOrgUnit>, limitByOrgUnitAndProgram:
  Map<Pair<String, String>, Int>)` — a sibling method, not a
  `RetentionGroupKey` variant (that abstraction was rejected repeatedly in
  Group 3 for having no real caller at the time; see design.md). Wired
  into `SyncedDataRetentionPurger`'s `PER_OU_AND_PROGRAM` branch: the
  limit for each `(orgUnit, program)` combination present among the
  candidates is the program's own resolved limit (same source `PER_PROGRAM`
  uses — no per-combination setting exists), keyed additionally by org
  unit so each org unit's candidates of that program compete for their
  own slice instead of sharing one program-wide pool. Verify: 7.1 passes,
  full `:core` suite green (unit + androidTest on `Pixel_9_Pro(AVD)`).

## 8. Full verification

- [x] 8.1 Mapped every scenario against a real test:

  From `specs/synced-data-retention-scope/spec.md`:
  | Scenario | Test |
  |---|---|
  | A program-specific limit overrides the global limit | `ProgramRetentionLimitResolverShould.use_specific_program_value_and_scope_when_present` |
  | No program-specific limit falls back to the global limit | `ProgramRetentionLimitResolverShould.fall_back_to_global_settings_when_no_specific_setting_for_program` |
  | No configured limit at all falls back to the default | `ProgramRetentionLimitResolverShould.fall_back_to_hardcoded_default_when_no_global_or_specific_setting` (TEI/Event) + `DataSetRetentionLimitResolverShould.fall_back_to_hardcoded_default_when_no_global_or_specific_setting` (dataset) |
  | A data-set-specific limit overrides the global limit | `DataSetRetentionLimitResolverShould.use_specific_data_set_value_when_present` |
  | One program's excess does not consume another program's quota | `RetentionSelectorShould.trim_each_programs_excess_candidates_independently_of_the_other_programs_eligible_count` |
  | A per-organisation-unit scope trims each organisation unit independently | `RetentionSelectorShould.trim_each_org_units_excess_candidates_independently_of_the_other_org_units_eligible_count` |
  | A tracked entity instance enrolled in multiple programs uses the most restrictive limit | `RetentionSelectorShould.group_a_multi_program_candidate_under_its_most_restrictive_programs_limit` |
  | File resource orphan trimming has no per-program scope | `OrphanFileResourceRetentionPurgerIntegrationShould.purge_an_eligible_file_resource_beyond_the_limit_together_with_its_physical_file` (single-pool `purge(limit)`, no scope involved) — confirmed in production code: `SyncedDataRetentionPurger.purge()` always passes the `NO_ORPHANS_ALLOWED = 0` constant, never resolving any scope for this type |

  From `specs/synced-data-retention-purge/spec.md`:
  | Scenario | Test |
  |---|---|
  | Only the oldest excess records are purged | `RetentionSelectorShould.select_the_oldest_candidates_beyond_the_limit_from_one_combined_pool` |
  | Non-eligible records are never counted toward the limit | `TrackedEntityRetentionPurgerIntegrationShould.keep_a_tracked_entity_instance_whose_aggregated_sync_state_is_not_synced` + `EventRetentionPurgerIntegrationShould.keep_a_tei_less_event_whose_own_aggregated_sync_state_is_not_synced` (non-eligible records are excluded from `eligibleCandidates()` itself, so they never reach counting or purge) |
  | A limit of zero purges everything eligible | `RetentionSelectorShould.select_every_candidate_when_limit_is_zero` (added — was previously only exercised indirectly via `purge(uids)` with a hand-fixed list, never through `select(candidates, limit)` itself) |
  | Fewer eligible records than the limit purges nothing | `RetentionSelectorShould.select_nothing_when_candidates_are_at_or_below_the_limit` (added — same gap as above) |

  **Gap found and closed**: `RetentionSelectorShould` had no test exercising
  `select(candidates, limit)` — the actual mechanism translating "limit" into
  "uids to purge" — at `limit = 0` or with `candidates.size <= limit`. Both
  were only covered indirectly through purger integration tests that call
  `purge(uids)` with a hand-fixed list, bypassing selection entirely. Added
  `select_every_candidate_when_limit_is_zero` and
  `select_nothing_when_candidates_are_at_or_below_the_limit`.

  Full `:core` suite green: `testDebugUnitTest` and `connectedDebugAndroidTest`
  (package `org.hisp.dhis.android.core.retention.internal`, 46/46 on
  `Pixel_9_Pro(AVD)`) both pass, `ktlintCheck` clean.

## 9. Expose `SyncedDataRetentionPurger` publicly via a new `RetentionModule`

**Commit: 9.1 + 9.2 together** (additive, no red -> green — `purge()`'s
behavior is already implemented and fully covered by
`SyncedDataRetentionPurgerIntegrationShould`; this group only makes it
reachable from outside `core`).

- [x] 9.1 Added `RetentionModule` (public interface,
  `core/src/main/java/org/hisp/dhis/android/core/retention/RetentionModule.kt`
  — outside `retention/internal`, matching where `WipeModule`/`SettingModule`
  live relative to their own `internal/` implementations) with a single
  `suspend fun purge()`. Added `RetentionModuleImpl`
  (`retention/internal/RetentionModuleImpl.kt`, `@Singleton`) delegating to
  the existing `SyncedDataRetentionPurger.purge()` — no new business logic.
- [x] 9.2 Wired the same way `wipeModule` is wired: `val retentionModule:
  RetentionModule` added to `D2DIComponent`'s constructor, and `fun
  retentionModule(): RetentionModule { return d2DIComponent.retentionModule
  }` added to `D2.kt`. Added `RetentionModuleImplShould` confirming the
  delegate calls through (a DI/wiring smoke test, not a re-test of
  `purge()`'s behavior — see design.md "RetentionModule"). Verified: full
  `:core` suite green (`testDebugUnitTest` + `connectedDebugAndroidTest`,
  package `org.hisp.dhis.android.core.retention.internal`, 46/46 on
  `Pixel_9_Pro(AVD)`), `ktlintCheck` clean.

Out of scope for this group (see design.md "Out of scope" under
`RetentionModule`): publishing a new SDK release, bumping `dhis2sdk` in
`dhis2-android-capture-app-extra`'s `gradle/libs.versions.toml`, and
rewriting that app's `AndroidSyncRepository.purgeSyncedData()` (which today
calls a non-existent `d2.wipeModule().wipeSyncedData()`) to call
`d2.retentionModule().purge()` instead — all tracked in the app repository.
