## 1. Program-scoped limit resolver (TEI/Event)

**Commit: 1.1 + 1.2 together** (red -> green).

- [ ] 1.1 Add a failing test for a new `ProgramRetentionLimitResolver`
  collaborator (`core/src/test/java/org/hisp/dhis/android/core/retention/internal/ProgramRetentionLimitResolverShould.kt`,
  mockito-kotlin + `runTest`, mocking `ProgramSettingsObjectRepository`)
  covering: (a) a program with its own `teiDBTrimming`/`eventsDBTrimming`
  and `settingDBTrimming` uses that program's own value and scope, (b) a
  program with no specific setting falls back to `globalSettings()`, (c) no
  global or specific setting at all falls back to the existing hardcoded
  default, (d) no `settingDBTrimming` scope set falls back to `GLOBAL`.
- [ ] 1.2 Implement `ProgramRetentionLimitResolver` (wraps
  `ProgramSettingsObjectRepository.blockingGet()` via
  `withContext(Dispatchers.IO)`, per design.md) exposing, for a given
  programUid and a field selector (`teiDBTrimming` vs `eventsDBTrimming`),
  the resolved `(limit: Int, scope: LimitScope)` pair. Verify: 1.1 passes.

## 2. Multi-program tracked entity instance grouping

**Commit: 2.1 + 2.2 together** (red -> green).

- [ ] 2.1 Add a failing test asserting that, given a TEI's set of enrolled
  program uids, the effective limit used for grouping is the minimum of
  each program's resolved limit (design.md decision: most-restrictive-wins).
  Include the edge case of a TEI enrolled in only one program (limit equals
  that program's own resolved limit, unchanged from single-program
  behavior).
- [ ] 2.2 Implement the resolution as a small function/extension taking a
  list of programUids and returning the effective `(limit, scope)` pair,
  reusing `ProgramRetentionLimitResolver` from Group 1. Verify: 2.1 passes.

## 3. Group-aware `RetentionPurger` and `TrackedEntityRetentionPurger`

**Commit: 3.1 + 3.2 together** (red -> green). **Commit: 3.3 alone**
(added coverage, no new implementation).

- [ ] 3.1 Add a failing `core/src/androidTest/.../TrackedEntityRetentionPurgerIntegrationShould.kt`
  test: two programs with different `PER_PROGRAM` resolved limits (one
  eligible TEI beyond its program's limit, one eligible TEI within its own
  program's separate limit) — assert the over-limit program's excess TEI is
  purged while the other program's TEI, within its own limit, survives even
  though combining both pools into one would have exceeded a single global
  limit.
- [ ] 3.2 Change `RetentionPurger.purge(limit: Int)` to a group-aware form
  (per design.md — exact signature at implementer's discretion, e.g.
  `purge(resolveLimit: suspend (groupKey) -> Int)` or an equivalent taking a
  pre-resolved `Map<groupKey, Int>`), update `TrackedEntityRetentionPurger`
  to group its eligible TEI candidates by resolved `LimitScope` (using
  Groups 1-2 for the program dimension, `TrackedEntityInstance
  .organisationUnit()` for the org-unit dimension per design.md's grouping
  table) before sorting/dropping per group. Verify: 3.1 passes, plus all
  pre-existing `TrackedEntityRetentionPurgerIntegrationShould` tests remain
  green (`GLOBAL` scope must reduce to exactly today's single-pool
  behavior).
- [ ] 3.3 Add a test for `PER_ORG_UNIT` scope (two org units under the same
  program, each with its own eligible TEIs beyond a shared per-org-unit
  limit) verifying each org unit's excess is trimmed independently of the
  other's eligible count.

## 4. Group-aware `EventRetentionPurger`

**Commit: 4.1 + 4.2 together** (red -> green).

- [ ] 4.1 Add a failing `EventRetentionPurgerIntegrationShould` test: two
  programs with different `PER_PROGRAM` resolved event limits, each with
  TEI-less eligible events beyond their own program's limit — assert each
  program's excess is purged independently (mirrors 3.1 for events, using
  `Event.program()` directly since TEI-less events carry their own program
  field).
- [ ] 4.2 Update `EventRetentionPurger` to implement the group-aware
  `RetentionPurger` form from Group 3, grouping eligible events by resolved
  scope using `Event.program()` and `Event.organisationUnit()` directly (no
  multi-program ambiguity here, unlike TEIs — an event belongs to exactly
  one program). Verify: 4.1 passes, pre-existing tests remain green under
  `GLOBAL` scope.

## 5. Dataset-scoped limit resolver and `DataValueRetentionPurger`

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
- [ ] 5.2 Implement `DataSetRetentionLimitResolver` and update
  `DataValueRetentionPurger` to implement the group-aware `RetentionPurger`
  form, grouping by `DataValue.dataSet()`. Verify: 5.1 passes, and a
  `GLOBAL`/no-specific-setting case reduces to today's single-pool
  behavior (regression check against existing tests).

## 6. Wire resolvers into `SyncedDataRetentionPurger`

**Commit: 6.1 + 6.2 together** (red -> green).

- [ ] 6.1 Add a failing test asserting `SyncedDataRetentionPurger` no longer
  requires a caller-supplied `RetentionLimits` for TEI/Event/DataValue —
  those three purgers resolve their own limits internally via Groups 1-5;
  `OrphanFileResourceRetentionPurger` keeps receiving an explicit limit
  (unchanged, no corresponding setting per design.md).
- [ ] 6.2 Update `RetentionLimits`/`SyncedDataRetentionPurger.purge(...)`
  signature accordingly (exact shape per design.md — likely `RetentionLimits`
  shrinks to just the file resource limit, or is removed entirely in favor
  of a single explicit file-resource-limit parameter). Verify: 6.1 passes,
  full `:core` unit + androidTest suite green (`./gradlew testDebugUnitTest`
  plus instrumented retention tests on `Pixel_9a`).

## 7. Full verification

- [ ] 7.1 Map every scenario in
  `openspec/changes/synced-data-retention-scope/specs/synced-data-retention-scope/spec.md`
  and the modified requirement in
  `specs/synced-data-retention-purge/spec.md` against a real test, listing
  the test name next to each scenario; close any gap found. Run the full
  `:core` suite (unit + instrumented) and confirm 0 failures before marking
  this change ready to archive.
