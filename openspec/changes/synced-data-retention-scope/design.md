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

internal class RetentionSelector {
    // Pure function of (candidates, resolved scope, resolved per-group limits) -> uids to
    // purge. GLOBAL -> one group (RetentionGroupKey.Global), no splitting: exactly today's
    // single-pool behavior. PER_PROGRAM/PER_ORG_UNIT/etc. -> groupBy the matching
    // RetentionGroupKey per the table above, each group independently
    // sortedByDescending { lastUpdated }.drop(resolvedLimitForThatGroup).
    fun select(
        candidates: List<RetentionCandidate>,
        scope: LimitScope,
        limitByGroup: Map<RetentionGroupKey, Int>,
    ): List<String>
}
```

The use case (`SyncedDataRetentionPurger`) is the only caller of both the
purger and the selector: for each entity type it calls
`purger.eligibleCandidates()`, resolves the run's `LimitScope` via the
Group 1-2 domain services, resolves a limit for every distinct group key
present among those candidates (the candidates themselves are the only
source `limitByGroup` needs — no separate query), calls
`retentionSelector.select(candidates, scope, limitByGroup)` to get the uids
to remove, then calls `purger.purge(uids)`. Resolving limits stays the use
case's job and can itself be `suspend`; `RetentionSelector` only ever reads
an already-resolved map, so it never needs to be `suspend` itself.
This keeps the purger a pure adapter (query + cascade delete, both
genuinely Room-specific) and keeps all business logic (grouping policy,
most-restrictive-wins, sort-and-trim) in one reusable, non-Room-dependent
service — substitutable to any other persistence technology without
duplicating the selection logic.

**This is the target shape, built incrementally (see tasks.md Groups
3.3-7.2), not landed as one commit.** The first version of Group 3
implemented `RetentionCandidate`/`RetentionSelector`/`RetentionGroupKey`
in this full shape in a single step and found it violated YAGNI at the
commit level — fields like `programUids`/`organisationUnitUid` and
`RetentionGroupKey`'s non-`Program` variants had no test exercising them
yet. They were stripped back to `RetentionCandidate(uid, lastUpdated)` and
`RetentionSelector.select(candidates, limit: Int)` (single-pool only,
matching only the `GLOBAL` case), and tasks.md now rebuilds each grouping
dimension one red -> green step at a time, each adding only the field/key
variant its own test requires. The code snippets above and the grouping
key table describe where this ends up, not what Group 3's first commit
contains.

**Alternative considered and rejected — a `suspend (RetentionGroupKey) ->
Int` callback the selector invokes per group it builds**: this was the
first shape tried for `select`/`selectByProgram`. Rejected once the read
port existed: by the time the selector runs, the use case already has
`candidates` from `purger.eligibleCandidates()`, so it already knows every
distinct group key present — nothing about resolving limits needs to be
lazy or per-group-on-demand. The callback bought no candidate the selector
would otherwise have to discover itself; it only made the selector
`suspend` and coupled it to the shape of an async resolver function for no
reason. Precomputing `limitByGroup: Map<RetentionGroupKey, Int>` from the
already-available candidates before calling `select` is strictly simpler:
`RetentionSelector` stays synchronous, and "how a limit is resolved" stays
entirely the use case's concern.

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

### DataValue → dataset resolution is ambiguous — most-restrictive-wins, same as TEI

**Problem**: unlike `Event`, which carries its own `program` field directly,
`DataValue` has no `dataSet` field at all — neither in this SDK's local
table (`dataElement`/`period`/`organisationUnit`/`categoryOptionCombo`/
`attributeOptionCombo` only) nor in the DHIS2 server's own model
(`DataValue`'s composite key is exactly those same 5 fields; the `datavalue`
table has no `datasetid` column). A dataset is a grouping of
`DataElement`s (`DataSetElement`, many-to-many) — never a property of the
value itself. The server resolves `dataSet=X` in `dataValueSets` exports by
joining `datavalue` against the set of `dataElement`s assigned to X; it
never resolves the reverse direction (given a `DataValue`, which dataset is
it "from"), because every consumer that needs the relationship — export,
audit, deletion handlers — always starts from a known dataset and joins
outward to data elements, never the other way around. Server-side dataset
deletion (`DataSetDeletionHandler`) confirms this further: deleting a
`DataSet` does not delete or touch any `DataValue` at all.

This SDK's purge is the first consumer that needs the reverse direction: it
starts from a loose `DataValue` (`eligibleCandidates()`, filtered by
`aggregatedSyncState == SYNCED`) and must decide which dataset's limit
applies to it. Because a `DataElement` can be assigned to more than one
`DataSet`, a `DataValue`'s dataset membership can be genuinely ambiguous —
the same shape of problem as a TEI enrolled in more than one program (see
"Multi-program TEI limit resolution" above), just one hop further removed
(via `DataElement` instead of directly).

**Decision**: resolve a `DataValue`'s candidate datasets via
`DataSetDataElementLink` (`dataElement` → every `DataSet` it's assigned to,
reusing the join `DataValueByDataSetQueryHelper` already has for a related,
narrower question), resolve each candidate dataset's own limit, and use the
smallest resolved limit as the value's effective group limit — identical
rule to TEI's most-restrictive-wins, for the same reason: a value must not
be able to "hide" behind whichever of its datasets has the most generous
allowance. A `DataValue` whose `dataElement` belongs to only one dataset
(the common case) resolves trivially to that dataset's own limit.

**Alternative considered and rejected — reuse
`DataValueByDataSetQueryHelper.firstValidDataSetQuery`'s "first dataset,
alphabetically by uid" tie-break**: that resolution already exists in this
SDK, but for a different question (validating one specific `(dataElement,
period, organisationUnit, categoryOptionCombo, attributeOptionCombo)` tuple
against one already-known candidate dataset during data entry, not
resolving "which of N datasets applies" for retention purposes). Picking
"alphabetically first" for purge grouping would be arbitrary with respect
to the actual limits configured — it could just as easily land a value in
its most permissive dataset as its most restrictive one, silently defeating
a stricter admin-configured limit on a different dataset the same value
also belongs to. Rejected for the same reason the TEI section above rejects
"group by the most recently updated enrollment's program": it optimizes for
implementation reuse over a limit that means what an admin configured it to
mean.

### RetentionCandidate is a sealed class; dataset membership is a list, same as program

`RetentionCandidate` is a sealed class with one variant per grouping shape a
purger actually produces (`ByProgramAndOrgUnit` for `TrackedEntityInstance`
and `Event` — both have their own `organisationUnit` plus 0+ associated
programs; `ByDataset` for `DataValue`), instead of one data class carrying
every field as nullable/defaulted. A flat "bag of nullable fields" would let
`RetentionSelector.selectByDataset` be called with `Event`/TEI candidates
(and vice versa) and only fail at runtime on the field that's missing for
that shape; the sealed class makes that a compile error instead, and each
`selectByX` in `RetentionSelector` is typed to the one variant it groups by.

Following the same most-restrictive-wins reasoning as "Multi-program TEI
limit resolution" above, `ByDataset` carries `dataSetUids: List<String>`
(every dataset the value's `dataElement` is assigned to), not a single
`dataSetUid` — `selectByDataset` groups each candidate under
`dataSetUids.minBy { limitByDataset.getValue(it) }`, mirroring
`selectByProgram`'s `programUids.minBy { ... }` exactly. This removes the
need for a separate "combined resolver" that would apply most-restrictive-
wins on top of `DataSetRetentionLimitResolver` + `getDataSetsForDataElement`
— the selector already does that grouping once the candidate carries the
full list.

`ByProgramAndOrgUnit.organisationUnitUid` and `programUids`'s elements are
non-nullable: `Event.program()`/`organisationUnit()` and
`TrackedEntityInstance.organisationUnit()` are `@Nullable` at the Java/Room
annotation level only — local creation (`EventCreateProjection`,
`TrackedEntityInstanceCreateProjection`) requires them `@NonNull`, and for
`Event`, `EventHandler.deleteIfCondition` deletes any downloaded event with
a null `organisationUnit` before it can ever be marked `SYNCED`. A candidate
reaching `eligibleCandidates()` (already filtered to `SYNCED`) is guaranteed
to have both, so `RetentionSelector` needs no defensive `!!`/null-handling
for them.

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
- **Known limitation — `ProgramSetting`'s fields are independently nullable,
  so a program's `limit` and `scope` can be resolved from different
  sources within the same `ProgramRetentionLimitResolver.resolve()` call.**
  All fields on `ProgramSetting` (`teiDBTrimming`, `eventsDBTrimming`,
  `settingDBTrimming`, ...) are `@Nullable` and independent of each other —
  nothing in the model enforces that a specific setting which overrides
  `settingDBTrimming` also defines `teiDBTrimming`/`eventsDBTrimming`, or
  vice versa. `resolve()` falls back specific → global → default
  *per field*, not per record, so two inconsistent configurations produce
  a silently blended result instead of an error:
  1. A program's specific setting defines `settingDBTrimming = PER_ORG_UNIT`
     but leaves `teiDBTrimming` unset; the global setting has
     `teiDBTrimming = 500` with `settingDBTrimming = GLOBAL`. The resolved
     `ResolvedRetentionLimit` is `(limit = 500, scope = PER_ORG_UNIT)` — the
     limit comes from global, the scope from the specific setting, a
     combination no admin configured together.
  2. `MultiProgramRetentionLimitResolver.resolve()` (used for a TEI
     enrolled in more than one program) picks the whole
     `ResolvedRetentionLimit` — limit **and** scope — of whichever
     program resolves to the smallest limit (`minBy { it.limit }`),
     discarding the scope of every other program in play. If program A
     resolves to `(limit=50, scope=PER_ORG_UNIT)` and program B to
     `(limit=10, scope=PER_PROGRAM)`, the TEI (and, in
     `SyncedDataRetentionPurger.purgeByProgramAndOrgUnit`, every other
     candidate in that same purge run) is grouped under B's `PER_PROGRAM`
     scope — A's `PER_ORG_UNIT` configuration is silently ignored for that
     run, even though it is the setting the admin wrote for A's own
     records.

  → Not fixed in this change: there is no product requirement yet on how
  to reconcile a mismatched limit/scope pair, and today's Settings Web App
  cannot even set these trimming fields (see the risk above), so the
  inconsistent shapes described here are not reachable through the
  supported configuration UI — only via direct API writes. Revisit if/when
  the Web App exposes trimming settings and this becomes reachable by
  normal admin configuration.
