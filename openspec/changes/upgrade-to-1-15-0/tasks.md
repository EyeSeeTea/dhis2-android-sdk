## 1. Start the merge (runbook phase 2)

- [x] 1.1 Fetch `upstream` tags and confirm `origin/upstream/1.15.0` points at `3ff01d8e93`; verify with `git rev-parse origin/upstream/1.15.0`
- [x] 1.2 On `feature/upgrade_1.15.0_temp`, run `git merge origin/upstream/1.15.0` and check that the conflicted files match the 26 listed in proposal.md; record any difference in `eyeseetea-docs/upgrade/upgrade-1.15.0-notes.md`

Commit: none yet. Groups 2 to 5 are resolved inside the single merge commit
that task 5.4 creates (git cannot commit a merge in parts).

## 2. Classify and resolve the easy batch (runbook phases 3 and 4)

- [x] 2.1 Fill the file classification table of `upgrade-1.15.0-notes.md` (file, category, expected delta, customization) for every conflicted file and every file in `customization-files.md`, using design.md D5; verify every inventory file has a row
- [x] 2.2 Resolve `accept_theirs` files: `core/api/core.api`, `.github/workflows/ci.yml`, `core/plugins/android-pmd.gradle` (accept deletion), `OpenIDConnectHandlerImpl.kt`, `SystemInfoSamples.kt`, `CreateEventUtils.java` and `LoginErrorHandlingRealIntegrationShould.java` (accept deletion); verify `git diff origin/upstream/1.15.0 -- <file>` is empty for each
- [x] 2.3 Resolve `CLAUDE.md` as Oslo's file plus the `@AGENTS-eyeseetea.md` import line; verify the two-dot diff is that one line
- [x] 2.4 Resolve `gradle/libs.versions.toml` as Oslo's and set `dhis2AndroidSdkVersion = "1.15.0-eyeseetea-fork-1"`; verify the two-dot diff is that line
- [x] 2.5 Accept Oslo's version of the five real-server integration tests renamed to Kotlin (`LogInCallRealIntegrationShould`, `MultiUserRealIntegrationShould`, `DataSetCompleteRegistrationCallRealIntegrationShould`, `EventDownloadRealIntegrationShould`, `TrackedEntityInstanceCallRealIntegrationShould`); verify their two-dot diff is empty (D2 keeps the three-argument `logIn`)

## 3. Re-apply customizations in conflicted files (runbook phase 5)

- [x] 3.1 Two-Factor Authentication Login, `UserModule.kt` / `UserModuleImpl.kt`: Oslo's file plus an appended block of four-argument overloads (`logIn` deprecated, `rxLogIn`, `blockingLogIn`, `suspendLogIn`) per design.md D2; verify the two-dot diff only adds the titled block
- [x] 3.2 Two-Factor Authentication Login and Disabled Account Login Handling, `LogInCall.kt`: start from Oslo's flow and re-insert the `/api/auth/login` request, two-factor status mapping, basic-auth fallback and disabled-account branch, with `twoFactorCode: String? = null` and credentials stored as `AuthorizationType.BASIC`; verify every inserted block carries its titled comment
- [x] 3.3 `D2ErrorCode.kt`: append the two-factor and disabled-account codes from `D2ErrorCode.java` at the end of the enum and accept the Java deletion; verify each code exists once
- [x] 3.4 `ParentAuthenticatorPlugin.kt` (auto-merged): check the `auth/login` login-call customization against Oslo's `when` on `authorizationType`; verify the two-dot diff is only the titled block
- [x] 3.5 `LogInCallUnitShould.kt` and `ParentAuthenticatorPluginShould.kt`: start from Oslo's tests and re-add the fork's two-factor and disabled-account cases; verify with `./gradlew :core:testDebugUnitTest --tests "*LogInCallUnitShould" --tests "*ParentAuthenticatorPluginShould"` after task 5.2
- [x] 3.6 Automatic Logout On Unauthorized Session, `AccountManagerImpl.kt`: Oslo's constructor plus `connectLogoutHandler`; verify `logOutObservable()` still delegates to `ConnectLogoutHandler`
- [x] 3.7 Change Server URL, `DatabaseConfigurationHelper.kt`: re-use an existing account, building new ones with Oslo's `authorizationType`; verify the two-dot diff is only the titled block
- [x] 3.8 Synced Data retention, `D2DIComponentFactory.kt`: Oslo's module list plus `RetentionDIModule`; verify the two-dot diff is the import and the module line
- [x] 3.9 URL Data Element Field: accept the deletion of `DataElement.java` and add `url` to `DataElement.kt`; re-check `DataElementDB.kt`, `DataElementFields.kt`, `85.sql` and `175.sql`; verify `DataElement.url()` and the `url` column are present
- [x] 3.10 Original Size Image Download, merge part: accept the deletion of `D2Configuration.java` and add `fileResizerDimension` to `D2Configuration.kt` with no default; resolve `FileResourceService.kt` and `FileResourceDownloadParams.kt` as `accept_theirs` (design.md D3); verify their two-dot diffs are empty

## 4. Verify the auto-merged files

- [x] 4.1 Two-dot diff every file of `customization-files.md` against `origin/upstream/1.15.0` and compare with its expected delta; verify no customization block is lost or duplicated
- [x] 4.2 Read the two-dot diff of the remaining both-sides files from proposal.md; restore Oslo's version where the difference is no-op drift (section 5 of `customization-files.md`); verify section 5 is empty
- [x] 4.3 Oslo fixes: check whether `1.15.0` still has the bugs behind "Encryption key preserved on database rename", "Follow the SQLite ALTER TABLE caveats" and "Offline-first search keeps local results" (Oslo partially reverted the last one); keep or retire each, and verify the decision is in the notes

## 5. Build and commit the merge

- [x] 5.1 `./gradlew :core:assembleDebug` succeeds; fix compile errors in the resolved files only (including Koin compile safety for `RetentionDIModule`)
- [x] 5.2 `./gradlew :core:testDebugUnitTest` passes, apart from tests that task 6 rewrites; list any such test in the notes
- [x] 5.3 Regenerate the public API dump (`./gradlew :core:releaseApiDump` or the task available in `1.15.0`); verify the `core.api` diff against Oslo only shows the customizations' public additions
- [x] 5.4 Commit the merge (`Merge upstream/1.15.0 into ...`); verify `git status` is clean

Commit: one merge commit for groups 2 to 5.

## 6. Original Size Image Download re-wire (after the merge)

- [ ] 6.1 Test, in `core/src/test/`, the dimension resolution for images and non-images: no configured dimension returns Oslo's selection, a configured SMALL/MEDIUM/LARGE/ORIGINAL returns that name even above MEDIUM, a non-image ignores the configuration (spec delta scenarios); verify it fails before 6.2
- [ ] 6.2 Implement the resolution in `GetDimension.kt` and call it from `FileResourceDownloadCall` instead of `computeScalingDimension`, wiring the configured dimension per design.md D3; remove the old "no dimension" path and its test; verify 6.1 and `FileResourceDownloaderShould` pass

Commit: 6.1 and 6.2 together (red → green).

## 7. Validate (runbook phase 6)

- [ ] 7.1 `./gradlew :core:ktlintCheck :core:detekt` and `./gradlew :core:testDebugUnitTest` pass
- [ ] 7.2 Run the targeted `androidTest` classes of `validation-checklist.md` on an emulator; verify they pass or record why one cannot run
- [ ] 7.3 Build the consumer apps against the local SDK (composite build in `EyeSeeTea.md`): `develop-eyeseetea`, `develop-widp`, `develop-oca`, `develop-spip`, `develop-sports`; verify each compiles or record the app-side change it needs
- [ ] 7.4 Work through the manual checks of `validation-checklist.md`, including a two-factor login and an original-size image download; verify each item is ticked or has a note

Commit: only if 7.1 or 7.2 need fixes; one commit per fix.

## 8. Documentation and finalization (runbook phase 7)

- [ ] 8.1 Update `customization-files.md`: Kotlin paths for the moved files, Original Size Image Download back to `active` with its new implementation points, Oslo fixes kept or retired, header baseline `1.15.0`; verify with `python3 eyeseetea-docs/scripts/check_upgrade_docs.py`
- [ ] 8.2 Remove the "broken" status note from the Purpose of `openspec/specs/original-size-image-download/spec.md`, set its status to `active` in `AGENTS-eyeseetea.md`, and update `validation-checklist.md` for the new behaviour; verify `openspec validate --specs`
- [ ] 8.3 Add technique T4 "Appended overload over an Oslo public API" to `customization-techniques.md` (design.md D2)
- [ ] 8.4 Before any push, resolve the branch question of design.md: if OCA is still not in `develop-eyeseetea`, merge OCA there first or repeat this process from `develop-eyeseetea`

Commit: 8.1 to 8.3 as one documentation commit. Archiving the change
(`/opsx:archive`) and deleting `upgrade-1.15.0-notes.md` happen after the PR
is merged.
