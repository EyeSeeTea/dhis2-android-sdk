# Upgrade 1.15.0 Notes

Temporary working notes for the Oslo `1.15.0` upgrade. Not stable
documentation: delete when the upgrade closes (see `upgrade-plan.md`).

## Header

- Upstream source: `dhis2/dhis2-android-sdk`
- From Oslo version: `1.14.2` (tag `1.14.2` = `578b62470b`, the merge-base of `develop-eyeseetea`)
- To Oslo version (tag): `1.15.0` (`3ff01d8e93`)
- Upstream tag branch: `origin/upstream/1.15.0` (exists)
- Upgrade branch: `feature/upgrade_1.15.0` (working copy: local `feature/upgrade_1.15.0_temp`, branched from `feature-oca/synced-data-retention-purge`, not pushed)
- OpenSpec change: `openspec/changes/upgrade-to-1-15-0/` (not created yet)
- Started on: `2026-10-21`
- Status: `in_progress` — merge of `1.15.0` started, conflicts not resolved yet

## Progress

- upstream tag branch pushed: `yes`
- onboarding inventory drafted: `yes` (this file, section below)
- onboarding inventory confirmed by a developer: `yes` (titles of A and B, 2026-10-21; A6 split into A6a/A6b; B documented as a table, no spec)
- specs created for confirmed customizations: `yes` (A1–A5), with inventory, checklist and code markers; A6a, A6b, A7 and B4 removed (2026-10-22)
- upgrade change proposed: `yes` (`openspec/changes/upgrade-to-1-15-0`, 2026-10-22)
- merge started: `yes` (2026-10-22, `git merge origin/upstream/1.15.0` on `feature/upgrade_1.15.0_temp`: 26 conflicted files, exactly the ones `git merge-tree` predicted)

## Onboarding inventory (draft — pending developer review)

Method:
- the EyeSeeTea delta is `git diff 1.14.2..HEAD` (90 production files); 1.14.2 is
  exactly the Oslo point `develop-eyeseetea` is based on, so this diff contains
  only EyeSeeTea changes plus the OCA retention work
- the 96 EyeSeeTea-only commits of `develop-eyeseetea`
  (`git rev-list --no-merges develop-eyeseetea --not --remotes=upstream <oslo tags>`)
  were checked one by one: what share of the lines each commit added still
  exists in `HEAD`, and what share exists in Oslo `1.15.0`
- "Consumers" is a grep of the app's `origin/develop-*` branches; it is a hint,
  not proof (the patterns are loose)
- "Oslo 1.15.0 impact" is how much Oslo changed the same files between `1.14.2`
  and `1.15.0` — the expected conflict surface

### A. Proposed customizations (need a spec)

Titles confirmed. A6 is split into A6a (`Tracked Entity Online Search By UIDs`) and A6b (`Semicolon Separated IN Filter Values`).

| # | Proposed title | Kind | What it does | Main files | Feature commits | Consumers (hint) | Oslo 1.15.0 impact |
|---|----------------|------|--------------|------------|-----------------|------------------|--------------------|
| A1 | Two-Factor Authentication Login | customization | Logs in through `POST /api/auth/login` with an optional `twoFactorCode`; maps the 2.41/2.42 2FA statuses (TOTP, email, SMS, too many attempts) to 7 new `D2ErrorCode`s; falls back to the legacy basic-auth login for servers without that endpoint; adds `twoFactorCode` to the public `UserModule.logIn`/`blockingLogIn`. Disabled Account Login Handling builds on it. | `LogInCall.kt`, `LoginPayload.kt`, `LoginResponse.kt`, `UserModule.kt`, `UserModuleImpl.kt`, `UserNetworkHandler(Impl).kt`, `UserService.kt`, `D2ErrorCode.java`, `ParentAuthenticatorPlugin.kt`, `UserIdAuthenticatorHelper.kt`, `Dhis2MockServer.kt` | `088c7205b3`, `69d759964a`, `309053f655`, `950f63f87d`, `2735a34fc3`, `8405e1fd27`, `cfc1436e56`, `a532dd36b3`, `759abb465b` | app `develop-eyeseetea` (login + 2FA error handling) | **high** — Oslo rewrote the login flow (OAuth2, credentials auth type, +108/−18 in `LogInCall.kt`), added rx/suspend variants to `UserModule` (+28/−6) and moved `D2ErrorCode` to Kotlin. Oslo now has `TwoFactorAuthManager` (enable/disable 2FA) but still no login with a 2FA code, so this is not absorbed. |
| A2 | Automatic Logout On Unauthorized Session | customization | When an authenticated request to the account's server gets HTTP 401, removes the stored credentials and emits on `AccountManager.logOutObservable()` so the app can return to login. | `PasswordAndCookieAuthenticator.kt`, `ConnectLogoutHandler.kt` (new), `AccountManager.kt`, `AccountManagerImpl.kt` | `c51c1bb67a`, `0f390e57f3` | widp (restored in app PR #335 after being lost in 3.3.1), spocc | medium — Oslo added its own `logOutObservable` for OAuth2/OpenID only; check whether one mechanism can serve both. |
| A3 | Change Server URL | customization | `AccountManager.changeServerUrl()` points the logged-in account (credentials and database configuration) at a new server URL without logging out. | `AccountManager.kt`, `AccountManagerImpl.kt` | `9068073932` | spip, sports, widp | low–medium (`AccountManagerImpl.kt` +8/−2) |
| A4 | Original Size Image Download | customization (**broken**) | `D2Configuration.fileResizerDimension` lets the app choose the image dimension for file resource downloads, including a new `ORIGINAL` value that requests the file without resizing. | `D2Configuration.java`, `FileResizerHelper.kt`, `FileResourceDIModule.kt`, `FileResourceDownloadParams.kt`, `FileResourceDownloadCall.kt`, `FileResourceService.kt`, `GetDimension.kt` (new) | `c05c5bd3a3` | spip, spocc, widp | **high** — `D2Configuration` moved to Kotlin, `FileResizerHelper.kt` (+56/−52) and `FileResourceDownloadCall.kt` (+31/−8) changed. |
| A5 | URL Data Element Field | customization | Downloads and stores the `url` field of data elements and exposes it as `DataElement.url()`. | `DataElement.java`, `DataElementDB.kt`, `DataElementFields.kt`, migrations `85.sql` and `175.sql` | `859f0185a1`, `f3921130f5`, `bb6494186c` | widp only — app spec `url-data-element` ("URL data element field"): `EventRepository.kt` reads `de?.url()`, `FieldUiModelExtensions.supportingText()` renders it under the field description; enrollment fields pass `url = null`. Kept in app PR #335 (3.4.2), not tested manually there. Other branches still reference it but no longer use it; may be dropped in a future upgrade | medium — `DataElement` moved to a Kotlin `@ModelBuilder` model. **The column is added by editing Oslo's existing migrations `85.sql` and `175.sql`**, not by a migration of its own: decide whether to keep that. |
| ~~A6~~ | ~~Tracked Entity Search By UIDs And Semicolon Lists~~ | removed | Online TEI queries passed the scope's `uids` to the server (A6a), and IN-filter values could be given as a `;`-separated string (A6b), for Simprints biometric GUID search (`e575a8c420`, `a6a763d83d`, `8ecd4d7a93`). No active app branch uses them: they search with `byFilter(...).in(list)`, which the SDK serializes with its own separator, and nobody calls `byTrackedEntities()`. Only `develop-simprints-3.2.1.1` (`byUIds()`, `in("a;b")`) used them, pinned to SDK `1.12.1-eyeseetea-fork-1`. **Reverted to Oslo 2026-10-22**, including the Oslo test case for `;` values. | | | | |
| ~~A7~~ | ~~RDQA Icons~~ | removed | Added the `ic_rdqa_*` drawables (`12d1dabaa6`, 2020, likely for PSI, no longer a client). No consumer in any app branch; the icons are only reachable through server icon metadata. **Removed 2026-10-22.** | | | | |

### B. Proposed Oslo bug fixes (`// EyeSeeTea fix`, still need a spec)

| # | Proposed title | What it fixes | Files | Commits | Oslo 1.15.0 |
|---|----------------|---------------|-------|---------|-------------|
| B1 | Encryption Key Preserved On Database Rename | Migration301 renames encrypted databases without moving their password; the fix copies it to the new name. | `Migration301.kt`, `DatabaseEncryptionPasswordManager.kt`, `DatabaseConfigurationMigration.kt` | `f2e617606a`, `3ca4b47125` | not fixed upstream |
| B2 | Program Rename Migration Fix | `164.sql` follows the SQLite `ALTER TABLE` caveats (Oslo `ANDROSDK-1871`). | `164.sql` | `16080ff19b` | not fixed upstream |
| B3 | Offline-First Search Keeps Local Results | Null-safe online query status so an `OFFLINE_FIRST` TEI search returns local results when the online part fails, instead of crashing. | `TrackedEntityInstanceQueryDataFetcher.kt` | `34c6efc4cd` | not fixed upstream |
| ~~B4~~ | ~~Oversized Icon Resources Removed~~ | Removed 6 `ic_blister_pills_*_x16` drawables that broke the build with `STRING_TOO_LARGE` (`ab1f7f4c50`, 2022). | | | **Reverted to Oslo 2026-10-22**: icons restored, `./gradlew :core:assembleDebug` passes. |

### C. Needs a decision (intent not clear)

- ~~`DatabaseConfigurationHelper.kt`~~ — resolved: part of A3 (Change Server URL), introduced by `9068073932` and restricted to the same `encrypted` flag by `fea2d9a6b8`.
- `86.sql` — only two trailing blank lines left after the `url` column moved to
  `85.sql`. Drift; restore Oslo's version.
- `core/plugins/android-pmd.gradle` — PMD report settings commented out "to
  avoid compile errors". The file no longer exists in Oslo `1.15.0`, so this
  disappears with the upgrade.

### D. Drift to drop during the upgrade (no behavior change)

Restore Oslo's version (`accept_theirs`):
- wildcard imports expanded or unused imports removed: `AnalyticExpressionParserUtils.kt`, `DimensionalResponseSamples.kt`, `GridAnalyticsResponseSamples.kt`, `APICallErrorCatcher.kt`, `ApiPagingEngine.kt`, `DatabaseMigrationParser.kt`, `DateFilterConnector.kt`, `EventDataFilterConnector.kt`, `DataSetHandler.kt`, `PastExpiryDateConflict.kt`, `PeriodAfterLatestOpenFutureConflict.kt`, `AggregatedDataCallBundleKey.kt`, `EnrollmentImportHandler.kt`, `EventImportHandler.kt`, `EventServiceImpl.kt`, `OrganisationUnitService.kt`, `ParserUtils.kt`, `ExpressionHelper.kt`, `ParentPeriodGenerator.kt`, `D2Modulus.kt`, `VCurrentDate.kt`, `JobReportHandler.kt`, `TrackerConflictHelper.kt`, `UserOrganisationUnitLinkStore.kt`, `OpenIDConnectHandlerImpl.kt`, `DateExtensions.kt`
- stray `import android.R` (looks accidental): `ScopedFilterConnectorFactory.kt`, `TrackerItemFilterConnector.kt`
- `OrphanCleaner.kt`, `SubCollectionCleaner.kt` — `interface` made `internal`
- `AppearanceSettings.java` — `AppearanceSettings.Builder` shortened to `Builder`
- blank-line-only: `DeletableDataColumns.kt`, `ForeignKeyCleaner.kt`
- ~~`Migration301.kt` — extra log lines around B1 (keep only the fix)~~ done in task 4.3

### E. Absorbed by Oslo or no longer present (no spec)

Commits whose code no longer exists in `HEAD` (0–30 % of their lines survive,
mostly as code Oslo itself now has under different files). No action beyond
confirming nobody relies on them:

- attributes and attribute values on `Program`, `ProgramStage`, `DataElement`
  (2020, `b9834423ed` … `a0d92b59cc`) — Oslo has `attributeValues` natively
- data element legend sets (2020, `55797e4a83` … `e809249fd3`)
- `avg`/`sum` aggregation for program indicators (`e3265edc93`, `f7365d0f86`,
  `de215c995f`, `7c8790b2f5`)
- full IN operator for TEI attribute queries (`83fb05abfc`, `76cda15294`) —
  A6 was its last remnant, reverted to Oslo on 2026-10-22
- ProgramStageSection order by `sortOrder` / order direction (`0caade7fa9`,
  `ca72a641eb`) — identical code in Oslo `1.15.0`
- migration `148.sql` fixes (`0239e1a634`, `3a8d7c9350`) and snapshot
  comparison (`d56f48e502`) — identical in Oslo `1.15.0`
- duplicate `Content-Disposition` header (`0a3d30e62d`) — identical in Oslo
- logout on redirect to `dhis-web-login` (`0111e24737`) — Oslo's
  `LOGIN_KEY_LIST` covers it

Lost in Oslo rewrites, confirmed not needed (2026-10-22):
- `f251e2f37f` "Save all cookies if sent by server" — covered by Oslo
  ANDROSDK-2344 (in `1.14.2`): `CookieAuthenticatorHelper` stores every
  `Set-Cookie` per host and `Path` and sends all matching ones in one `Cookie`
  header.
- `87b902ba3a` "Fix bug when location is not a URL" — avoided the OkHttp crash
  on a relative `Location` in `ServerURLVersionRedirectionInterceptor.kt`. Oslo
  ANDROSDK-2032 replaced it with the Ktor `ServerURLVersionRedirectionPlugin`,
  whose `URLBuilder.takeFrom` resolves relative locations, so the crash cannot
  happen. Same file in `1.15.0`.

### F. Already documented

Synced Data Retention Purge, Synced Data Relationships Purge, Synced Data
Retention Scope, Disabled Account Login Handling — spec and
`customization-files.md` entries exist. Conflict surface in `1.15.0`:
`D2DIComponentFactory.kt` (+48/−48), `LogInCall.kt` (shared with A1),
`D2ErrorCode` (moved to Kotlin).

## Open Questions

- A5: keep the `url` column inside Oslo's migrations `85.sql`/`175.sql` for this
  upgrade (recommended), and revisit when WIDP stops using it.

## Decisions

- A4 Original Size Image Download: **re-wire it during the `1.15.0` merge** on
  top of Oslo's size selection (`FileResourceUtil.computeScalingDimension`,
  called from `FileResourceDownloadCall.downloadFile`). Not fixed before the
  merge. WIDP wants to keep the original size; `develop-widp` does not set
  `fileResizerDimension` today (it set `MEDIUM` in `952770b35`, 2025-01-09,
  and removed it as dead code in `b354b59e8`, 2026-03-09), so the WIDP app
  needs `.fileResizerDimension(ORIGINAL)` once the SDK is fixed.

### File classification (task 2.1)

Categories from `conflict-rules.md`, plus two for files git did not flag:
`verify_automerge` (both sides changed, git auto-merged: read the two-dot
diff) and `no_conflict` (Oslo did not change the file). Paths under
`core/src/main/java/org/hisp/dhis/android/` are shown without that prefix;
test paths keep their source set. "Expected delta" is what should remain
against `origin/upstream/1.15.0` once resolved.

| File | Classification | Expected delta | Customization | Status | Notes |
|------|----------------|----------------|---------------|--------|-------|
| `core/api/core.api` | accept_theirs | regenerated: only the customizations' public additions | n/a (generated) | resolved (Oslo, regenerate in 5.3) | regenerate in task 5.3, never by hand |
| `.github/workflows/ci.yml` | manual_reapply_on_theirs | skip BrowserStack without credentials: `Check BrowserStack credentials` step, `if` on the run step, `Skip BrowserStack tests notice` step, `if` on the summary link (fork commits `39fa1d8231`, re-applied in `dfe8a235fe`) | n/a (fork identity) | resolved | Oslo 1.15.0 still runs BrowserStack unconditionally (401 without secrets). The fork's `jacoco-report` `if` is dropped: Oslo removed the job-level `if` and tolerates missing reports, and the job is `success`, never `skipped`, when the step is skipped |
| `CLAUDE.md` | accept_theirs | one line: `@AGENTS-eyeseetea.md` import | n/a (fork identity) | resolved | add/add: Oslo now ships its own |
| `gradle/libs.versions.toml` | accept_theirs | `dhis2AndroidSdkVersion = "1.15.0-eyeseetea-fork-1"` | n/a (fork identity) | resolved | Oslo's tag says `1.15.0-SNAPSHOT`; `dhis2AndroidSdkCode` stays Oslo's `306` |
| `core/plugins/android-pmd.gradle` | accept_theirs | file deleted (as Oslo) | n/a (drift) | resolved | modify/delete: the fork only changed PMD report settings |
| `core/src/sharedTest/java/org/hisp/dhis/android/core/data/systeminfo/SystemInfoSamples.kt` | accept_theirs | none | n/a | resolved | false rename pairing by git: the HEAD side is `AppearanceSettings.java` (no-op drift, already deleted as in Oslo) |
| `core/user/openid/OpenIDConnectHandlerImpl.kt` | accept_theirs | none | n/a (drift) | resolved | fork side only had expanded imports |
| `core/src/androidTest/java/org/hisp/dhis/android/core/event/CreateEventUtils.java` | accept_theirs | file deleted (as Oslo) | n/a (drift) | resolved | modify/delete; fork change was `Columns.UID` import drift |
| `core/src/androidTest/java/org/hisp/dhis/android/core/user/LoginErrorHandlingRealIntegrationShould.java` | accept_theirs | file deleted (as Oslo) | Two-Factor Authentication Login | resolved | modify/delete; fork change was `logIn(..., null)` |
| `core/src/androidTest/java/org/hisp/dhis/android/core/LogInCallRealIntegrationShould.kt` | accept_theirs | none | Two-Factor Authentication Login | resolved | Java → Kotlin; fork change was `logIn(..., null)`, unnecessary with design D2 |
| `core/src/androidTest/java/org/hisp/dhis/android/core/MultiUserRealIntegrationShould.kt` | accept_theirs | none | Two-Factor Authentication Login | resolved | same as above |
| `core/src/androidTest/java/org/hisp/dhis/android/core/dataset/internal/DataSetCompleteRegistrationCallRealIntegrationShould.kt` | accept_theirs | none | Two-Factor Authentication Login | resolved | same as above |
| `core/src/androidTest/java/org/hisp/dhis/android/core/event/internal/EventDownloadRealIntegrationShould.kt` | accept_theirs | none | Two-Factor Authentication Login | resolved | same as above |
| `core/src/androidTest/java/org/hisp/dhis/android/core/trackedentity/TrackedEntityInstanceCallRealIntegrationShould.kt` | accept_theirs | none | Two-Factor Authentication Login | resolved | same as above |
| `network/fileresource/FileResourceService.kt` | accept_theirs | none | Original Size Image Download | resolved | design D3: `ORIGINAL` sent as Oslo's dimension name, no nullable `dimension` |
| `core/user/UserModule.kt` | manual_reapply_on_theirs | appended block: four-argument `logIn`/`rxLogIn`/`blockingLogIn`/`suspendLogIn` | Two-Factor Authentication Login | resolved | design D2; Oslo's three-argument API untouched |
| `core/user/internal/UserModuleImpl.kt` | manual_reapply_on_theirs | appended block implementing the four-argument overloads | Two-Factor Authentication Login | resolved | design D2 |
| `core/user/internal/LogInCall.kt` | manual_reapply_on_theirs | `twoFactorCode = null` parameter, `/api/auth/login` request, 2FA status mapping, basic-auth fallback, disabled-account branch | Two-Factor Authentication Login, Disabled Account Login Handling | resolved | `loginOnline` keeps Oslo's `credentialsSecureStore.set(credentials)` again: Oslo's OAuth2/OpenID token login (`logInWithToken`) relies on it, and the fork's removal made Oslo's `keep_the_pin_in_the_credentials_on_oauth2_online_relogin` fail. Basic-login credentials come from `Credentials(...)` with no OAuth/OpenID state, so they are `BASIC` by default (unit test added). | Oslo: `passwordOrPin`, OAuth2 relogin, `authorizationType`; credentials from 2FA login MUST be `BASIC` |
| `core/maintenance/D2ErrorCode.kt` | manual_reapply_on_theirs | 7 two-factor codes appended to the enum | Two-Factor Authentication Login | resolved | Java → Kotlin; `D2ErrorCode.java` already staged as deleted. Disabled Account Login Handling has no own code: it uses Oslo's `USER_ACCOUNT_DISABLED` |
| `core/src/test/java/org/hisp/dhis/android/core/user/internal/LogInCallUnitShould.kt` | manual_reapply_on_theirs | fork's 2FA and disabled-account test cases | Two-Factor Authentication Login, Disabled Account Login Handling | resolved | Oslo's test plus `twoFactorCode`, `whenLoginAPICall`, the 2FA, disabled-account and old-login tests, and a new BASIC-credentials test; 50 tests pass | 9 hunks; Oslo +451 lines |
| `core/src/test/java/org/hisp/dhis/android/core/arch/api/authentication/internal/ParentAuthenticatorPluginShould.kt` | manual_reapply_on_theirs | fork's `auth/login` login-call cases | Two-Factor Authentication Login | resolved | not 2FA: only the Automatic Logout mock; fork's expanded imports dropped (no-op drift) |  |
| `core/user/internal/AccountManagerImpl.kt` | manual_reapply_on_theirs | `connectLogoutHandler` constructor param + `logOutObservable()` + `changeServerUrl()` | Automatic Logout On Unauthorized Session, Change Server URL | resolved | conflict only in the constructor: Oslo's two secure stores, then `connectLogoutHandler` |
| `core/configuration/internal/DatabaseConfigurationHelper.kt` | manual_reapply_on_theirs | reuse existing account, updating its `authorizationType`; new accounts built with Oslo's `authorizationType` | Change Server URL | resolved | a reused account takes the login's `authorizationType`, so switching basic ↔ OAuth2 does not keep the old type. Only caller (`RoomMultiUserDatabaseManager`) always passes a non-null type |
| `core/arch/d2/internal/D2DIComponentFactory.kt` | manual_reapply_on_theirs | import + `RetentionDIModule::class` with marker | Synced Data Retention Scope | resolved | Oslo ANDROSDK-2241 (Koin compile safety) rewrote the module list |
| `core/D2Configuration.kt` | manual_reapply_on_theirs | `fileResizerDimension` field and accessor, no default | Original Size Image Download | resolved | Java → Kotlin; `D2Configuration.java` is modify/delete → accept deletion |
| `core/dataelement/DataElement.kt` | manual_reapply_on_theirs | `url` field and accessor | URL Data Element Field | resolved | Java → Kotlin; `DataElement.java` is modify/delete → accept deletion. Finding: `network/dataelement/DataElementDTO.kt` (Oslo ANDROSDK-1981, Jan 2025) has no `url`, so the requested field is dropped when parsing and `url()` is always null (broken since that Oslo merge; also on `develop-eyeseetea` and 1.14.2 base) |
| `core/arch/api/authentication/internal/ParentAuthenticatorPlugin.kt` | verify_automerge | titled block: `auth/login` counts as a login call | Two-Factor Authentication Login | resolved | `auth/login` check sits before Oslo's `when` on `authorizationType`; two-dot diff is the titled block | Oslo now dispatches on `authorizationType` (ANDROSDK-2369) |
| `core/mockwebserver/Dhis2MockServer.kt` | verify_automerge | mock `auth/login` response | Two-Factor Authentication Login | pending |  |
| `core/arch/helpers/FileResizerHelper.kt` | verify_automerge | `Dimension.ORIGINAL` | Original Size Image Download | pending | re-check against Oslo's `DimensionSize` in task 6 |
| `core/fileresource/internal/FileResourceDownloadParams.kt` | accept_theirs | none | Original Size Image Download | resolved | restored to Oslo; design D3 drops the fork's `dimension` |
| `core/fileresource/internal/FileResourceDownloadCall.kt` | verify_automerge | task 6: one call to the EyeSeeTea dimension resolution | Original Size Image Download | resolved (merge) | import drift restored to Oslo; task 6 adds the call |
| `persistence/dataelement/DataElementDB.kt` | verify_automerge | `url` column mapping | URL Data Element Field | pending |  |
| `core/src/androidTest/java/org/hisp/dhis/android/core/dataset/internal/DataSetElementStoreIntegrationShould.kt` | verify_automerge | test of `getDataSetsForDataElement` | Synced Data Retention Scope | pending |  |
| `core/configuration/internal/DatabaseEncryptionPasswordManager.kt` | defer_after_build_verification | fix block | Oslo fix: Encryption key preserved on database rename | resolved: kept | Oslo 1.15.0 only touched it for Koin; its `Migration301` still does not copy the password |
| `core/trackedentity/search/TrackedEntityInstanceQueryDataFetcher.kt` | defer_after_build_verification | null-safe status lookup, or none if Oslo fixed it | Oslo fix: Offline-first search keeps local results | resolved: kept | Oslo 1.15.0 reverted its online-failure filter (`2f194a328b`); only our null-safe status lookup remains (Oslo still uses `!!`). Capture app 3.5.0 reads the search through `resultDataSource` and maps each `Result.Failure` to an error row, so local results stay visible |
| `core/src/main/assets/migrations/85.sql, 175.sql` | no_conflict | `url` column | URL Data Element Field | resolved | new `181.sql` does not rebuild `DataElement` |
| `core/src/main/assets/migrations/164.sql` | no_conflict | fix block | Oslo fix: Follow the SQLite ALTER TABLE caveats | resolved: kept | Oslo's `164.sql` unchanged since 2024; still runs for databases older than version 164 |
| `core/configuration/internal/migration/Migration301.kt, DatabaseConfigurationMigration.kt`, `Migration301IntegrationShould.kt`, `DatabaseConfigurationMigrationIntegrationShould.kt` (androidTest) | no_conflict | fix blocks | Oslo fix: Encryption key preserved on database rename | resolved: kept | `Migration301.kt` rebuilt from Oslo with only the marked fix (extra log lines and KDoc edits dropped) |
| `core/arch/api/authentication/internal/PasswordAndCookieAuthenticator.kt` | no_conflict | `logoutOrReturnRes` on HTTP 401 | Automatic Logout On Unauthorized Session | pending |  |
| `core/user/AccountManager.kt` | no_conflict | `changeServerUrl()`, `logOutObservable()` | Automatic Logout On Unauthorized Session, Change Server URL | pending |  |
| `core/arch/api/authentication/internal/UserIdAuthenticatorHelper.kt, core/user/internal/UserNetworkHandler.kt, network/user/UserNetworkHandlerImpl.kt, network/user/UserService.kt` | no_conflict | 2FA login request and headers | Two-Factor Authentication Login | pending |  |
| `core/D2.kt, core/arch/d2/internal/D2DIComponent.kt` | no_conflict | `retentionModule()` accessor and wiring | Synced Data Retention Scope | pending | no `// EyeSeeTea customization` marker today: add it (task 8.1) |
| `core/dataset/internal/DataSetElementStore.kt, persistence/dataset/DataSetDataElementLinkStoreImpl.kt` | no_conflict | `getDataSetsForDataElement` | Synced Data Retention Scope | pending |  |
| `core/fileresource/FileResourceDIModule.kt` | no_conflict | configured dimension wiring (task 6) | Original Size Image Download | resolved (merge) | restored to Oslo: its `FileResourceDownloadParams` has no `dimension`; task 6 wires the configuration again |
| `network/dataelement/DataElementFields.kt` | no_conflict | `url` field | URL Data Element Field | pending |  |
| `core/src/test/java/org/hisp/dhis/android/core/fileresource/FileResourceDownloaderShould.kt, core/fileresource/internal/GetDimensionShould.kt` | no_conflict | rewritten in task 6 | Original Size Image Download | partial | `FileResourceDownloaderShould.kt` restored to Oslo (asserted the dropped `params.dimension`); `GetDimensionShould.kt` kept until task 6 |
| `core/src/test/java/org/hisp/dhis/android/core/trackedentity/search/TrackedEntityInstanceQueryOnlineHelperShould.kt` | no_conflict | none | n/a | pending | identical to Oslo since A6 was reverted: drop from section 2.5 of `customization-files.md` (task 8.1) |
| EyeSeeTea-owned files: `retention/**` (+ tests), `ConnectLogoutHandler.kt`, `LoginPayload.kt`, `LoginResponse.kt`, `GetDimension.kt`, `auth_login_success.json`, `jitpack.yml`, `EyeSeeTea.md`, `.github/workflows/eyeseetea-main.yml` | no_conflict | unchanged | several | pending | not in Oslo; only need to compile against `1.15.0` |


### Findings while resolving group 3

- `core/dataset/internal/DataSetHandler.kt`: fork had the `dataset.*` wildcard import expanded (no-op drift), including `DataSetInternalAccessor`, which Oslo 1.15.0 deletes (ANDROSDK-2293). Restored to Oslo; it was the first compile error.
- `RetentionDIModule.kt` (EyeSeeTea-owned): Koin compile safety (ANDROSDK-2241) validates it on its own because no Oslo module includes it (Oslo's aggregator is `WipeDIModule`). It now declares `includes = [ArchDIModule, PersistenceDIModule, SettingsDIModule]`; no Oslo file touched.
- `AccountManagerImplShould.kt` (new Oslo test): constructor gets the Automatic Logout `connectLogoutHandler` mock.
- `DataSetRetentionLimitResolverShould.kt`, `ProgramRetentionLimitResolverShould.kt`: Oslo's Kotlin `DataSetSettings`/`ProgramSettings` make `globalSettings` non-null; the "no global settings" case uses an empty global setting.
- Automerge verification (tasks 4.1, 4.2), two-dot diff of the whole tree against `origin/upstream/1.15.0`:
  - Restored to Oslo (71 files, no-op drift): 41 `androidTest` files whose only change was the fourth `null` argument of `logIn`/`blockingLogIn` (Oslo's three-argument API is kept, design D2), plus an old `enqueueMockResponse(204)` call and expanded imports in three of them; 28 main files with expanded wildcard imports, removed unused imports, `internal` on `OrphanCleaner`/`SubCollectionCleaner`, and `86.sql` trailing lines.
  - Every remaining hunk in an Oslo file carries its marker, except `D2.kt` and `D2DIComponent.kt` (retention accessor and wiring; marker in task 8.1) and the Oslo fixes `Migration301.kt` / `164.sql` and their tests (task 4.3). EyeSeeTea-owned files (`retention/`, `jitpack.yml`, `eyeseetea-main.yml`, `auth_login_success.json`) have no markers by convention.
  - `.idea/gradle.xml` restored to Oslo (`jbr-17`), decided by the user.
- URL Data Element Field fixed: `DataElementDTO.kt` gets `url` and maps it in `toDomain()`; `DataElementUrlShould` failed before the fix (`url()` null) and passes after it.
  - `:core:compileDebugKotlin`, `:core:compileDebugUnitTestKotlin` and `:core:compileDebugAndroidTestKotlin` pass after the restore.
- `:core:compileDebugKotlin` passes; targeted unit tests pass (LogInCallUnitShould 50, ParentAuthenticatorPluginShould 2, AccountManagerImplShould 3, retention resolvers 9). Full `testDebugUnitTest` is task 5.2.

## Validation Notes

- Build (tasks 5.1 to 5.3, 2026-10-22):
  - `:core:assembleDebug` passes, including Koin compile safety for `RetentionDIModule`.
  - `:core:testDebugUnitTest`: 1789 tests in 428 classes, 0 failures, 1 skipped (`FieldsStringGenerationShould`, `@Ignore` in Oslo). No test is left for task 6 to fix; `FileResourceDownloaderShould` is Oslo's and passes.
  - `:core:releaseApiDump`: the `core.api` diff against Oslo only adds the customizations' public API: `D2.retentionModule()`, `RetentionModule`, `D2Configuration.fileResizerDimension` (constructor and `copy` change shape), `FileResizerHelper.Dimension.ORIGINAL`, `GetDimensionKt.getDimension` (rewritten in task 6), `DatabaseEncryptionPasswordManager.copyPasswordForRenamedDatabase` (B1), `DataElement.url` and `DataElementTableInfo.URL`, the seven two-factor `D2ErrorCode`s, `AccountManager.changeServerUrl` / `logOutObservable`, the four-argument `UserModule` overloads, and the Koin hints of `RetentionDIModule` and `ConnectLogoutHandler`.

- Original Size Image Download re-wired (tasks 6.1, 6.2, 2026-10-22):
  - `GetDimension.kt` (now `internal`): for images, the configured `D2Configuration.fileResizerDimension` name when set, otherwise Oslo's `computeScalingDimension`; non-images always get Oslo's result. A configured dimension also overrides Oslo's `NOT_SUPPORTED` (image above `maxContentLength`), per design D3.
  - `FileResourceDownloadCall.kt`: `D2Configuration` injected through Koin (already a singleton in `appContextDIModule`, accepted by compile safety) and one call to `getDimension` instead of `computeScalingDimension`; `FileResourceDIModule`, `FileResourceDownloadParams` and `FileResourceService` stay as Oslo's.
  - `GetDimensionShould` rewritten: 3 of 5 tests failed against the stock-only behaviour and pass after the change. Full unit suite: 1792 tests, 0 failures. `core.api` only loses `GetDimensionKt`.
  - Gotcha: running `releaseApiDump` in the same Gradle invocation as test compilations adds `moduledef_*` Koin hints for Oslo modules to `core.api`; regenerate it from `:core:clean`.
  - `:core:detekt` fails with two issues left by the merge, for task 7.1: `D2Configuration` `TooManyFunctions` (11/11, the `fileResizerDimension()` accessor) and `LogInCallUnitShould` `LargeClass` (601/600).

## Finalization

- surviving customizations reflected in specs and `customization-files.md`: `no`
- absorbed customizations removed (code, spec, inventory, checklist): `no`
- stable rules moved to `conflict-rules.md`: `no`
- unexplained diff remaining against upstream: `yes`
- upgrade change archived and this file deleted: `no`
