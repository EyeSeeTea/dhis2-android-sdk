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
- Oslo version tracked: `1.15.0`
- Fork version: see `dhis2AndroidSdkVersion` in `gradle/libs.versions.toml`
- Base branch: `develop-eyeseetea`
- Generated on: `2026-10-21` (updated for `1.15.0` on `2026-10-22`)

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
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionDIModule.kt` — includes `ArchDIModule`, `PersistenceDIModule` and `SettingsDIModule` so Koin compile safety (Oslo ANDROSDK-2241, `1.15.0`) can validate it on its own
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionModuleImpl.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionSelector.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/DataValueRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/EventRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/OrphanFileResourceRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/RetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/SyncedDataRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/retention/internal/TrackedEntityRetentionPurger.kt`
- `core/src/main/java/org/hisp/dhis/android/core/D2.kt` — `retentionModule()` accessor, marked (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/arch/d2/internal/D2DIComponent.kt` — `retentionModule` constructor parameter, marked (Oslo file)
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
- `core/src/main/java/org/hisp/dhis/android/core/maintenance/D2ErrorCode.kt` — uses Oslo's `USER_ACCOUNT_DISABLED`; adds no code
- `core/api/core.api` — generated; must not gain a public `ACCOUNT_DISABLED` value

Supporting files in the same workflow:
- `core/src/test/java/org/hisp/dhis/android/core/user/internal/LogInCallUnitShould.kt`

Technical note:
- An inline edit in Oslo's `LogInCall.kt` (placement level 4), on top of the
  EyeSeeTea `/api/auth/login` flow (2.5, Two-Factor Authentication Login).
  Published in `1.14.2-eyeseetea-fork-3`.

### 2.5 Two-Factor Authentication Login

Status: `active`

Spec: `openspec/specs/two-factor-login/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/LogInCall.kt` — `twoFactorCode` parameter, `loginInDhis2AndGetUser`, `oldLogin`, `generateLoginErrorIfRequired`; Oslo's `loginOnline` still stores the credentials, which OAuth2/OpenID relogin needs (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/LoginPayload.kt` (new)
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/LoginResponse.kt` (new)
- `core/src/main/java/org/hisp/dhis/android/core/user/UserModule.kt` — appended four-argument overloads with `twoFactorCode`: `logIn` (deprecated), `rxLogIn`, `blockingLogIn`, `suspendLogIn`; Oslo's three-argument API untouched (Oslo file, public API)
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/UserModuleImpl.kt` — appended `suspendLogIn` with `twoFactorCode` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/UserNetworkHandler.kt` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/network/user/UserNetworkHandlerImpl.kt` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/network/user/UserService.kt` — `POST auth/login` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/maintenance/D2ErrorCode.kt` — 7 two-factor codes after `INVALID_CHARACTERS` (Oslo file, public API)
- `core/src/main/java/org/hisp/dhis/android/core/arch/api/authentication/internal/ParentAuthenticatorPlugin.kt` — treats `auth/login` as a login call (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/arch/api/authentication/internal/UserIdAuthenticatorHelper.kt` — no user id header before the user is known (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/mockwebserver/Dhis2MockServer.kt` — mock `auth/login` response (Oslo file)
- `core/api/core.api` — generated

Supporting files in the same workflow:
- `core/src/test/java/org/hisp/dhis/android/core/user/internal/LogInCallUnitShould.kt`
- `core/src/test/java/org/hisp/dhis/android/core/arch/api/authentication/internal/ParentAuthenticatorPluginShould.kt`
- `core/src/sharedTest/resources/user/auth_login_success.json`
- `core/src/androidTest/**` — no changes since `1.15.0`: tests call Oslo's three-argument `logIn`/`blockingLogIn`

Touched by the feature commits but restored to Oslo (no EyeSeeTea change left):
- `core/src/test/java/org/hisp/dhis/android/core/trackedentity/search/TrackedEntityInstanceQueryOnlineHelperShould.kt`

Technical note:
- Highest conflict surface of the fork. Oslo `1.15.0` rewrites the login flow
  (OAuth2, credentials auth type), adds rx/suspend variants to `UserModule`
  and moves `D2ErrorCode` to Kotlin. It adds a `TwoFactorAuthManager`
  (enable/disable 2FA) but still no login with a code, so this is not
  absorbed. Disabled Account Login Handling (2.4) is built on this flow.
- Since `1.15.0` the public API is an appended overload over Oslo's
  (technique T4 in `customization-techniques.md`), and `LogInCall.logIn` takes
  a trailing `twoFactorCode: String? = null`. Credentials from the password
  path carry `AuthorizationType.BASIC`, or `ParentAuthenticatorPlugin` stops
  sending them (`store_credentials_with_basic_authorization_after_two_factor_login`).

### 2.6 Automatic Logout On Unauthorized Session

Status: `active`

Spec: `openspec/specs/automatic-logout-on-unauthorized/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/arch/api/authentication/internal/PasswordAndCookieAuthenticator.kt` — `logoutOrReturnRes` on HTTP 401 (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/ConnectLogoutHandler.kt` (new)
- `core/src/main/java/org/hisp/dhis/android/core/user/AccountManager.kt` — `logOutObservable()` (Oslo file, public API)
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/AccountManagerImpl.kt` (Oslo file)

Technical note:
- Consumed by WIDP (restored in app PR #335). Kept as is in `1.15.0`:
  `AccountManager.logOutObservable()` is still only EyeSeeTea's (`core.api`),
  and `AccountManagerImpl` takes `ConnectLogoutHandler` after Oslo's OAuth2 and
  OpenID state stores. Oslo tests that build these classes
  (`AccountManagerImplShould`, `ParentAuthenticatorPluginShould`) need a
  `ConnectLogoutHandler` mock.

### 2.7 Change Server URL

Status: `active`

Spec: `openspec/specs/change-server-url/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/user/AccountManager.kt` — `changeServerUrl()` (Oslo file, public API)
- `core/src/main/java/org/hisp/dhis/android/core/user/internal/AccountManagerImpl.kt` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/configuration/internal/DatabaseConfigurationHelper.kt` — reuses the existing account instead of creating a new one (Oslo file)

Technical note:
- Consumers: SPIP, Sports, WIDP. The account reuse in
  `DatabaseConfigurationHelper` is what keeps the same database after a URL
  change; `fea2d9a6b8` restricted it to the same `encrypted` flag.

### 2.8 Original Size Image Download

Status: `active`

Spec: `openspec/specs/original-size-image-download/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/D2Configuration.kt` — `fileResizerDimension` property, no default and no `fileResizerDimension()` accessor (detekt `TooManyFunctions`) (Oslo file, public API)
- `core/src/main/java/org/hisp/dhis/android/core/arch/helpers/FileResizerHelper.kt` — `Dimension.ORIGINAL` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/core/fileresource/internal/GetDimension.kt` — `getDimension`: the configured dimension for images, otherwise Oslo's `computeScalingDimension` (new, `internal`)
- `core/src/main/java/org/hisp/dhis/android/core/fileresource/internal/FileResourceDownloadCall.kt` — injects `D2Configuration` and calls `getDimension` (Oslo file)

Supporting files in the same workflow:
- `core/src/test/java/org/hisp/dhis/android/core/fileresource/internal/GetDimensionShould.kt`

Touched by the feature commits but restored to Oslo in `1.15.0` (no EyeSeeTea change left):
- `core/src/main/java/org/hisp/dhis/android/core/fileresource/FileResourceDIModule.kt`
- `core/src/main/java/org/hisp/dhis/android/core/fileresource/internal/FileResourceDownloadParams.kt`
- `core/src/test/java/org/hisp/dhis/android/core/fileresource/FileResourceDownloaderShould.kt`

Technical note:
- Broken from the Oslo `1.12.0` merge (PR #461, 2025-09-03) to `1.14.2`:
  Oslo replaced the download with its own content-length-based size selection
  and nothing read the configuration. Re-wired in `1.15.0` on top of
  `FileResourceUtil.computeScalingDimension`, its single call site.
- A configured dimension replaces Oslo's selection for images entirely,
  including its MEDIUM cap and its `maxContentLength` check. Non-images always
  get Oslo's selection. Oslo already sends `ORIGINAL` as a dimension name, so
  `FileResourceService`, `FileResourceDownloadParams` and `FileResourceDIModule`
  are Oslo's.
- Consumers: `develop-spip` configures `ORIGINAL`. WIDP removed its setting in
  app commit `b354b59e8` while this was broken and must add
  `fileResizerDimension(ORIGINAL)` to get original images.

### 2.9 URL Data Element Field

Status: `active`

Spec: `openspec/specs/url-data-element/spec.md`

Main implementation points:
- `core/src/main/java/org/hisp/dhis/android/core/dataelement/DataElement.kt` — `url` / `url()` (Oslo file, public API)
- `core/src/main/java/org/hisp/dhis/android/persistence/dataelement/DataElementDB.kt` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/network/dataelement/DataElementFields.kt` (Oslo file)
- `core/src/main/java/org/hisp/dhis/android/network/dataelement/DataElementDTO.kt` — parses `url` and passes it to the model (Oslo file)
- `core/src/main/assets/migrations/85.sql` — adds the column (Oslo migration)
- `core/src/main/assets/migrations/175.sql` — keeps the column when the table is rebuilt (Oslo migration)

Technical note:
- Consumer: WIDP only. The column lives inside Oslo's own migrations, so a
  new Oslo migration that rebuilds `DataElement` drops it silently: check every
  new migration that touches `DataElement`. Oslo `1.15.0` moves `DataElement`
  to a Kotlin `@ModelBuilder` model.
- Tests: `core/src/test/java/org/hisp/dhis/android/core/dataelement/DataElementUrlShould.kt`
  with its fixture `core/src/sharedTest/resources/dataelement/data_element_with_url.json`
  (a copy of Oslo's `data_element.json` with a non-empty `url`, so Oslo's fixture
  stays untouched).
- The JSON goes through `DataElementDTO` since Oslo ANDROSDK-1981 (January
  2025). The DTO had no `url` until the `1.15.0` upgrade, so `url()` was always
  null in between. Any new Oslo DTO or mapper for `DataElement` must carry it.

## 3. Oslo bug fixes

Patches for Oslo bugs, marked in code with
`// EyeSeeTea fix - <description> (Oslo <ticket>, introduced <version>)`. They
have no spec: each one is retired when Oslo ships its own fix. Check the
"Retire when" column at every upgrade.

| Fix | Oslo ticket | Introduced | Files | Commits | Retire when |
|-----|-------------|------------|-------|---------|-------------|
| Encryption key preserved on database rename | ANDROSDK-2194 | 1.13.1 | `configuration/internal/migration/Migration301.kt`, `configuration/internal/DatabaseEncryptionPasswordManager.kt`, `configuration/internal/DatabaseConfigurationMigration.kt` (+ `Migration301IntegrationShould.kt`, `DatabaseConfigurationMigrationIntegrationShould.kt`) | `f2e617606a`, `3ca4b47125` | Oslo copies the encryption password when Migration301 renames an encrypted database (not in `1.15.0`) |
| Follow the SQLite ALTER TABLE caveats | ANDROSDK-1871 | 1.10.1 | `core/src/main/assets/migrations/164.sql` | `16080ff19b` | Oslo rewrites `164.sql` without `ALTER TABLE ... RENAME` of a referenced table (not in `1.15.0`) |
| Offline-first search keeps local results | ANDROAPP-7646 | — | `trackedentity/search/TrackedEntityInstanceQueryDataFetcher.kt` | `34c6efc4cd` | Oslo makes the online query status lookup null-safe. Oslo merged the rest of this fix in `1.14.1` (`58ef16e3fe`) and reverted that part in `1.15.0` (`2f194a328b`), so online failures reach the app again; capture app 3.5.0 shows them as error rows. Still `!!` in `1.15.0` |

Paths are relative to `core/src/main/java/org/hisp/dhis/android/core/` unless
they start with `core/`.

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

### 4.5 Two-Factor Authentication Login

- `088c7205b3` — Implement login with new auth/login endpoint: Payload has optional twoFactorCode
- `42fee86bf4` — Fix LoginCall unit tests
- `3cabd27114` — Add two factor authentication tests
- `69d759964a` — Add backward compatibility to login in previous versions to 2.41 without two factor endpoint
- `309053f655` — Call old login when unexpected error occur to do login
- `950f63f87d` — Call old login when unexpected error occur to do login
- `2735a34fc3` — Fix 2 factor code login
- `8405e1fd27` — Support new error code INCORRECT_TWO_FACTOR_CODE_TOTP
- `cfc1436e56` — Implement support to two factor by email
- `a532dd36b3` — Support TWO_FACTOR_MANY_SEND_ATTEMPTS error
- `759abb465b` — Implement two factor by sms

### 4.6 Automatic Logout On Unauthorized Session

- `c51c1bb67a` — Implement automatic logout
- `0f390e57f3` — Add tests for launch logout if response code is 401

### 4.7 Change Server URL

- `9068073932` — feat: enable change url of current account
- `fea2d9a6b8` — Fix test export import with encrypt

### 4.8 Original Size Image Download

- `c05c5bd3a3` — Enable by d2Configuration set download images in original size
- `1876279b11` — Fix GetDimensionShould
- `59fb0039ec` — fix: Download images at the configured dimension on top of Oslo's size selection

### 4.9 URL Data Element Field

- `859f0185a1` — Download url of the data elements
- `f3921130f5` — Restore url field in the data element table

## 5. Shared drift still differing

Differences against Oslo that are neither a customization nor an Oslo fix.
Every entry needs a short reason; this section must be empty when an upgrade
closes.

- None. The no-op drift found in `1.14.2` (expanded wildcard imports, stray
  `import android.R`, blank lines, `internal` on `OrphanCleaner` /
  `SubCollectionCleaner`, `86.sql` trailing lines, PMD report settings) was
  restored to Oslo during the `1.15.0` merge.

Not customizations (listed so they are not re-investigated): earlier
EyeSeeTea work that Oslo absorbed or that no longer exists in the code —
attribute values on Program/ProgramStage/DataElement, data element legend
sets, avg/sum program indicators, the full IN operator for TEI attributes,
ProgramStageSection ordering, the `148.sql` fixes, the duplicate
`Content-Disposition` header fix, the "save all cookies" fix (Oslo
ANDROSDK-2344), the relative `Location` redirect fix (Oslo ANDROSDK-2032), and
the RDQA icons and `STRING_TOO_LARGE` blister icon removal (both reverted to
Oslo on 2026-10-22: no consumer, and the build passes with Oslo's icons), and
the online TEI search by UIDs and `;`-separated IN filter values (`e575a8c420`,
`a6a763d83d`, `8ecd4d7a93`; reverted to Oslo on 2026-10-22: only the
`develop-simprints-3.2.1.1` app line used them, pinned to
`1.12.1-eyeseetea-fork-1`).

## 6. Notes

- This inventory reflects the current branch state only.
- If a code comment and a spec title diverge, the spec title wins; fix the
  comment.
- Consumers: `EyeSeeTea/dhis2-android-capture-app` (`develop-eyeseetea` and the
  client branches) via `com.github.EyeSeeTea:dhis2-android-sdk`. How the app
  consumes a customization (worker scheduling, settings UI, per-fork gating)
  is documented in the app repository, not here.
