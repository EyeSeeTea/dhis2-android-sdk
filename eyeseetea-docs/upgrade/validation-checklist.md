# Validation Checklist

Validation flows per customization. Each section heading is the exact
`# <Title>` of the matching `openspec/specs/<capability>/spec.md`.

There is no UI in this repository, so validation is mostly the automated
suite. The manual checks cover what a test suite alone does not give enough
confidence on: real migrations, real encrypted databases, real account state,
and behavior only observable from the consuming app (build it against the
local SDK with the composite build described in `EyeSeeTea.md`).

Run after any Oslo upgrade, and after touching a customization directly.

## 0. Full automated suite (always first)

```bash
./gradlew :core:ktlintCheck :core:detekt
./gradlew :core:testDebugUnitTest
./gradlew :core:releaseApiCheck
```

If the automated suite fails, stop here; the checks below assume a green
suite. `connectedDebugAndroidTest` commands need a device or emulator.

## 1. Synced Data Retention Purge

Automated coverage:
```bash
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=org.hisp.dhis.android.core.retention.internal
```

Manual check (from the consuming app, OCA flavor):
- [ ] With more synced TEIs, events and data values than the configured
      limit, run the purge: the oldest synced records beyond the limit are
      gone, the rest remain.
- [ ] A record with pending changes (or a tree with any non-synced
      descendant) is never purged.

## 2. Synced Data Relationships Purge

Automated coverage: the `retention.internal` package run in section 1
(`RelationshipEligibilityCheckerIntegrationShould`,
`RelationshipRetentionPurgerIntegrationShould`).

Manual check: none required unless Oslo changes the relationship schema or
its FK cascade; then run the package above and confirm no relationship row
points at a purged entity.

## 3. Synced Data Retention Scope

Automated coverage:
```bash
./gradlew :core:testDebugUnitTest --tests "org.hisp.dhis.android.core.retention.internal.*"
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hisp.dhis.android.core.dataset.internal.DataSetElementStoreIntegrationShould
```

Manual check (from the consuming app, OCA flavor):
- [ ] With per-program limits configured in the Android Settings Web App,
      each program keeps its own limit; one program's excess does not consume
      another program's quota.
- [ ] Re-verify if Oslo changes `ProgramSetting`, `DataSetSetting`,
      `LimitScope` or the `DataSetDataElementLink` table.

## 4. Disabled Account Login Handling

Automated coverage:
```bash
./gradlew :core:testDebugUnitTest --tests "org.hisp.dhis.android.core.user.internal.LogInCallUnitShould"
```

Manual check (from the consuming app):
- [ ] Log in with a disabled account: the app shows the disabled-account
      error, not "bad credentials".
- [ ] With a previously stored account on the device, a rejected login does
      not delete that account or its unsynced data.

## 5. Two-Factor Authentication Login

Automated coverage:
```bash
./gradlew :core:testDebugUnitTest --tests "org.hisp.dhis.android.core.user.internal.LogInCallUnitShould"
./gradlew :core:testDebugUnitTest --tests "org.hisp.dhis.android.core.arch.api.authentication.internal.ParentAuthenticatorPluginShould"
```

Manual check (from the consuming app, against a 2.41+ server):
- [ ] A user without 2FA logs in.
- [ ] A TOTP user is asked for the code, a wrong code is rejected, the right
      code logs in.
- [ ] An email or SMS 2FA user gets the "code sent" answer, then logs in with
      the received code.
- [ ] A server older than 2.41 (no `/api/auth/login`) still logs in through
      the fallback.
- [ ] Offline login with a known account still works.

## 6. Automatic Logout On Unauthorized Session

Automated coverage: none in this repository beyond the login tests; the app
side is covered by WIDP's `OpenIdSessionTest`.

Manual check (from the consuming app, WIDP):
- [ ] Logged in with username/password, invalidate the session on the server
      (change the password or revoke the session) and trigger a sync: the app
      returns to login with "session expired".
- [ ] A wrong password at login shows the login error, not a logout.

## 7. Change Server URL

Manual check (from the consuming app, SPIP / Sports / WIDP):
- [ ] Change the server URL from the app to another address of the same
      instance: requests go to the new URL and local data is still there.
- [ ] Log out and log in again with the new URL: the same database opens.
- [ ] An account for another user on the device is not changed.

## 8. Original Size Image Download

Automated coverage:
```bash
./gradlew :core:testDebugUnitTest --tests "org.hisp.dhis.android.core.fileresource.internal.GetDimensionShould"
```

Manual check (from the consuming app, SPIP, or WIDP once it sets the
dimension):
- [ ] With `fileResizerDimension(ORIGINAL)`, sync a tracker image larger than
      the medium size: the stored file has the original resolution, also when
      it is above the sync settings' `fileMaxLengthBytes`.
- [ ] Without the setting, the same image is stored at the size Oslo selects
      (never above medium).
- [ ] A non-image file resource downloads the same with and without the
      setting.

## 9. URL Data Element Field

Manual check (from the consuming app, WIDP):
- [ ] After a metadata sync, an event form field whose data element has a
      `url` shows that URL under its description.
- [ ] Upgrade a device with existing data to the build under test: the app
      opens and data element URLs are still there (migrations `85.sql` and
      `175.sql`).

## Oslo bug fixes

Fixes listed in section 3 of `customization-files.md`. Their checks are
removed together with the fix when Oslo ships its own.

### Encryption key preserved on database rename

Automated coverage:
```bash
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hisp.dhis.android.core.configuration.internal.Migration301IntegrationShould
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hisp.dhis.android.core.configuration.internal.DatabaseConfigurationMigrationIntegrationShould
```

Manual check (only if the migration path itself changed):
- [ ] Install a build from before the DB-rename migration, log in with an
      **encrypted** account, sync some data.
- [ ] Update to the build under test without uninstalling.
- [ ] The app opens without `SQLiteNotADatabaseException` and the synced data
      is still there.
- [ ] A **non-encrypted** account migrates the same way.

### Follow the SQLite ALTER TABLE caveats (`164.sql`)

Manual check (only if migration ordering or scripts around it change):
- [ ] Run the full migration chain from an old schema version through 164 on
      a device/emulator and confirm the `Program`/`ProgramStage` `displayXXX`
      columns are renamed with data intact.

### Offline-first search keeps local results

Manual check (from the consuming app):
- [ ] Run an `OFFLINE_FIRST` TEI search with the server unreachable or
      failing: local results are shown, the app does not crash. Since Oslo
      `1.15.0` the online failure also reaches the app (capture app `3.5.0`
      shows it as an error row below the local results).

## Maintenance rule

When a customization survives an upgrade:
- keep its validation flow here, under its spec title
- keep its functional description in its spec
- keep its technical inventory in `customization-files.md`
