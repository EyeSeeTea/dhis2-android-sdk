# Customization Files

Technical inventory of every EyeSeeTea customization on top of Oslo upstream
(`dhis2/dhis2-android-sdk`): where each one is implemented and which commits
built it.

This file is for:
- listing where each confirmed customization is implemented
- tracking the technical status of each customization against the Oslo
  version this fork tracks
- listing the feature commits of each customization, so the inventory can be
  cross-checked (see the automerge verification rule in
  `upgrade/conflict-rules.md`)

This file is not for:
- functional intent or business justification — that lives in
  `openspec/specs/<capability>/spec.md`
- raw full diff dumps
- temporary upgrade progress — that lives in `upgrade/upgrade-<version>-notes.md`
- stable merge rules — those live in `upgrade/conflict-rules.md`

## Header

- Upstream: `dhis2/dhis2-android-sdk`
- Oslo version tracked: `1.14.2`
- Fork version: see `dhis2AndroidSdkVersion` in `gradle/libs.versions.toml`
- Base branch: `develop-eyeseetea`
- Generated on: `2026-10-21`

## Title rule

Every section heading in section 2 is the exact top-level `# <Title>` of the
matching `openspec/specs/<capability>/spec.md`, and the exact string used in
code as `// EyeSeeTea customization - <Title>`.
`scripts/check_upgrade_docs.py` checks that the three agree.

## 1. Fork identity surface

Files that differ from Oslo only to build and publish this fork. They are not
functional customizations and have no spec, but every upgrade must keep them.

- `gradle/libs.versions.toml` — `dhis2AndroidSdkVersion` (`<oslo>-eyeseetea-fork-<n>`)
- `jitpack.yml` — JitPack build
- `.github/workflows/eyeseetea-main.yml` — EyeSeeTea CI
- `.github/workflows/ci.yml` — CI adjusted for this fork (e.g. skips BrowserStack without credentials)
- `EyeSeeTea.md` — publication and local-development guide

`EyeSeeTea.md` describes `group = "com.github.EyeSeeTea"` in `build.gradle.kts`
and EyeSeeTea POM URLs in `buildSrc/src/main/kotlin/Props.kt`, but both files
are identical to Oslo: JitPack derives the coordinates from the repository, not
from those fields. Either align the guide or apply the change; until then
neither file is part of the fork surface.

## 2. Customization implementation points

### 2.1 Synced Data Retention Purge

Status: `active`

Spec: `openspec/specs/synced-data-retention-purge/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/DataValueRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/EventRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/OrphanFileResourceRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/TrackedEntityRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/ValueFileResourcePurger.kt`

Supporting files in the same workflow:
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/DataValueRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/EventRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/OrphanFileResourceRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/RetentionTestFixtures.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/TrackedEntityRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/ValueFileResourcePurgerIntegrationShould.kt`

Technical note:
- Lives entirely in the new `retention/` package (placement level 1), so it
  does not conflict with Oslo files. Origin: OCA requirement; published in
  `1.14.2-eyeseetea-fork-2`.

### 2.2 Synced Data Relationships Purge

Status: `active`

Spec: `openspec/specs/synced-data-relationships-purge/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RelationshipEligibilityChecker.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RelationshipRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/EventRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/TrackedEntityRetentionPurger.kt`

Supporting files in the same workflow:
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/EventRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/RelationshipEligibilityCheckerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/RelationshipRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/TrackedEntityRetentionPurgerIntegrationShould.kt`

Technical note:
- Same `retention/` package as 2.1. Depends on the Oslo relationship tables
  and their FK cascade: relationships are purged before the entity they point
  at, so the FK cascade cannot orphan them. Re-check that ordering if Oslo
  changes the relationship schema.

### 2.3 Synced Data Retention Scope

Status: `active`

Spec: `openspec/specs/synced-data-retention-scope/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/retention/RetentionModule.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/DataSetRetentionLimitResolver.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/ProgramRetentionLimitResolver.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionCandidate.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionDIModule.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionModuleImpl.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionSelector.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/DataValueRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/EventRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/OrphanFileResourceRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/TrackedEntityRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/D2.kt` — `retentionModule()` accessor (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/arch/d2/internal/D2DIComponent.kt` — wiring (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/arch/d2/internal/D2DIComponentFactory.kt` — registers `RetentionDIModule` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/dataset/internal/DataSetElementStore.kt` — `getDataSetsForDataElement` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/persistence/dataset/DataSetDataElementLinkStoreImpl.kt` — its implementation (Oslo file)
- `core/api/core.api` — generated; carries no customization comments

Supporting files in the same workflow:
- `core/src/androidTest/java/org/hisp/dhis/android/core/dataset/internal/DataSetElementStoreIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/DataValueRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/EventRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/RetentionTestFixtures.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurgerIntegrationShould.kt`
- `core/src/androidTest/java/org/hisp/dhis/android/core/retention/internal/TrackedEntityRetentionPurgerIntegrationShould.kt`
- `core/src/test/java/org/hisp/dhis/android/core/retention/internal/DataSetRetentionLimitResolverShould.kt`
- `core/src/test/java/org/hisp/dhis/android/core/retention/internal/ProgramRetentionLimitResolverShould.kt`
- `core/src/test/java/org/hisp/dhis/android/core/retention/internal/RetentionModuleImplShould.kt`
- `core/src/test/java/org/hisp/dhis/android/core/retention/internal/RetentionSelectorShould.kt`
- `core/src/test/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurgerShould.kt`

Technical note:
- The only retention customization that touches Oslo files: the public
  `D2.retentionModule()` entry point, its DI registration, and the dataset
  lookup that groups `DataValue` candidates by dataset. Those five Oslo files
  are the conflict surface of the whole retention feature. Published in
  `1.14.2-eyeseetea-fork-2`.
- Consumed by the app's `feature-oca/synced-data-retention-purge` branch.

### 2.4 Disabled Account Login Handling

Status: `active`

Spec: `openspec/specs/disabled-account-login/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/LogInCall.kt`
- `core/src/main/java/org/hisp/dhis/android/core/maintenance/D2ErrorCode.java`
- `core/api/core.api` — generated; must not gain a public `ACCOUNT_DISABLED` value

Supporting files in the same workflow:
- `core/src/test/java/org/hisp/dhis/android/core/user/internal/LogInCallUnitShould.kt`

Technical note:
- An inline edit in Oslo's `LogInCall.kt` (placement level 4), on top of the
  EyeSeeTea `/api/auth/login` flow, which is itself still undocumented (see
  section 3). Published in `1.14.2-eyeseetea-fork-3`.

## 3. Shared drift still differing

Differences against Oslo that are not yet backed by a spec. Every entry needs a
short reason. Confirmed customizations move to section 2 once they have a spec;
absorbed or obsolete ones are removed instead.

**This section is not exhaustive yet.** `develop-eyeseetea` carries 96
EyeSeeTea-only commits since 2020
(`git rev-list --no-merges develop-eyeseetea --not --remotes=upstream <oslo tags>`);
the onboarding inventory (see `README.md`, "Onboarding the existing
customizations") must classify each of them as active, absorbed by Oslo, or
removed.

Confirmed customizations without a spec yet:
- `core/src/main/java/org/hisp/dhis/android/core/configuration/internal/migration/Migration301.kt`,
  `core/src/main/java/org/hisp/dhis/android/core/configuration/internal/DatabaseEncryptionPasswordManager.kt`
  (+ `Migration301IntegrationShould.kt`, `DatabaseConfigurationMigrationIntegrationShould.kt`)
  — copies the encryption password of an encrypted account to the new key when
  its database file is renamed, so the renamed database can be opened. Needs a
  spec and a titled marker (current markers have no title).
- `core/src/main/assets/migrations/164.sql` — Oslo bug fix (`ANDROSDK-1871`)
  following the SQLite `ALTER TABLE` caveats. Needs an `EyeSeeTea fix` marker
  with ticket and version instead of the current free-text comment.

Candidates pending confirmation (found in the diff, intent not confirmed):
- `LogInCall.kt`, `UserNetworkHandlerImpl.kt`, `UserService.kt`,
  `LoginPayload.kt`, `LoginResponse.kt`, `D2ErrorCode.java` — login through
  `/api/auth/login` with an optional `twoFactorCode` (TOTP, email, SMS,
  `TWO_FACTOR_MANY_SEND_ATTEMPTS`), with a fallback to the old login for
  servers older than 2.41. Not present in Oslo `1.15.0`.
- `ConnectLogoutHandler.kt`, `AccountManager.kt`, `AccountManagerImpl.kt` —
  `changeServerUrl` for the current account, and automatic logout on a 401
  response or a redirect to `dhis-web-login`.
- `DatabaseConfigurationHelper.kt` — reuses an existing account with the same
  `serverUrl` + `username` + `encrypted` instead of creating a new one.
- `GetDimension.kt`, `FileResourceDownloadCall.kt`,
  `FileResourceDownloadParams.kt`, `D2Configuration.java` — file resource
  download sizing.

Not customizations (listed so they are not re-investigated):
- `core/src/main/java/org/hisp/dhis/android/core/parser/internal/expression/ParserUtils.kt`
  — wildcard imports expanded; no behavior change.

## 4. Feature commits

Original commits of each customization in section 2. Every code file one of
these commits touched must appear under the matching section 2 entry.

### 4.1 Synced Data Retention Purge

- `6a2a6647a8` — feat: purge oldest synced DataValues beyond a retention limit
- `42979c96e6` — test: purge every synced DataValue when the retention limit is zero
- `30fa8eb226` — test: keep every synced DataValue at or below the retention limit
- `d857b45769` — feat: make DataValue retention purge transactional
- `b1daaed55f` — refactor: rename test helper and failing store for clarity
- `d030ce14ab` — feat: purge fully synced TrackedEntityInstances and extract RetentionPurger
- `ac92369f7a` — test: keep TEI whose aggregatedSyncState is not synced
- `6ad31caf6f` — feat: purge enrollments and their notes when purging a tracked entity instance
- `3df1a8b474` — test: keep an enrollment of a non-eligible TEI even if the enrollment is synced
- `1e2d83297e` — feat: purge events, their data values and notes when purging a tracked entity instance
- `8b6e01f200` — test: extend partial-tree protection test to a real TEI-Enrollment-Event tree
- `4ab877e0d3` — feat: purge oldest synced TEI-less events beyond their own retention limit
- `1eefdd34a6` — feat: purge oldest synced FileResources beyond the retention limit
- `eb56996f1b` — refactor: move transactional wrapping out of individual purgers
- `1b0796a54c` — feat: add public entry point for cross-module synced-data retention purge
- `61be653ead` — test: cover trimming leaf data does not affect tracked entity data scenario
- `f53b92e1f2` — feat: add ValueFileResourcePurger for tree-eligible FileResource cascade
- `6e46bc844b` — refactor: read TrackedEntityDataValue rows before deleting them
- `480b3c0d78` — feat: wire ValueFileResourcePurger into DataValue/TrackedEntity/Event purgers
- `1074a20a6e` — refactor: rename FileResourceRetentionPurger to OrphanFileResourceRetentionPurger
- `4de1ef6b5f` — refactor: simplify ValueFileResourcePurger and use SQL subquery for orphan detection
- `bc36ecd752` — test: extract shared retention test fixtures into RetentionTestFixtures
- `edeb9c7b3d` — refactor: reuse TrackedEntityDataValueSamples, drop runBlocking in fixtures
- `5987c73056` — refactor: extract event purge to fix detekt NestedBlockDepth violation

### 4.2 Synced Data Relationships Purge

- `dca72b5ce7` — feat: add RelationshipRetentionPurger to purge relationships of a purged entity
- `0832a449fa` — feat: add RelationshipEligibilityChecker for cross-tree purge eligibility
- `6187278eb8` — feat: wire relationship eligibility and cascade into TrackedEntityRetentionPurger
- `1c1d1b6cc0` — feat: wire relationship eligibility and cascade into enrollment purge step
- `2fc4251ef1` — feat: wire relationship eligibility and cascade into EventRetentionPurger
- `319fb1dc28` — feat: wire relationship eligibility and cascade into cascaded event purge step
- `ebbd730ed0` — test: cover relationship purge directionality independence scenario
- `213efc9a6c` — fix: Purge a TEI's relationships before deleting it so the FK cascade does not orphan them
- `a2e521ea09` — fix: Purge an enrollment's relationships before deleting it so the FK cascade does not orphan them
- `e79b6996bf` — fix: Purge a TEI event's relationships before deleting it so the FK cascade does not orphan them
- `2a50f59e8f` — fix: Purge a TEI-less event's relationships before deleting it so the FK cascade does not orphan them

### 4.3 Synced Data Retention Scope

- `10aa296c3a` — feat: add program-scoped retention limit resolver
- `ec11f4a351` — feat: resolve multi-program TEI retention limit as most-restrictive-wins
- `cbc56586ca` — feat: split RetentionPurger into read/write ports with RetentionSelector domain service
- `02e20be615` — feat: trim retention candidates independently per program
- `6c5ccce4a7` — feat: group multi-program TEI candidates under their most restrictive program limit
- `6ef8380324` — test: cover eligibleCandidates and purge for multi-program TEIs
- `1661f4ea17` — feat: add selectByOrgUnit as a sibling of selectByProgram
- `a35899bbc6` — feat: populate program and org unit on event retention candidates
- `1e86c34608` — feat: resolve DataValue dataset membership as a list for most-restrictive-wins grouping
- `94a12d9e98` — feat: wire resolvers and grouped selection into SyncedDataRetentionPurger
- `2c0ad32407` — feat: implement PER_OU_AND_PROGRAM scope and verify ALL_ORG_UNITS equivalence
- `8d5bf2c40d` — test: close RetentionSelector coverage gap for limit=0 and at-or-below-limit cases
- `0101927a39` — perf: resolve program settings once per program instead of twice per purge
- `25a23d4c8f` — feat: expose SyncedDataRetentionPurger publicly via a new RetentionModule
- `621c988be3` — fix: register RetentionDIModule so Koin can resolve RetentionModule
- `fd55292d79` — fix: preserve the real failure message when retention purge fails unexpectedly
- `09f09b55dd` — fix: suppress detekt's generic-exception warning on the intentional catch
- `d860880d29` — fix: Group enrollment-less TEIs under the global limit in PER_PROGRAM retention purge
- `f0e2cd7377` — fix: Group enrollment-less TEIs per org unit under the global limit in PER_OU_AND_PROGRAM retention purge
- `5f2383c975` — fix: Fall back to the global retention limit and scope when no candidate has a program
- `929820b8b3` — fix: Fall back to the exception type when a retention purge failure has no message
- `1bd21a8341` — test: Cover that enrollment-less TEIs do not change the resolved retention scope

### 4.4 Disabled Account Login Handling

- `4380f4e3f8` — fix: handle disabled account status during login
- `98dfc187c8` — test: cover disabled account cleanup behavior
- `ca7bc60626` — fix: handle disabled account login without deleting local data
- `febe27964b` — docs: document disabled account login customization

## 5. Notes

- This inventory reflects the current branch state only.
- If a code comment and a spec title diverge, the spec title wins; fix the
  comment.
- Consumers: `EyeSeeTea/dhis2-android-capture-app` (`develop-eyeseetea` and the
  client branches) via `com.github.EyeSeeTea:dhis2-android-sdk`. How the app
  consumes a customization (worker scheduling, settings UI, per-fork gating)
  is documented in the app repository, not here.
