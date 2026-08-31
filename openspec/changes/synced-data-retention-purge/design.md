## Context

See `proposal.md` — Why, for the motivation. Relevant current-state constraints:

- The existing wipe machinery (`wipe/internal/ModuleWiper`, `TableWiper`,
  `WipeModuleImpl`) is all-or-nothing: `TableWiper.wipeTable(s)` issues an
  unconditional `DELETE FROM <table>` with no `WHERE`, no ordering, no limit.
  `ModuleWiper` is a plain `internal interface` with `wipeMetadata()` /
  `wipeData()` and no default implementations — every one of the ~30 existing
  wipers implements both methods directly.
- `DatabaseAdapter` already exposes what this capability needs without any new
  low-level primitive: `delete(tableName, whereClause, whereArgs)` for
  filtered deletes, and `rawQuery`/`rawQueryWithTypedValues` for arbitrary
  `SELECT ... ORDER BY ... LIMIT` reads (used to select purge candidates before
  deleting them).
- `DataStatePropagatorImpl` (`common/internal/`) already computes and persists
  `aggregatedSyncState` on every write; it is covered by
  `DataStatePropagatorIntegrationShould.kt`. This design treats it as a given —
  it reads the column, it does not recompute the aggregation.
- The 5 relevant tables (Event, Enrollment, TrackedEntityInstance, DataValue,
  FileResource) all carry a `lastUpdated` column already, so "oldest first"
  ordering needs no new column or migration.
- `FileResource` rows carry their own `path` column pointing at the on-device
  file, so a row-by-row purge can resolve which physical file to delete without
  a directory scan (unlike the existing full wipe, which deletes the whole
  file-resource directory).

## Goals / Non-Goals

**Goals:**
- Add a purge mechanism usable by any caller (SDK internal or, later, the
  consuming app) that is additive to the SDK's public surface.
- Keep the existing wipe machinery (`wipeData()`, `wipeEverything()`) completely
  unchanged in behavior and code.
- Make the new mechanism's building blocks (candidate selection, cascade,
  transactional guarantee) reusable by the follow-up specs
  (`synced-data-retention-scope`, `synced-data-relationships-purge`) without
  rework — see the spec-splitting checks already applied when scoping this
  proposal.

**Non-Goals:**
- Resolving the retention count limit from `ProgramSetting` / `DataSetSetting`
  / `LimitScope` — this design accepts the limit as a plain input parameter per
  data type; wiring it to settings is `synced-data-retention-scope`.
- `Relationship` purge.
- Any scheduling, triggering, or UI — those live in the consuming app repo.

## Decisions

### A new, narrow interface — do not touch `ModuleWiper`
`ModuleWiper` is implemented by ~30 modules that have nothing to do with
retention (metadata-only modules). Adding a method there — even with a default
no-op — means every one of those files is touched or at minimum re-evaluated,
which is the highest-conflict-surface option this fork explicitly avoids for
customization code, and would be true here even though this is fork-owned code
in a fork-owned interface, simply because of blast radius on an unrelated
interface used everywhere.

Alternative considered: add `wipeSyncedData()` to `ModuleWiper` with a default
no-op (this is what the earlier internal spike did). Rejected: unnecessary
33-file blast radius for a capability only 5 modules need; a new interface
implemented only by those 5 modules achieves the same registration pattern
(a `List<T>` resolved by Koin, same as `moduleWipers` in `WipeModuleImpl`)
without touching the other 28.

Decision: introduce a new internal interface (name TBD at implementation time,
e.g. `RetentionPurger`) with a single suspend method that a module implements
only if it holds purgeable user data. The 5 modules (event, enrollment,
trackedentity, datavalue, fileresource) implement it; no other module is
touched.

Sequencing: extract this interface only once a second implementation exists
(the TrackedEntityInstance purger, task 3.2) — not from the first one
(`DataValueRetentionPurger`, task 1.2) alone. `DataValueRetentionPurger` is a
leaf with no cascade, so `purge(limit: Int): Unit` may not be the right shape
for a tree root that needs to hand its selected/purged IDs down to its
children's purgers (Enrollment, Event) for cascade. Fixing the interface with
one data point risks designing it around the wrong case and having to break it
once the tree-aware shape is known — the exact throwaway-layering failure mode
this change's specs were already re-split to avoid (see the spec-splitting
discussion this proposal's scoping went through). `DataValueRetentionPurger`
stays a standalone class, no interface, until task 3.2 lands.

### Candidate selection: read-then-delete, not delete-with-subquery
Selecting "the oldest N eligible rows beyond the limit" needs an ordered read
before a delete. Two shapes were considered:

1. **Single SQL delete with an `ORDER BY ... LIMIT` subquery** (closest to the
   existing spike's `wipeChildTableWhereParentSynced` pattern, adapted to add
   ordering/limit). Rejected as the primary mechanism: SQLite's `DELETE`
   support for `ORDER BY`/`LIMIT` inside a correlated subquery is fragile
   across the SQLite versions Android ships, and it would make the "which rows
   were selected" step invisible to the caller — which matters for cascade
   (the same selected root IDs drive the child deletes) and for the future
   scope spec (which will need to reuse the exact same selection logic with a
   different limit source).
2. **Two-step: `SELECT` eligible root IDs ordered by `lastUpdated` with a
   count/offset boundary, then `DELETE ... WHERE id IN (:selected)`** (and, for
   hierarchical data, the same selected root IDs drive `DELETE` on each child
   table via a foreign key `IN` filter). Chosen: keeps selection and deletion
   as separate, independently testable steps: the read is what "eligible,
   ordered, over the limit" scenarios are tested against; the write is what
   "transactional, no partial state" scenarios are tested against.

### Cascade: root-first selection, children resolved by parent ID set, not a second aggregatedSyncState check
Once a set of tree roots (TEI) is selected as eligible and over the limit,
their children (enrollments, events, values) are deleted by following the
parent-child ID relationship — not by re-checking each child's own sync state.
This is safe specifically because eligibility was already established via
`aggregatedSyncState` on the root: a `SYNCED` root aggregate already guarantees
every descendant is `SYNCED` (see `DataStatePropagatorImpl`), so re-checking
would be redundant work, not an extra safety net.

This purger's own tests intentionally do not re-verify that guarantee — doing
so would mean fabricating a TEI/Event state combination
(`aggregatedSyncState = SYNCED` on the TEI with a non-`SYNCED` Event beneath
it) that the real system never produces, since `DataStatePropagatorImpl`
always keeps them consistent. That guarantee is already covered by
`core/src/androidTest/.../common/internal/DataStatePropagatorIntegrationShould.kt`
(e.g. `set_parent_state_to_update_if_has_synced_state`,
`do_not_set_parent_state_to_update_if_has_error_state`) — this purger's tests
build on top of it rather than duplicating it.

### Leaf data (DataValue, TEI-less events): independent selection, no cascade
`DataValue` has no children; TEI-less events are their own root. Both use the
same "eligible, ordered by `lastUpdated`, over the limit" read, but with a
single-table delete instead of a cascade — a straightforward reuse of the same
selection building block, with no cascade step attached.

### FileResource: row-by-row, not directory-based
Unlike the existing `wipeData()` (deletes the entire file-resource directory
recursively), purge must be selective — most files stay, only the excess
already-synced ones go. This means iterating the selected `FileResource` rows,
resolving `path` per row, and attempting file deletion per row, tolerating a
missing file (`runCatching` or equivalent) without aborting the row's DB
deletion — a missing physical file is a legitimate "already gone" state, not a
purge failure.

### Transactionality: reuse `d2CallExecutor.executeD2CallTransactionally`, same as `WipeModuleImpl` — at the composed entry point only
No new transaction mechanism. But — revised after building the first 4
per-module purgers — the transaction wrapping belongs **only on the composed
public entry point** (task 8.2), not inside each individual
`XxxRetentionPurger`. Verified against the SDK's actual precedent: no
`ModuleWiper` implementation calls `executeD2CallTransactionally` itself;
only `WipeModuleImpl` does, wrapping the `forEach` over every wiper from the
outside. There is no precedent anywhere in this codebase for nesting
`executeD2CallTransactionally` calls, and nothing confirms Room's
`immediateTransaction` would even merge nested calls into one atomic unit
here — so per-purger transactions were removed (each purger's `purge()` no
longer takes a `D2CallExecutorInterface` or wraps itself) and the single
`executeD2CallTransactionally` call now lives in the composed entry point
that calls every purger's `purge()` in sequence. This mirrors
`WipeModuleImpl` exactly: one caller, one transaction, several transaction-free
callees.

## Risks / Trade-offs

- **[Risk]** Two-step read-then-delete is not atomic at the SQL level between
  the `SELECT` and the `DELETE` — a concurrent write between the two could
  change what "eligible" means mid-operation.
  → **Mitigation**: the whole read+delete sequence runs inside the same
  `executeD2CallTransactionally` block already used for the existing wipe
  operations, so it is atomic at the transaction level regardless of the
  two-step shape at the code level.
- **[Risk]** Selecting large ID sets for the `IN (...)` clause (e.g. thousands
  of TEIs) could hit SQLite's parameter/expression limits.
  → **Mitigation**: not addressed by this design; flagged here as a known
  scaling limitation to revisit if it surfaces in practice (batch the `IN`
  clause), since neither the proposal nor the spec claims a specific device
  scale target.
- **[Trade-off]** Root-first cascade (not re-verifying each child's own state)
  is faster and simpler but depends entirely on `aggregatedSyncState` being
  correct. This is an accepted dependency — re-deriving it manually was
  explicitly rejected in favor of the existing, tested primitive (see
  Context and `config.yaml` rules).
