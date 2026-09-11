## Why

`synced-data-retention-purge` (already implemented, archived) purges already-synced
Event/Enrollment/TrackedEntity/DataValue/FileResource data, but explicitly excludes
`Relationship` — the client's original scope ("alcance D": tracker + aggregate +
files + relationships/notes, all filtered to already-synced) is not yet complete.
No `ModuleWiper` purges `Relationship` today; a TEI/Enrollment/Event purged by the
existing mechanism can leave a `Relationship`/`RelationshipItem` row pointing at a
uid that no longer exists locally, because the FK `ON DELETE CASCADE` declared in
the Room schema is never active in production (`PRAGMA foreign_keys` is always
`OFF` — verified while building the FileResource cascade in the prior change).

## What Changes

- Add relationship purge to the existing retention mechanism, following the same
  principle already applied to every other data type: purging must leave the local
  database as if the purged record had never been created or downloaded locally,
  without ever affecting the record's state on the server.
- A `Relationship` is eligible for purge only when **both** of its items
  (`RelationshipItem.from`/`to`) are themselves eligible — i.e. the root of each
  item's own tree (TrackedEntityInstance, Enrollment, or Event, per
  `RelationshipConstraintType`) has `aggregatedSyncState = SYNCED`. A relationship
  with one eligible side and one non-eligible side protects both sides from purge:
  neither the eligible TEI/Enrollment/Event nor its subtree is purged while the
  relationship still points at a non-eligible counterpart.
- When a TrackedEntityInstance, Enrollment, or Event is purged (by the existing
  per-module purgers), every `Relationship` where it is a `RelationshipItem` is
  purged together with it, in the same transaction — closing the gap where the
  current retention purge already deletes these root types without touching the
  `Relationship`/`RelationshipItem` rows that reference them.
- Applies uniformly regardless of `RelationshipType.bidirectional()` and
  regardless of which constrained type (`trackedEntityType`, `program`,
  `programStage`) each side is restricted to — a relationship's eligibility is
  never a special case of the item type at either end, only of that item's own
  tree eligibility.

## Capabilities

### New Capabilities
- `synced-data-relationships-purge`: purges `Relationship` rows whose linked
  `RelationshipItem`s are both fully synced (own tree `aggregatedSyncState =
  SYNCED`), and protects any tree that still has a relationship pointing at a
  non-eligible counterpart from being purged by the existing per-module purgers.

### Modified Capabilities
- `synced-data-retention-purge`: the "purge eligibility requires a fully synced
  record tree" requirement is extended — a TrackedEntityInstance, Enrollment, or
  Event's eligibility now additionally depends on every `Relationship` where it
  participates having a fully-synced counterpart on the other side. This is a
  requirement-level change (a record already eligible by today's rules can become
  ineligible once its relationships are considered), not just an added capability,
  so the existing spec needs a delta alongside the new one.

## Impact

- **Affected domain modules**: `relationship` (new `RelationshipRetentionPurger`
  or equivalent, following the `RetentionPurger` shape already established — not
  `ModuleWiper`, since this reuses the narrow interface introduced by the prior
  change specifically to avoid the ~30-wiper blast radius). `trackedentity`,
  `enrollment`, `event` purgers are affected too: each needs to (a) factor
  relationship eligibility into its own selection query, and (b) purge the
  relationships of a row it purges, in the same cascade.
- **Not touching**: the shared `ModuleWiper` interface — untouched, same
  reasoning as the prior change.
- **Reused, not reimplemented**: `DataStatePropagatorImpl`'s existing
  `aggregatedSyncState` computation already factors a `Relationship`'s own
  `syncState` into its two endpoints' aggregated state (confirmed by reading
  `refreshTrackedEntityInstanceAggregatedSyncState` et al.) — but it does **not**
  factor in the *other* endpoint's tree state. This change adds that missing
  cross-tree check; it does not duplicate or fight the existing propagator.
- **Explicitly out of scope for this change**: `synced-data-retention-scope`
  (resolving the retention count limit from `ProgramSetting`/`DataSetSetting` via
  `LimitScope`) — tracked as a separate, independent future spec.
