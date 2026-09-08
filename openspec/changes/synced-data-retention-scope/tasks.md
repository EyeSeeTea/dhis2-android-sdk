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

- [ ] 5.1 Add a failing test for a new `DataSetRetentionLimitResolver`
  (mirrors Group 1 but for `DataSetSettingsObjectRepository` /
  `DataSetSetting.periodDSDBTrimming()` — no `LimitScope` field on
  `DataSetSetting`, so this resolver returns only a limit, grouped by
  datasetUid, never further split by org unit per design.md) plus a failing
  `DataValueRetentionPurgerIntegrationShould` test: two data sets with
  different resolved limits, each with eligible data values beyond their
  own data set's limit — assert each data set's excess is purged
  independently of the other's eligible count.
- [ ] 5.2 Implement `DataSetRetentionLimitResolver`, add
  `RetentionGroupKey.Dataset(dataSetUid: String)`, update
  `DataValueRetentionPurger.eligibleCandidates()` to populate a dataset
  grouping key from `DataValue.dataSet()` (no org-unit dimension per
  design.md); `purge(uids)` stays unchanged. Verify: 5.1 passes, and a
  `GLOBAL`/no-specific-setting case reduces to today's single-pool
  behavior (regression check against existing tests using the ungrouped
  `select` overload).

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

- [ ] 7.1 Add a failing `RetentionSelectorShould` test for
  `PER_OU_AND_PROGRAM` (a candidate's group is the combination of its
  org unit and most-restrictive program) and confirm `ALL_ORG_UNITS`
  reduces to the same grouping key as `PER_ORG_UNIT` per design.md's
  table (no separate implementation needed, only a test documenting the
  equivalence).
- [ ] 7.2 Implement `RetentionGroupKey.OuAndProgram(organisationUnitUid,
  programUid)` and wire it into the classifier used by
  `SyncedDataRetentionPurger` from Group 6. Verify: 7.1 passes, full
  `:core` suite green.

## 8. Full verification

- [ ] 8.1 Map every scenario in
  `openspec/changes/synced-data-retention-scope/specs/synced-data-retention-scope/spec.md`
  and the modified requirement in
  `specs/synced-data-retention-purge/spec.md` against a real test, listing
  the test name next to each scenario; close any gap found. Run the full
  `:core` suite (unit + instrumented) and confirm 0 failures before marking
  this change ready to archive.
