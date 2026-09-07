## Why

`synced-data-retention-purge` (already implemented and archived) purges the
oldest fully synced records once their count exceeds a limit, but that limit
is a single hardcoded `RetentionLimits` value — the same number for every
program and data set, for every org unit. The client (Miquel, 13 Jul) already
confirmed the count limit must follow the same global/per-program-or-dataset
pattern used elsewhere in the app's settings, and the SDK already models this:
`ProgramSetting.teiDBTrimming` / `.eventsDBTrimming` / `.settingDBTrimming`
(`LimitScope`) and `DataSetSetting.periodDSDBTrimming` are already synced and
persisted from the Settings Web App model, but nothing in the SDK reads them
today. This change closes that gap by making `SyncedDataRetentionPurger`
resolve its limits from these existing settings instead of a fixed value.

## What Changes

- Add a scope-resolution step that, for the TEI and Event retention limits,
  looks up `ProgramSetting.teiDBTrimming` / `.eventsDBTrimming` and
  `.settingDBTrimming` (`LimitScope`) following the same global/per-program
  precedence already used for download limits (`ProgramSettings
  .specificSettings()` override, falling back to `ProgramSettings
  .globalSettings()`, falling back to today's hardcoded default when no
  setting exists at all).
- Add an equivalent lookup for the DataValue retention limit via
  `DataSetSetting.periodDSDBTrimming`, following the same global/per-dataset
  precedence (`DataSetSettings.globalSettings()` / `.specificSettings()`);
  `DataSetSetting` has no `LimitScope` field of its own, so this limit is
  always applied per-dataset when a specific setting exists, with no
  further org-unit split (matches today's data model — see design.md).
- **BREAKING**: when `LimitScope` resolves to `PER_PROGRAM` (or
  `PER_OU_AND_PROGRAM`), the retention count limit is enforced *per program*,
  not globally across all programs — a TEI/Event purger candidate set that
  today is selected and trimmed as one pool becomes multiple pools, one per
  programUid, each trimmed to its own resolved limit. This changes the
  `RetentionPurger` contract: instead of a single `purge(limit: Int)`
  method, it splits into a read (`eligibleCandidates()`) and a write
  (`purge(uids: List<String>)`), with grouping/sorting/trimming moved to a
  new `RetentionSelector` domain service — for the trackedentity and event
  purgers; the data value purger gets the equivalent per-dataset grouping.
  `ALL_ORG_UNITS`/`PER_ORG_UNIT`/`PER_OU_AND_PROGRAM` splitting by org unit
  is addressed the same way (grouping key extended with org unit) — see
  design.md for the exact grouping key per scope value and the rationale
  for the two-port split.
- `RetentionLimits` changes from a single flat value set into a value
  resolved per program/dataset/org-unit group before each purger runs —
  **BREAKING** for any caller currently constructing `RetentionLimits`
  directly, since the resolution now requires reading
  `ProgramSettings`/`DataSetSettings` rather than accepting bare ints.
- Explicitly out of scope (per client decision, 13 Jul): notifying the user
  when a record is purged, and whether TEI-less events are grouped with or
  counted separately from dataset events under the event limit — both left
  for a future decision, not part of this change.

## Capabilities

### New Capabilities
- `synced-data-retention-scope`: resolving the effective per-entity retention
  limit (TEI, Event, DataValue) from existing program/dataset settings,
  following the global → specific → hardcoded-default precedence already
  established for download limits.

### Modified Capabilities
- `synced-data-retention-purge`: the requirement describing how
  `RetentionLimits` values are obtained changes from "supplied by the caller
  as a fixed value" to "resolved per entity from settings before each purge
  run" — the purge/eligibility/cascade behavior itself (order, tree
  eligibility, transactionality) does not change.

## Impact

- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionLimits.kt`
  — shape changes from 4 flat `Int`s to a resolved-per-run value (exact shape
  decided in design.md).
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionPurger.kt`
  — the shared interface every value purger implements changes from
  `purge(limit: Int)` (one pool) to two methods, `eligibleCandidates():
  List<RetentionCandidate>` and `purge(uids: List<String>)` — the
  highest-conflict-surface option available here, but required to keep
  grouping/limit policy out of the persistence adapter (see design.md);
  touches `TrackedEntityRetentionPurger`, `EventRetentionPurger`,
  `DataValueRetentionPurger` (3 implementations, not ~30+ like
  `ModuleWiper`). `OrphanFileResourceRetentionPurger` keeps today's
  single-pool `purge(limit: Int)` behavior and does not implement
  `RetentionPurger` (no corresponding setting to group by — see design.md).
- New `RetentionSelector` domain service in `retention/internal` — pure
  grouping/sorting/trimming logic (`RetentionCandidate` list + resolved
  `LimitScope` + a per-group limit lookup -> uids to purge), with no
  dependency on Room or any store; used by `SyncedDataRetentionPurger`
  between each purger's `eligibleCandidates()` and `purge(uids)` calls.
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurger.kt`
  — gains a dependency on the new scope resolvers and `RetentionSelector`
  instead of accepting `RetentionLimits` as an opaque caller-supplied
  parameter; becomes the sole orchestrator that reads candidates, resolves
  scope, selects uids, and purges them per entity type.
- New collaborator(s) in `retention/internal` for resolving limits and
  grouping keys from `ProgramSettingsObjectRepository` /
  `DataSetSettingsObjectRepository` (existing repositories, not modified).
- Affected domain modules: `trackedentity` (TEI limit), `event` (event
  limit), `datavalue` (data value limit) — all consumers via
  `ProgramSetting`/`DataSetSetting`, which are not modified themselves.
  `relationship` retention is unaffected. `fileresource` retention
  (`fileResource` limit in `RetentionLimits`) has no corresponding setting
  field and stays a hardcoded, ungrouped default — see design.md.
- No `ModuleWiper` interface is touched by this change.
- Downstream: the caller that currently builds `RetentionLimits` (not yet
  wired into any app — `SyncedDataRetentionPurger` has no production caller
  yet per the retention-purge spec) will need to stop supplying limits
  directly once this lands; that wiring is app-side work tracked separately,
  not part of this SDK change.
