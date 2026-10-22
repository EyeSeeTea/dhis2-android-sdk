## Why

The fork is on Oslo `1.14.2` and the consuming apps are moving to capture-app
`3.5.0`, which needs Oslo **`1.15.0`** (tag `1.15.0`, `upstream/1.15.0`,
`3ff01d8e93`; 626 commits since `1.14.2`). The upgrade is also the agreed
moment to repair Original Size Image Download, broken since the Oslo `1.12.0`
merge.

## What Changes

- Merge `origin/upstream/1.15.0` into the fork following
  `eyeseetea-docs/upgrade/upgrade-plan.md` (phases 2 to 7).
- Re-apply every active customization on top of Oslo `1.15.0` with its
  requirements unchanged; only the baseline moves:
  - Two-Factor Authentication Login and Disabled Account Login Handling on
    Oslo's new login flow (rx/blocking/suspend `UserModule` API, PIN login,
    authorization type).
  - Original Size Image Download re-wired on Oslo's download size selection,
    so the configured dimension is honoured again (it is ignored today). Its
    requirements are adjusted to that selection (see Capabilities).
  - URL Data Element Field and the `D2Configuration` part of Original Size
    Image Download moved into the Kotlin models that replace the Java ones.
  - Change Server URL, Automatic Logout On Unauthorized Session and the three
    Synced Data retention customizations re-applied where they conflict.
- Re-check the three Oslo bug fixes (`// EyeSeeTea fix`) and retire any that
  Oslo `1.15.0` already fixes.
- Restore Oslo's version of every no-op drift file.
- Regenerate `core/api/core.api`, bump `dhis2AndroidSdkVersion` to
  `1.15.0-eyeseetea-fork-1` and update the documentation baseline.
- Not **BREAKING** for apps by design: the four-argument `logIn` with a
  two-factor code keeps compiling (see design.md). Apps still have to adapt to
  Oslo's own `1.15.0` API changes, which are not part of this change.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `original-size-image-download`: the dimension setting becomes an override of
  Oslo's own size selection. Without a configured dimension the SDK keeps
  Oslo's selection (what every app gets today) instead of a fixed medium size,
  and a configured original size is requested as Oslo's `ORIGINAL` dimension
  instead of a request without dimension. The spec's "broken" status note is
  removed.

Every other active spec keeps its requirements unchanged.

## Impact

**Target:** Oslo tag `1.15.0`. Work starts on the local branch
`feature/upgrade_1.15.0_temp` to find out what the merge involves (see
design.md, "Branch").

**Customizations touching Oslo files that changed between `1.14.2` and
`1.15.0`** (from `git merge-tree HEAD upstream/1.15.0`: 26 conflicted files,
27 files changed on both sides that git auto-merges):

| Customization | Oslo files changed upstream | Conflict |
|---|---|---|
| Two-Factor Authentication Login | `UserModule.kt`, `UserModuleImpl.kt`, `LogInCall.kt`, `D2ErrorCode` (Java → Kotlin), `ParentAuthenticatorPlugin.kt`, `LogInCallUnitShould.kt`, `ParentAuthenticatorPluginShould.kt`, 5 real-server integration tests (Java → Kotlin) | yes (plugin auto-merged) |
| Disabled Account Login Handling | `LogInCall.kt`, `D2ErrorCode` | yes |
| Original Size Image Download | `D2Configuration` (Java → Kotlin), `FileResourceService.kt`, `FileResizerHelper.kt`, `FileResourceDownloadCall.kt`, `FileResourceDownloadParams.kt` | yes (3 auto-merged) |
| URL Data Element Field | `DataElement` (Java → Kotlin), `DataElementDB.kt` | yes (DB auto-merged) |
| Change Server URL | `DatabaseConfigurationHelper.kt` | yes |
| Automatic Logout On Unauthorized Session | `AccountManagerImpl.kt` | yes |
| Synced Data Retention Purge / Relationships Purge / Retention Scope | `D2DIComponentFactory.kt` (DI module registration) | yes |
| Oslo fixes (encryption key on DB rename, offline-first search) | `DatabaseEncryptionPasswordManager.kt`, `TrackedEntityInstanceQueryDataFetcher.kt` | auto-merged |

Also conflicted, not customizations: `core.api` (regenerated), `CLAUDE.md`
(Oslo now ships one; keep it and add the `@AGENTS-eyeseetea.md` import),
`.github/workflows/ci.yml`, `gradle/libs.versions.toml`,
`core/plugins/android-pmd.gradle` (deleted by Oslo), `SystemInfoSamples`,
`OpenIDConnectHandlerImpl.kt` (imports only).

**EyeSeeTea-owned files vs Oslo edits:** the retention packages
(`retention/`), `ConnectLogoutHandler.kt` and `GetDimension.kt` are
EyeSeeTea-owned and do not conflict; every file in the table above is an Oslo
file edited by the fork.

**Domain modules affected:** user (login, account manager), fileresource,
dataelement, configuration, maintenance (error codes), trackedentity (search
fix only). No `ModuleWiper` change, and the shared `ModuleWiper` interface is
not touched.

**Public API:** `core.api` is regenerated. Two-factor login stays available
with the same four-argument call; `D2Configuration` and `DataElement` keep
`fileResizerDimension` and `url`, now in Kotlin.

**Consumers:** `dhis2-android-capture-app` branches `develop-eyeseetea`,
`develop-widp`, `develop-oca`, `develop-spip`, `develop-sports` must build
against the new version; WIDP must set `.fileResizerDimension(ORIGINAL)` to
get original images.
