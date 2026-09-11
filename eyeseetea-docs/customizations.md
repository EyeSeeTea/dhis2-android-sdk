# EyeSeeTea customizations

Differences of this fork compared to Oslo upstream (`dhis2/dhis2-android-sdk`).

See `README.md` for the document model (one fork, no per-client layer) and
`upgrade/conflict-rules.md` for how to resolve conflicts and keep this file
in sync during an upgrade.

**Scope note**: this file currently reflects two comparisons that are not
the same and must not be confused:

- `origin/develop-eyeseetea` vs `upstream/develop` (merge-base `c4c2542d78`)
  — the stable EyeSeeTea baseline vs. Oslo. Sections 1.1 and 2.1 come from
  this comparison.
- `HEAD` (branch `feature-oca/synced-data-retention-purge`, ~65 feature
  commits) vs `origin/develop-eyeseetea` — work not yet merged into the
  baseline. Section 1.2 comes from this comparison. Once this branch merges
  into `develop-eyeseetea`, re-derive section 1.2 against the new
  `develop-eyeseetea` head to confirm nothing was dropped, per
  `upgrade/conflict-rules.md`'s automerge verification rule.

## 1. Functional customizations

### 1.1 Encryption key preserved when a renamed database is opened

Status: `active`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/configuration/internal/migration/Migration301.kt`
- `core/src/main/java/org/hisp/dhis/android/core/configuration/internal/DatabaseEncryptionPasswordManager.kt`

Supporting files in the same workflow:
- `core/src/androidTest/.../Migration301IntegrationShould.kt`
- `core/src/androidTest/.../DatabaseConfigurationMigrationIntegrationShould.kt`

What it does:
When migrating an encrypted account whose database file gets renamed
(hash-suffix migration), the encryption password stored under the old
database name is copied to the new name in the secure store before the
renamed database is opened. Without this, opening the renamed database
throws `SQLiteNotADatabaseException` because the password lookup key no
longer matches the file name.

### 1.2 Synced-data retention purge (count-limit-based local cleanup)

Status: `active` — merged into `feature-oca/synced-data-retention-purge`,
**not yet merged into `develop-eyeseetea`**. Do not treat as landed until
that merge happens; re-verify this section against `develop-eyeseetea`
afterwards.

Client: OCA (client requirement — device-level cleanup of already-synced
sensitive data once it is no longer needed locally).

This is one capability built across three chained OpenSpec changes. All
three add a public/internal purge mechanism that deletes already-synced
(`aggregatedSyncState == SYNCED`) records once their count exceeds a
configurable limit — never partially-synced data, never data still pending
upload.

#### 1.2.1 Base purge mechanism (`synced-data-retention-purge`, archived)

Introduces the whole `retention/` package: eligibility (only fully synced,
tree-safe-to-delete records — reuses the existing `aggregatedSyncState`
computation, no new sync-state logic), cascade delete (TEI → Enrollment →
Event → TrackedEntityDataValue/Note, keeping partial trees intact), and
`FileResource`/`OrphanFileResourceRetentionPurger` cleanup.

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/`
  (`TrackedEntityRetentionPurger.kt`, `EventRetentionPurger.kt`,
  `DataValueRetentionPurger.kt`, `OrphanFileResourceRetentionPurger.kt`,
  `ValueFileResourcePurger.kt`, `RetentionPurger.kt`,
  `SyncedDataRetentionPurger.kt`)

Spec: `openspec/changes/archive/2026-09-01-synced-data-retention-purge/`

#### 1.2.2 Relationship coverage (`synced-data-relationships-purge`, archived)

Extends eligibility and cascade to `Relationship` rows: a purged
TEI/Enrollment/Event/DataValue also purges relationships pointing at it, and
an entity with a relationship to a not-yet-eligible entity is protected from
purge (cross-tree eligibility), independent of relationship direction.

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RelationshipEligibilityChecker.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RelationshipRetentionPurger.kt`

Spec: `openspec/changes/archive/2026-09-01-synced-data-relationships-purge/`

#### 1.2.3 Settings-driven scope (`synced-data-retention-scope`, in progress on this branch)

Resolves the retention count limit from `ProgramSetting.teiDBTrimming` /
`.eventsDBTrimming` / `.settingDBTrimming` (`LimitScope`) and
`DataSetSetting.periodDSDBTrimming` — already-synced settings the Settings
Web App model exposes but the SDK didn't read before this change — instead
of one hardcoded number for the whole device. Splits eligible records into
independent pools by program, org unit, or both (`GLOBAL` / `PER_PROGRAM` /
`PER_ORG_UNIT` / `ALL_ORG_UNITS` / `PER_OU_AND_PROGRAM`) and enforces each
pool's own resolved limit — one program's excess never consumes another
program's quota. Also adds the dataset-resolution helper in section 2.1,
which this scope resolution needs for `DataValue` grouping.

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/ProgramRetentionLimitResolver.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/DataSetRetentionLimitResolver.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionCandidate.kt`
  (sealed class: `ByProgramAndOrgUnit`, `ByDataset`)
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionSelector.kt`
  (`select`, `selectByProgram`, `selectByOrgUnit`, `selectByOrgUnitAndProgram`,
  `selectByDataset`)
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurger.kt`
  (orchestrates scope resolution + selection per entity type)
- `core/src/main/java/org/hisp/dhis/android/core/retention/RetentionModule.kt`
  + `retention/internal/RetentionModuleImpl.kt` — public entry point,
  `D2.retentionModule().purge()`, following the existing `<Domain>Module`
  pattern (`WipeModule`, `SettingModule`)
- `core/src/main/java/org/hisp/dhis/android/core/arch/d2/internal/D2DIComponent.kt`,
  `core/src/main/java/org/hisp/dhis/android/core/D2.kt` — wiring

Spec: `openspec/changes/synced-data-retention-scope/` (not archived yet —
`tasks.md` groups 1-9 complete, see that file's own progress notes).

**Out of scope, tracked in the app repository, not here**: consuming
`D2.retentionModule().purge()` from `dhis2-android-capture-app-extra`
(bumping the `dhis2sdk` dependency version, rewiring
`AndroidSyncRepository.purgeSyncedData()`, which currently calls a
non-existent `d2.wipeModule().wipeSyncedData()`, and any settings UI).

## 2. Oslo bug fixes

### 2.1 `ALTER TABLE` migration script fix

Status: `active`

Main implementation points:
- `core/src/main/assets/migrations/164.sql`

What it does:
Fixes a bug in a migration script that renamed columns to `displayXXX` for
`Program`/`ProgramStage` (Oslo `ANDROSDK-1871`), following the SQLite
documentation caveats for `ALTER TABLE` (https://sqlite.org/lang_altertable.html#caution).
Not tied to a business feature — a correctness fix to a migration step.

## 3. Candidates pending confirmation

These were found by comparing `origin/develop-eyeseetea` against
`upstream/develop` (`git diff upstream/develop...origin/develop-eyeseetea`,
merge-base `c4c2542d78`) but are **not yet verified as intentional,
still-needed customizations** — they have no `// EyeSeeTea customization`
marker, and their business justification has not been confirmed against a
spec, ticket, or client request. Do not treat this section as a stable
inventory; move each entry to section 1 (with a marker added in code) once
confirmed, or drop it if it turns out to be obsolete/absorbed.

- `core/src/main/java/org/hisp/dhis/android/core/user/internal/LogInCall.kt`
  and related login/2FA files (`UserNetworkHandlerImpl.kt`, `UserService.kt`,
  `D2ErrorCode.java`) — a `twoFactorCode` parameter appears to have been
  added to the login flow. The app-side doc
  (`dhis2-android-capture-app-extra/eyeseetea-docs/customizations/eyeseetea/customizations-eyeseetea.md`,
  section 3, "2FA and authentication compatibility") already documents the
  app consuming *some* SDK login overload with a 2FA parameter — this SDK
  side of that contract has never been documented here. Needs confirmation:
  is this an EyeSeeTea-added capability, or does upstream Oslo already have
  2FA support and this is just how the fork wires into it?
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/ConnectLogoutHandler.kt`
  (new file) and `AccountManagerImpl.kt` (`changeServerUrl` method) —
  appears to let the account switch server URL without losing the session.
  No existing documentation found on either side of the fork for this.
- `core/src/main/java/org/hisp/dhis/android/core/configuration/internal/DatabaseConfigurationHelper.kt` —
  appears to reuse an existing account instead of always creating a new one
  when one already exists with the same `serverUrl`+`username`+`encrypted`.
  Needs confirmation of intent.
- `core/src/main/java/org/hisp/dhis/android/core/fileresource/internal/GetDimension.kt` —
  new small helper file, no marker, purpose not yet confirmed against a
  spec.

Not customizations (confirmed drift/lint only, listed here so they are not
re-investigated on the next pass):
- `core/src/main/java/org/hisp/dhis/android/core/arch/api/internal/ParserUtils.kt` —
  wildcard imports expanded to explicit imports; no behavior change.

Also not yet classified (found while comparing `HEAD` against
`origin/develop-eyeseetea` for section 1.2, but unrelated to the retention
feature — this is upstream Oslo work, not an EyeSeeTea customization, but
listed here as a reminder to confirm before this branch merges):
- `CookieAuthenticatorHelper.kt`, `ParentAuthenticatorPlugin.kt`,
  `PasswordAndCookieAuthenticator.kt` — corresponds to Oslo commits
  `[ANDROSDK-2344] store cookies by host to avoid mixing sessions across
  servers`. Confirm this lands via the normal Oslo-upstream merge path into
  `develop-eyeseetea`, not as an EyeSeeTea-specific change, before this
  branch merges.

**This section is not exhaustive.** The `origin/develop-eyeseetea` vs
`upstream/develop` comparison touches 125 files across 171 commits; only
the ones above were reviewed in enough depth to report here. Treat any file
not listed in section 1, 2, or 3 as unreviewed, not as "confirmed no
customization."
