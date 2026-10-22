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
- Status: `onboarding_inventory` — customizations are being inventoried before the merge

## Progress

- upstream tag branch pushed: `yes`
- onboarding inventory drafted: `yes` (this file, section below)
- onboarding inventory confirmed by a developer: `yes` (titles of A and B, 2026-10-21; A6 split into A6a/A6b; B documented as a table, no spec)
- specs created for confirmed customizations: `yes` (A1–A5), with inventory, checklist and code markers; A6a, A6b, A7 and B4 removed (2026-10-22)
- upgrade change proposed: `no`
- merge started: `no`

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
- `Migration301.kt` — extra log lines around B1 (keep only the fix)

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

| File | Classification | Expected delta | Customization | Status | Notes |
|------|----------------|----------------|---------------|--------|-------|

## Validation Notes

## Finalization

- surviving customizations reflected in specs and `customization-files.md`: `no`
- absorbed customizations removed (code, spec, inventory, checklist): `no`
- stable rules moved to `conflict-rules.md`: `no`
- unexplained diff remaining against upstream: `yes`
- upgrade change archived and this file deleted: `no`
