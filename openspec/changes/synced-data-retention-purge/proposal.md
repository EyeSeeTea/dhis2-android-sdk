## Why

The SDK today only offers all-or-nothing local wipes (`wipeData()`, `wipeEverything()`),
with no way to keep already-synced data from accumulating indefinitely on the device.
A client (OCA) needs sensitive data removed from the device once it is no longer
needed locally — while data still pending sync (anywhere in its ancestor tree) must
never be touched. This capability is the reusable SDK mechanism that any downstream
fork can build a retention policy on top of.

## What Changes

- Add a new, narrow purge capability, implemented only by the domain modules that
  hold real user data (Event, Enrollment, TrackedEntity, DataValue, FileResource).
- A record (or, for hierarchical data, its whole ancestor tree) is eligible for
  purge only when fully synced: `aggregatedSyncState` (or `syncState` for leaf
  data with no children) is `SYNCED`. A single non-`SYNCED` descendant anywhere in
  the tree protects the entire tree from deletion.
- Eligible records are ordered oldest-first by `lastUpdated` and purged until a
  caller-supplied count limit is reached. Hierarchical data (TrackedEntity →
  Enrollment → Event → values) is purged as a full tree per eligible root; leaf
  data with no tree (`DataValue`, TEI-less events) is trimmed independently under
  its own limit.
- `limit = 0` is a supported scenario, not a special case: it degrades to "purge
  everything already synced."
- The purge runs transactionally: any failure partway through leaves the database
  unchanged from before the attempt.
- `FileResource` purge additionally deletes the row's physical file; a missing
  physical file does not abort the row deletion or raise an error to the caller.

## Capabilities

### New Capabilities
- `synced-data-retention-purge`: count-limited, tree-aware purge of already-synced
  local data (Event, Enrollment, TrackedEntity, DataValue, FileResource), ordered
  by recency, transactional, reusing the existing sync-state aggregation.

### Modified Capabilities
(none — this is additive; it does not change the behavior of `wipeData()` or
`wipeEverything()`)

## Impact

- **Affected domain modules**: event, enrollment, trackedentity, datavalue,
  fileresource. Each needs a new purge implementation; no other domain module is
  touched.
- **Not touched**: the shared `ModuleWiper` interface (`wipe/internal/`) is left
  as-is — a new, narrow, additive interface is introduced instead, implemented
  only by the 5 modules above, to avoid a change with ~30+ files of conflict
  surface across every existing (unrelated) wiper.
- **Reused, not reimplemented**: `DataStatePropagatorImpl`'s existing
  `aggregatedSyncState` computation is the sole source of truth for "is this tree
  safe to delete."
- **Explicitly out of scope for this change** (tracked as separate future specs):
  resolving the count limit from `ProgramSetting`/`DataSetSetting`/`LimitScope`
  (`synced-data-retention-scope`), `Relationship` coverage
  (`synced-data-relationships-purge`), and any app-side trigger or UI (a different
  repository, `dhis2-android-capture-app-extra`).
