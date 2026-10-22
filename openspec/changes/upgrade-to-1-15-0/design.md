## Context

See proposal.md for motivation and the conflict surface. The facts that shape
the approach:

- `git merge-tree HEAD upstream/1.15.0` reports 26 conflicted files and 27
  files changed on both sides that git auto-merges. The auto-merged ones still
  have to be read (automerge verification rule in `conflict-rules.md`).
- Oslo `1.15.0` moves `D2Configuration`, `DataElement` and `D2ErrorCode` from
  Java AutoValue to Kotlin `@ModelBuilder` data classes / enums, and deletes
  `core/plugins/android-pmd.gradle`.
- Oslo `1.15.0` reworks the user module: `UserModule.logIn` is deprecated in
  favour of `rxLogIn` / `blockingLogIn` / `suspendLogIn` (three arguments),
  `LogInCall.logIn` takes `passwordOrPin` (PIN login), credentials carry an
  `authorizationType`, and `ParentAuthenticatorPlugin` only handles a password
  call when that type is `BASIC`.
- Oslo `1.15.0` chooses the image download size in
  `FileResourceUtil.computeScalingDimension` (SMALL, MEDIUM or ORIGINAL by
  content length, never above MEDIUM) from `FileResourceDownloadCall`.
  `FileResourceDownloadParams` has no `dimension`, and
  `FileResourceService` takes a non-null `dimension`.
- The consuming apps call the two-factor login with four arguments from Java
  (`UserManagerImpl.java`: `logIn(username, password, serverUrl, code)`) and
  from Kotlin (`LoginRepositoryImpl.kt`: `blockingLogIn(..., code)`).
- `feature/upgrade_1.15.0_temp` starts from
  `feature-oca/synced-data-retention-purge`, which contains all of
  `develop-eyeseetea` plus the OCA customizations not yet merged there.

## Goals / Non-Goals

**Goals:**
- Keep each surviving customization as a small, titled delta on top of Oslo
  `1.15.0`, preferring appended blocks and EyeSeeTea-owned files over inline
  edits.
- Keep the four-argument two-factor login compiling for Java and Kotlin
  callers.
- Make the configured image dimension effective again, with the behaviour in
  this change's `original-size-image-download` delta.

**Non-Goals:**
- Adopting Oslo's new features in the apps (PIN login, rx/suspend API,
  OAuth2 changes).
- Reworking customizations beyond what the merge requires, except Original
  Size Image Download.
- Publishing or opening the PR from the temp branch (see "Branch").

## Decisions

### D1. Branch: explore on `feature/upgrade_1.15.0_temp`

The merge is first done on the local temp branch to see what it really
involves. The runbook branch (`feature/upgrade_1.15.0` from
`develop-eyeseetea`) is created once the OCA work is in `develop-eyeseetea`,
or the temp branch is renamed if that is where it lands (Open Questions).
Alternative: start from `develop-eyeseetea` now. Rejected because it lacks
the four OCA customizations and the documentation baseline built on this
branch.

### D2. Two-factor login: appended four-argument overloads, Oslo's three-argument API untouched

`UserModule` keeps Oslo's declarations as they are. An appended block adds
four-argument overloads with `twoFactorCode: String?` for `logIn`
(deprecated like Oslo's), `rxLogIn`, `blockingLogIn` and `suspendLogIn`,
implemented in an appended block of `UserModuleImpl`. `LogInCall.logIn` gets
a trailing `twoFactorCode: String? = null`, so Oslo's own three-argument path
reaches the same flow with no code.

Why: Java callers cannot use Kotlin default arguments, so the four-argument
form must be a real overload. Leaving Oslo's three-argument API untouched
removes the conflicts in the five real-server integration tests and most of
`LogInCallUnitShould.kt`, and keeps `core.api` additive.
Alternatives: a default `twoFactorCode` on Oslo's methods (Java callers
break, `UserModule.kt` keeps conflicting); keeping the fork's replacement of
the three-argument signature (largest conflict surface, breaks Oslo's tests).
Technique: new, "Appended overload over an Oslo public API"; add it to
`customization-techniques.md` as T4.

`LogInCall` is resolved `manual_reapply_on_theirs`: start from Oslo's flow
(PIN, authorization type) and re-insert the `/api/auth/login` request, the
two-factor status mapping, the basic-auth fallback and the Disabled Account
Login Handling branch. Credentials stored by the two-factor path MUST carry
`AuthorizationType.BASIC`, or `ParentAuthenticatorPlugin` stops sending them.
The plugin's own customization (`auth/login` counts as a login call) is
re-checked against Oslo's `when` on the authorization type.

### D3. Original Size Image Download: override Oslo's selection at its single call site

`D2Configuration` (Kotlin) gains `fileResizerDimension: FileResizerHelper.Dimension?`
with no default in `builder()`. `FileResourceDownloadCall` computes the
dimension through one EyeSeeTea function, kept in the owned
`GetDimension.kt`: for images, the configured dimension's name when set,
otherwise Oslo's `computeScalingDimension` result; for non-images, always
Oslo's result. `ORIGINAL` is sent as Oslo's `ORIGINAL` dimension name, so
`FileResourceService.kt` and `FileResourceDownloadParams.kt` are resolved
`accept_theirs`, and the old "no dimension" path (`getDimension` returning
`null`) is removed with its test.

How the configuration reaches the call: through `FileResourceDIModule`
as today, or by injecting `D2Configuration` into the call if Koin's compile
safety (ANDROSDK-2241) makes the module provider awkward. Decided at
implementation, no behaviour difference.

Why: one inline line in an Oslo file, the logic in an owned file, and two
fewer conflicting Oslo files. A configured dimension replaces Oslo's
selection entirely for images, including its cap at MEDIUM and its
`maxContentLength` check, because the app asked for that size explicitly.
Alternatives: keep a nullable `dimension` in `FileResourceService` (Oslo edit
kept only to send no parameter); change `computeScalingDimension` itself
(inline edit inside Oslo's selection logic).

### D4. Java to Kotlin moves

Follow "File migration rule: Java to Kotlin" in `conflict-rules.md`: accept
the deletion of `D2Configuration.java`, `DataElement.java`,
`D2ErrorCode.java` and the deleted Java tests, and re-add the customization
to the Kotlin replacement:
- `DataElement.kt`: `val url: String?` plus its accessor, with
  `DataElementDB.kt` / `DataElementFields.kt` re-checked (URL Data Element
  Field).
- `D2ErrorCode.kt`: the two-factor and disabled-account codes appended at the
  end of the enum.
- `D2Configuration.kt`: see D3.

### D5. Classification of the remaining files

| File | Category |
|---|---|
| `core/api/core.api` | `accept_theirs`, then regenerate with the API dump task |
| `CLAUDE.md` | `accept_theirs` plus the single `@AGENTS-eyeseetea.md` import line |
| `.github/workflows/ci.yml`, `core/plugins/android-pmd.gradle`, `OpenIDConnectHandlerImpl.kt`, `SystemInfoSamples.kt`, `CreateEventUtils.java`, `LoginErrorHandlingRealIntegrationShould.java` | `accept_theirs` (fork changes are drift or follow `logIn` signature) |
| `gradle/libs.versions.toml` | `accept_theirs`, then `dhis2AndroidSdkVersion = 1.15.0-eyeseetea-fork-1` |
| `AccountManagerImpl.kt` (Automatic Logout) | `manual_reapply_on_theirs`: Oslo's constructor plus `ConnectLogoutHandler` |
| `DatabaseConfigurationHelper.kt` (Change Server URL) | `manual_reapply_on_theirs`: reuse the existing account, building new ones with Oslo's `authorizationType` |
| `D2DIComponentFactory.kt` (retention) | `manual_reapply_on_theirs`: Oslo's module list plus `RetentionDIModule`, made Koin-compile-safe if needed |
| `DatabaseEncryptionPasswordManager.kt`, `TrackedEntityInstanceQueryDataFetcher.kt` (Oslo fixes) | `defer_after_build_verification`: keep only if `1.15.0` still has the bug |
| other auto-merged both-sides files | read the two-dot diff; restore Oslo for no-op drift |

## Risks / Trade-offs

- [Two-factor credentials without `AuthorizationType.BASIC` stop being sent
  after login] → unit test on the stored credentials, plus the manual 2FA
  login in the validation checklist.
- [Git auto-merge silently drops or duplicates customization code in the 27
  both-sides files] → two-dot diff of every file in `customization-files.md`
  against `origin/upstream/1.15.0`.
- [A configured dimension bypasses Oslo's `maxContentLength` cap for images]
  → accepted: the app opts in explicitly; recorded in the spec scenarios and
  the release notes for WIDP and SPIP.
- [Apps rely on Oslo `1.15.0` API changes beyond login] → build every
  consumer branch against the local SDK before publishing.
- [`RetentionDIModule` fails Koin compile-time checks] → resolve during the
  build step; it is EyeSeeTea-owned, so the fix does not touch Oslo files.

## Migration Plan

1. Merge, resolve and verify on `feature/upgrade_1.15.0_temp` (tasks.md).
2. Publish `1.15.0-eyeseetea-fork-1` to JitPack only after the consumer apps
   build and the validation checklist passes.
3. Rollback: apps stay on `1.14.2-eyeseetea-fork-*` until they bump the
   version; nothing is forced on them.

## Open Questions

- Final branch for the PR. The temp branch stays local, with no push. If the
  OCA work is still not in `develop-eyeseetea` when the definitive upgrade is
  requested, decide then: most likely merge OCA into `develop-eyeseetea`
  first; otherwise repeat this process from `develop-eyeseetea`. Does not
  change the merge work done here.
