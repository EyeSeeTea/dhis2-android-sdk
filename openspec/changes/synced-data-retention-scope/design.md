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

### `RetentionPurger` splits into a read port and a write port — selection moves to a domain service

**Problem with the first version of this decision**: an earlier draft had
`RetentionPurger.purge(resolveLimit: (groupKey: RetentionGroupKey) -> Int)`
— a callback the purger invokes per group it decides to build. This was
rejected during implementation of Group 3: it makes the purger (a
persistence adapter) responsible for deciding *whether* and *how* to group
candidates, which is a business rule, not a persistence concern. Concretely,
it broke the `GLOBAL` case — an adapter that always groups by program, even
when handed a callback that returns the same flat number for every group,
does not reduce to single-pool behavior (dropping N from *each* per-program
group is not equivalent to dropping N from the combined pool). The adapter
had no way to know, from an `Int`-returning callback alone, whether the
caller intended one pool or several.

**Decision**: `RetentionPurger` splits into two operations — a read (list
eligible candidates with their grouping attributes) and a write (delete a
given set of uids). Selection — grouping, sorting, and trimming to the
resolved limit — moves out of the purger entirely and into a new domain
service, `RetentionSelector`, that operates on plain data and knows nothing
about Room or any store:

```kotlin
internal interface RetentionPurger {
    suspend fun eligibleCandidates(): List<RetentionCandidate>
    suspend fun purge(uids: List<String>)
}

internal data class RetentionCandidate(
    val uid: String,
    val lastUpdated: Date,
    val programUids: List<String> = emptyList(),   // TEI: 0..n via enrollments; Event: exactly 1
    val organisationUnitUid: String? = null,
)

internal class RetentionSelector(
    private val programRetentionLimitResolver: ProgramRetentionLimitResolver,
    private val trackedEntityInstanceRetentionLimitResolver: TrackedEntityInstanceRetentionLimitResolver,
) {
    // Pure function of (candidates, resolved scope, limit lookup) -> uids to purge.
    // GLOBAL -> one group (RetentionGroupKey.Global), no splitting: exactly today's
    // single-pool behavior. PER_PROGRAM/PER_ORG_UNIT/etc. -> groupBy the matching
    // RetentionGroupKey per the table above, each group independently
    // sortedByDescending { lastUpdated }.drop(resolvedLimitForThatGroup).
    suspend fun select(
        candidates: List<RetentionCandidate>,
        scope: LimitScope,
        limitFor: suspend (RetentionGroupKey) -> Int,
    ): List<String>
}
```

The use case (`SyncedDataRetentionPurger`) is the only caller of both the
purger and the selector: for each entity type it calls
`purger.eligibleCandidates()`, resolves the run's `LimitScope` via the
Group 1-2 domain services, calls `retentionSelector.select(candidates,
scope, limitFor)` to get the uids to remove, then calls `purger.purge(uids)`.
This keeps the purger a pure adapter (query + cascade delete, both
genuinely Room-specific) and keeps all business logic (grouping policy,
most-restrictive-wins, sort-and-trim) in one reusable, non-Room-dependent
service — substitutable to any other persistence technology without
duplicating the selection logic.

**Alternative considered and rejected — a precomputed
`Map<RetentionGroupKey, Int>` passed as a plain data structure**: avoids a
callback, but the caller (use case) cannot know which programUids exist
among the candidates without first querying them — which is exactly the
`eligibleCandidates()` read the purger already needs to do. Building the
map would require either a redundant preliminary query from the use case
(duplicating store access outside the adapter) or exposing the store to the
domain layer. The two-port split (read candidates, then write uids) gives
the use case the grouping-relevant data it needs without either.

**Alternative considered and rejected**: keep `purge(limit: Int)` unchanged
and resolve one effective global number by summing/flattening group limits
before calling it. Rejected — collapsing groups into a single number before
selection makes it impossible to protect one program's quota from being
consumed by another's excess (e.g. program A's 200 synced TEIs would count
against program B's separate limit of 50), which defeats the purpose of
`PER_PROGRAM` scope entirely.

**`OrphanFileResourceRetentionPurger` keeps `purge(limit: Int)` unchanged
and does not implement `RetentionPurger`** — no setting exists to group
file resources by, so there is no candidate/selector split needed for it;
it stays a single concrete method called directly by
`SyncedDataRetentionPurger`, same as the current implementation (see Group
3 commit `ec11f4a351`, which already made this change ahead of this
design revision).

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
  instead of being purely additive, and splitting it into two ports (read +
  write) plus a new `RetentionSelector` service adds a class not in the
  original proposal] → Scoped and justified explicitly per proposal.md -
  Impact; contained to the `retention/internal` package (not the ~30+
  `ModuleWiper` blast radius the project's design rules warn about). No
  production caller exists yet for `SyncedDataRetentionPurger` (per the
  retention-purge spec), so there is no external behavior to break by
  changing the interface now versus later. The split was adopted after the
  single-callback form (`purge(resolveLimit: (key) -> Int)`) was found
  during Group 3 implementation to push a business decision (grouping
  policy) into the persistence adapter — see the "splits into a read port
  and a write port" decision above.
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
