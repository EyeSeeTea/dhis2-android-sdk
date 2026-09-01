## Context

See proposal.md - Why/What Changes for motivation and scope. Current state
relevant to this design:

- `SyncedDataRetentionPurger.purge(limits: RetentionLimits)` calls 4
  `RetentionPurger.purge(limit: Int)` implementations
  (`DataValueRetentionPurger`, `TrackedEntityRetentionPurger`,
  `EventRetentionPurger`, `OrphanFileResourceRetentionPurger`) inside one
  `d2CallExecutor.executeD2CallTransactionally` block. Each implementation
  selects its own eligible candidates (already filtered by
  `aggregatedSyncState == SYNCED` and, for TEI/Event, by relationship
  eligibility — both existing and untouched by this change), sorts them by
  `lastUpdated()` descending, and drops the first `limit` — the survivors are
  purged. Today `limit` is one flat number for the whole candidate pool.
- `ProgramSetting.teiDBTrimming` / `.eventsDBTrimming` / `.settingDBTrimming`
  (`LimitScope`) and `DataSetSetting.periodDSDBTrimming` already exist,
  already synced, with no consumer today.
- `LimitScope` has 5 values: `GLOBAL`, `PER_PROGRAM`, `PER_ORG_UNIT`,
  `PER_OU_AND_PROGRAM`, `ALL_ORG_UNITS`. `ProgramSettings` exposes
  `globalSettings(): ProgramSetting` and
  `specificSettings(): Map<String, ProgramSetting>` (keyed by programUid);
  `DataSetSettings` mirrors this shape for dataset uid, but `DataSetSetting`
  has no `LimitScope` field — a dataset's trimming limit is always a single
  per-dataset (or global) number, never further split by org unit.
- The existing precedence for download limits
  (`TrackerQueryFactoryCommonHelper.getConfigLimit`/`hasLimitByOrgUnit`) is:
  specific-program override wins if present; otherwise fall back to
  `globalSettings()`; otherwise a hardcoded SDK default. This design reuses
  that precedence, not a new one.

## Goals / Non-Goals

**Goals:**
- Resolve the TEI, Event, and DataValue retention limits from
  `ProgramSetting`/`DataSetSetting` instead of a caller-supplied constant,
  honoring `LimitScope` (`GLOBAL`, `PER_PROGRAM`, `PER_ORG_UNIT`,
  `PER_OU_AND_PROGRAM`, `ALL_ORG_UNITS`) for TEI/Event, and the
  global/per-dataset precedence for DataValue.
- When a scope splits the pool (by program and/or org unit), enforce the
  resolved limit independently per group, not as one combined pool.

**Non-Goals:**
- Changing purge eligibility, ordering, cascade, or transactionality —
  those requirements are untouched (see `synced-data-retention-purge`
  spec).
- Adding a `LimitScope`-equivalent for `fileResource`'s limit — no such
  setting field exists on any settings model; it stays a hardcoded,
  ungrouped default, same as today.
- Adding a settings UI in the Android Settings Web App to let admins set
  these trimming values — Oslo's app today only exposes *download* limit
  fields, not trimming ones. This design makes the SDK ready to consume the
  values the moment they can be set; it does not add the ability to set
  them. Until then, the resolver falls back to `globalSettings()` /
  hardcoded default exactly as if the field were unset, so behavior is
  unchanged for any deployment where Oslo has not yet added the UI.
- Deciding whether TEI-less events are grouped with or counted separately
  from dataset events — explicitly deferred per client decision (see
  proposal.md).

## Decisions

### Grouping key per scope value (TEI/Event)

`LimitScope` decides the grouping key used before sorting/trimming a
candidate pool:

| `LimitScope`         | Grouping key                          |
|----------------------|----------------------------------------|
| `GLOBAL`              | none — one pool, current behavior     |
| `PER_PROGRAM`         | programUid                            |
| `PER_ORG_UNIT`        | organisationUnitUid                   |
| `PER_OU_AND_PROGRAM`  | (organisationUnitUid, programUid)     |
| `ALL_ORG_UNITS`       | organisationUnitUid (same grouping key as `PER_ORG_UNIT`; the difference between the two is only relevant to *download* scoping — for retention there is no query-splitting concern, so both are treated identically here) |

The resolved `LimitScope` for a run comes from `settingDBTrimming` (specific
program setting first, falling back to `globalSettings().settingDBTrimming()`,
falling back to `GLOBAL` if neither is set) — same precedence as scope
resolution for downloads.

### A tracked entity instance's group when it spans multiple programs

`TrackedEntityInstance` has no program field of its own — a program is only
known through its `Enrollment`s (`Enrollment.program()`), and a single TEI
can have enrollments in more than one program. The purge unit is the whole
TEI (cascading to every enrollment, event, and value it owns) — there is no
way to purge "just the enrollment in program A" while leaving the TEI's
enrollment in program B untouched. So a TEI with enrollments in programs
with different `PER_PROGRAM` limits cannot be assigned to a single group by
looking at any one enrollment in isolation.

**Decision**: resolve each of a TEI's enrollment programs to its own limit,
then use the smallest resolved limit as the TEI's effective group limit
(the most conservative option protects the most restrictive program's
quota). Concretely: for a TEI with enrollments in programs P1 (limit 50) and
P2 (limit 200), the TEI is evaluated as if its limit were 50 — i.e. it
competes for space in the smaller quota, never allowed to be "hidden" behind
a more generous program's larger allowance.

**Alternatives considered and rejected**:
- *Only group TEIs with exactly one enrollment program; multi-program TEIs
  always fall back to GLOBAL* — simpler, but silently exempts exactly the
  TEIs an admin most needs the limit to protect against (a TEI actively
  used across two programs is not an edge case in tracker data).
- *Group by the most recently updated enrollment's program* — ties the
  grouping decision to enrollment recency, which can flip a TEI between
  groups run to run as its enrollments are updated, making the resolved
  limit unstable and harder to reason about than the always-deterministic
  minimum.

### `RetentionPurger` becomes group-aware

**Decision**: change `RetentionPurger.purge(limit: Int)` to
`RetentionPurger.purge(resolveLimit: (groupKey: RetentionGroupKey) -> Int)`
(exact key/type naming left to implementation) rather than adding a second,
parallel "grouped" method. The purger already builds the full candidate
list and knows each candidate's program/org-unit/dataset; grouping is a
`groupBy { ... }` on that same list before the existing
`sortedByDescending { lastUpdated() }.drop(limit)` step, with the resolved
limit looked up per group instead of once.

**Alternative considered and rejected**: keep `purge(limit: Int)` unchanged
and resolve one effective global number by summing/flattening group limits
before calling it. Rejected — collapsing groups into a single number before
selection makes it impossible to protect one program's quota from being
consumed by another's excess (e.g. program A's 200 synced TEIs would count
against program B's separate limit of 50), which defeats the purpose of
`PER_PROGRAM` scope entirely.

**`OrphanFileResourceRetentionPurger` keeps `purge(limit: Int)` unchanged** —
no setting exists to group file resources by, so it is not migrated to the
group-aware form; `RetentionPurger` becomes a shared interface with two
purge signatures split by capability, or `OrphanFileResourceRetentionPurger`
stops implementing `RetentionPurger` and calls a single-group resolver
directly (implementation detail for tasks.md to settle without spec impact
either way).

### DataValue (dataset) limit — no org-unit split

Because `DataSetSetting` has no `LimitScope` field, `DataValueRetentionPurger`
groups only by datasetUid when a specific dataset setting exists (falling
back to `globalSettings().periodDSDBTrimming()`, falling back to today's
hardcoded default). There is no `ALL_ORG_UNITS`/`PER_ORG_UNIT` case to handle
for data values under this design — that would require a new field on
`DataSetSetting`, out of scope here (see proposal.md - Impact).

### Settings repositories are blocking, not suspend

`ProgramSettingsObjectRepository`/`DataSetSettingsObjectRepository` expose
`blockingGet(): ProgramSettings`/`DataSetSettings` (Java, blocking call —
also a `get(): Single<...>` RxJava2 variant, not used here). The new
resolver collaborator is a `suspend` function per this codebase's
conventions, so it wraps the blocking call with
`withContext(Dispatchers.IO)` rather than calling it directly from a
coroutine, and resolves `ProgramSettings`/`DataSetSettings` once per purge
run (see Risks below), not once per candidate or per group.

### Resolution reuses `aggregatedSyncState`, no new sync-state computation

This design does not compute or alter sync state; eligibility filtering
(already `aggregatedSyncState == SYNCED` plus relationship eligibility) runs
exactly as today, upstream of the new grouping/limit-resolution step.

## Risks / Trade-offs

- [Changing a shared interface (`RetentionPurger`) touches 3 implementations
  instead of being purely additive] → Scoped and justified explicitly per
  proposal.md - Impact; contained to the `retention/internal` package (not
  the ~30+ `ModuleWiper` blast radius the project's design rules warn
  about). No production caller exists yet for `SyncedDataRetentionPurger`
  (per the retention-purge spec), so there is no external behavior to
  break by changing the interface now versus later.
- [Groups with very few candidates could rebuild the same
  `ProgramSettings`/`DataSetSettings` lookup repeatedly per group] →
  Mitigation: resolve `ProgramSettings`/`DataSetSettings` once per purge run
  (existing repositories already return the fully materialized object with
  `globalSettings()`/`specificSettings()`, no additional query per group).
- [A record whose program/org unit was deleted or reassigned after the
  record was created has no matching specific setting] → Falls back to
  `globalSettings()`, then hardcoded default — same fallback as an
  unconfigured program, no special-casing needed.
- [Settings Web App does not yet expose trimming fields, so `PER_PROGRAM`/
  `PER_ORG_UNIT` scope cannot be configured in production today] → Not a
  code risk: the resolver still works correctly if these settings are
  API-set directly or added later; `GLOBAL`/default behavior is unaffected
  in the meantime. Documented as a Non-Goal, not blocking this change.
