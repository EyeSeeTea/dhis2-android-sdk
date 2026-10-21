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
./gradlew :core:apiCheck            # Oslo 1.15.0+: :core:releaseApiCheck
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

## Pending a spec

Confirmed customizations listed in section 3 of `customization-files.md`
that still need a spec. Their checks move to a titled section above once the
spec exists.

### Encryption key preserved when a renamed database is opened

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

### `ALTER TABLE` migration script fix (`164.sql`)

Manual check (only if migration ordering or scripts around it change):
- [ ] Run the full migration chain from an old schema version through 164 on
      a device/emulator and confirm the `Program`/`ProgramStage` `displayXXX`
      columns are renamed with data intact.

## Maintenance rule

When a customization survives an upgrade:
- keep its validation flow here, under its spec title
- keep its functional description in its spec
- keep its technical inventory in `customization-files.md`
