# Validation Checklist

Manual validation flows for the customizations in `customizations.md`.

There is no UI in this repository — validation is mostly the automated
suite. This checklist exists for the parts a test suite alone does not give
enough confidence on: real migrations, real encrypted databases, real
account state, and behavior only observable from the consuming app.

Run after any Oslo upgrade, and after touching a customization directly.

## 0. Full automated suite (baseline, always run first)

```bash
./gradlew ktlintCheck
./gradlew testDebugUnitTest testDhis2DebugUnitTest testAndroidHostTest
```

Targeted `androidTest` per touched module — see `AGENTS.md` for the
per-module task naming. If the automated suite fails, stop here; the manual
checks below assume a green suite.

## 1. Encryption key preserved when a renamed database is opened

Covers: `core/src/main/java/org/hisp/dhis/android/core/configuration/internal/migration/Migration301.kt`,
`DatabaseEncryptionPasswordManager.kt`.

Automated coverage:
```bash
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hisp.dhis.android.core.configuration.internal.Migration301IntegrationShould
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hisp.dhis.android.core.configuration.internal.DatabaseConfigurationMigrationIntegrationShould
```

Manual check (do this if the migration path itself changed, not just
unrelated code near it):
- [ ] Install a build from before this fork's DB-rename migration on a real
      device/emulator, log in with an **encrypted** account, sync some
      data.
- [ ] Update to the build under test (containing `Migration301`) without
      uninstalling.
- [ ] Confirm the app opens without `SQLiteNotADatabaseException` and the
      previously synced data is still there.
- [ ] Confirm a **non-encrypted** account migrates the same way with no
      regression (the fix must not affect the non-encrypted path).

## 2. Dataset resolution for a `DataValue`'s data element

Covers: `core/src/main/java/org/hisp/dhis/android/core/dataset/internal/DataSetElementStore.kt`
(`getDataSetsForDataElement`), `DataSetDataElementLinkStoreImpl.kt`.

Automated coverage:
```bash
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=org.hisp.dhis.android.core.dataset.internal.DataSetElementStoreIntegrationShould
./gradlew :core:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=org.hisp.dhis.android.core.retention.internal
```

Manual check: none required — this is pure Room-query logic fully exercised
by `DataSetElementStoreIntegrationShould` and the retention purger
integration tests. Only re-verify manually if the underlying
`DataSetDataElementLink` table schema changes upstream.

## 3. `ALTER TABLE` migration script fix (`164.sql`)

Covers: `core/src/main/assets/migrations/164.sql`.

Manual check (only if migration ordering/scripts around it change):
- [ ] Run the full migration chain from an old schema version through 164
      on a real device/emulator (not just the migration unit tests) and
      confirm `Program`/`ProgramStage` `displayXXX` columns are correctly
      renamed with data intact.

## 4. Candidates pending confirmation (`customizations.md` section 3)

Do not add a manual check here until the candidate is confirmed as an
intentional, still-needed customization and moved to `customizations.md`
section 1. Once confirmed, add its validation flow to this file following
the same shape as sections 1-3 above.
