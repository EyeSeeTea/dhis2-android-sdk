## Context

See `proposal.md` for motivation. Relevant current-state facts:

- `Relationship` has its own `syncState` (no `aggregatedSyncState` — it is a leaf
  in isolation), and is linked to exactly two `RelationshipItem` rows (`from`/`to`,
  distinguished via `RelationshipItemStore.getForRelationshipUidAndConstraintType`).
  Each `RelationshipItem` points at exactly one of `TrackedEntityInstance`,
  `Enrollment`, or `Event` (`RelationshipItem.elementUid()`/`elementType()` already
  resolve which one, and to which uid — no need to re-derive this).
- `DataStatePropagatorImpl.refreshTrackedEntityInstanceAggregatedSyncState` (and
  its Enrollment/Event equivalents) already fold a `Relationship`'s own
  `syncState` into both endpoints' `aggregatedSyncState` via
  `getRelationshipsByItem(...)`. What it does **not** do is fold in the *other*
  endpoint's own `aggregatedSyncState` — a TEI whose only pending change is a
  relationship's counterpart TEI is still computed as fully `SYNCED` today. This
  gap is what this change closes, without touching `DataStatePropagatorImpl`
  itself (see Decisions — computed at purge-selection time, not by changing the
  aggregation SDK-wide).
- `RelationshipItemStore.getByEntityUid(entityUid)` already returns every
  `RelationshipItem` referencing a given TEI/Enrollment/Event uid — the exact
  lookup needed to find "which relationships does this row about to be purged
  participate in."
- The Room schema declares `ON DELETE CASCADE` from `RelationshipItem` to
  `TrackedEntityInstance`/`Enrollment`/`Event` (and from `Relationship` to
  `RelationshipType`), but `PRAGMA foreign_keys` is never set to `ON` anywhere in
  production code (`RoomDatabaseManager.createInMemoryDatabase()` explicitly sets
  it `OFF`, and no call site ever passes `true` to
  `DatabaseAdapter.setForeignKeyConstraintsEnabled`) — confirmed while building
  the FileResource cascade in the prior change. This means today, purging a TEI
  via the existing `TrackedEntityRetentionPurger` already leaves any
  `Relationship`/`RelationshipItem` referencing it as an orphan row — a
  pre-existing gap this change also closes, not a new risk it introduces.
- No `ModuleWiper` or `RetentionPurger` implementation exists yet for the
  `relationship` module.

## Goals / Non-Goals

**Goals:**
- Make a `Relationship` and its two `RelationshipItem` rows purge-eligible only
  when both endpoints' own trees are fully synced, and purge them together with
  whichever endpoint the existing purgers remove.
- Make a TrackedEntityInstance/Enrollment/Event with a relationship pointing at a
  non-eligible counterpart ineligible for purge itself, symmetric to how a
  non-`SYNCED` child already protects its ancestor chain.
- Keep the change additive to the `retention/internal` package introduced by the
  prior change: a new `RelationshipRetentionPurger`-shaped collaborator, not a
  change to the shared `ModuleWiper` interface.

**Non-Goals:**
- Changing `DataStatePropagatorImpl`'s `aggregatedSyncState` computation itself.
  That primitive stays exactly as-is; this change adds a purge-time-only check on
  top of it (see Decisions) rather than making `aggregatedSyncState` itself
  cross-tree-aware, which would be a much larger, SDK-wide behavior change
  affecting every consumer of that state, not just retention purge.
- Resolving the retention count limit from `ProgramSetting`/`DataSetSetting`
  (`synced-data-retention-scope` — separate, independent future spec).
- Activating `PRAGMA foreign_keys` / relying on Room cascade deletes as the purge
  mechanism. The orphan-row gap this change closes is fixed by explicit
  application-level cascade (matching every other purger already built), not by
  turning FK enforcement on — enabling FKs SDK-wide is an orthogonal, much
  higher-risk change outside this scope.

## Decisions

### Cross-tree eligibility is computed at purge-selection time, not baked into `aggregatedSyncState`
For each candidate row (TEI/Enrollment/Event) a purger is about to select, the
purger additionally checks: for every `RelationshipItem` returned by
`RelationshipItemStore.getByEntityUid(candidateUid)`, resolve the *other* item of
the same `Relationship` (`getForRelationshipUid(relationship.uid())`, filter out
the item matching `candidateUid`) and read that other item's own
`elementType()`/`elementUid()` → its own `aggregatedSyncState`. If any such
counterpart is not `SYNCED`, the candidate is excluded from this purge run.

Alternative considered: extend `DataStatePropagatorImpl` so a `Relationship`'s
`aggregatedSyncState` propagation looks at the counterpart's aggregated state too,
making every consumer of `aggregatedSyncState` (not just retention purge)
automatically cross-tree-aware. Rejected: `aggregatedSyncState` is read by sync
upload ordering and conflict resolution elsewhere in the SDK, not just by
retention purge; changing what it means for every existing caller is a much
larger, riskier change than this feature needs, and the two `Relationship` rows
already have no shared write path that would keep such a bidirectional
propagation cheap and race-free at write time (unlike a real tree, siblings don't
share an ancestor to converge state through). Computing it at purge-selection
time — a read-only, on-demand check confined to the retention purge code path —
achieves the same purge-safety guarantee without that blast radius.

### `RelationshipRetentionPurger`: purges relationships of an already-purged row, called inline — same shape as `ValueFileResourcePurger`
Mirrors the pattern already established for the FileResource cascade: a plain
injected collaborator (not a `RetentionPurger`, no limit of its own), with one
method, e.g. `purgeForEntity(entityUid: String)`, called inline by
`TrackedEntityRetentionPurger`, the enrollment-cascade step, and
`EventRetentionPurger`/the event-cascade step, right after (or as part of) each
row's own delete — not as a separate pass at the end. This is deliberately the
same shape as `ValueFileResourcePurger` for the same reason: the correct trigger
for "purge this relationship" is "one of its two endpoints was just purged", which
only the purger deleting that endpoint knows at the exact moment it happens;
there is no shared delete path across TEI/Enrollment/Event to hang a generic
cascade off of.

Homogeneous alternative (a `RelationshipModuleWiper`-style top-level pass that
scans all `Relationship` rows once, independent of the three purgers) was
rejected for the same reason `OrphanFileResourceRetentionPurger`'s shape was kept
separate from `ValueFileResourcePurger`: relationships purged *because* their
endpoint was purged are not the same case as relationships with no purgeable
endpoint at all — and unlike FileResource, `Relationship` has no genuine
"orphan" case here: every `Relationship` always has exactly two items pointing at
existing rows (enforced by the schema's `NOT NULL` `RelationshipItemDB` columns
appropriate to `relationshipItemType`), so there is nothing analogous to
`OrphanFileResourceRetentionPurger` to build — a `Relationship` is purged if and
only if the endpoint-eligibility check above passes for both sides, which is
exactly the condition under which one of the three existing purgers is about to
delete one of those sides anyway.

### Selection-query change in the three existing purgers, not a new standalone eligibility pass
`TrackedEntityRetentionPurger`, the enrollment step, and `EventRetentionPurger`
each add the cross-tree relationship check (see above) as an additional filter on
top of their existing `aggregatedSyncState = SYNCED` selection, before computing
`toPurge = eligible.drop(limit)`. This keeps the "eligible, ordered, over the
limit" selection shape from the prior change intact — relationships only narrow
the already-eligible set, they do not introduce a second selection mechanism.

### `RelationshipRetentionPurger` needs read access to `RelationshipStore`, `RelationshipItemStore`, and the three root stores (or their `aggregatedSyncState` lookups)
To resolve "the other endpoint's own tree state" it needs to read
`TrackedEntityInstance`/`Enrollment`/`Event` `aggregatedSyncState` by uid
regardless of which of the three the counterpart turns out to be — the same
three stores the composed entry point (`SyncedDataRetentionPurger`) already
depends on transitively via the existing purgers, so this does not introduce a
new store dependency to the module, only a new consumer of already-present ones.

## Risks / Trade-offs

- **[Risk]** The cross-tree check reads `RelationshipItemStore`/`RelationshipStore`
  once per candidate row per purge run (N extra reads for N candidates), on top
  of the existing per-row cascade reads already present in
  `TrackedEntityRetentionPurger`. → **Mitigation**: not addressed by this design;
  flagged as a known scaling consideration consistent with the prior change's
  accepted N+1-shaped reads in the same purgers (batching was explicitly
  deferred there as low-priority given realistic on-device row counts — the same
  reasoning applies here, same order of magnitude of extra reads).
- **[Risk]** A relationship whose counterpart item points at a uid that no longer
  exists at all (already-orphaned by some other path, e.g. a manual wipe or a
  pre-this-change purge run) — resolving its `aggregatedSyncState` finds nothing.
  → **Mitigation**: treat a missing counterpart row as vacuously eligible (there
  is nothing left to protect), so the relationship and any remaining item are
  purged rather than permanently blocking the candidate from ever being purged
  again. This also self-heals any orphans created before this change existed.
- **[Trade-off]** Cross-tree eligibility depends on reading the counterpart's
  `aggregatedSyncState` fresh at purge time rather than a value cached/propagated
  onto the `Relationship` row itself. This is accepted deliberately (see
  Decisions — rejected extending `DataStatePropagatorImpl`) in favor of not
  widening what `aggregatedSyncState` means for every other SDK consumer.
