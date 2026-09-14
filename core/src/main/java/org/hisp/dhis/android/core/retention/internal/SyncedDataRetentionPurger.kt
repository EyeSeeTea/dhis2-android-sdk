package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.hisp.dhis.android.core.maintenance.D2Error
import org.hisp.dhis.android.core.maintenance.D2ErrorCode
import org.hisp.dhis.android.core.maintenance.D2ErrorComponent
import org.hisp.dhis.android.core.settings.LimitScope
import org.hisp.dhis.android.core.settings.ProgramSetting
import org.koin.core.annotation.Singleton

@Singleton
internal class SyncedDataRetentionPurger(
    private val dataValuePurger: DataValueRetentionPurger,
    private val trackedEntityPurger: TrackedEntityRetentionPurger,
    private val eventPurger: EventRetentionPurger,
    private val orphanFileResourcePurger: OrphanFileResourceRetentionPurger,
    private val programRetentionLimitResolver: ProgramRetentionLimitResolver,
    private val dataSetRetentionLimitResolver: DataSetRetentionLimitResolver,
    private val retentionSelector: RetentionSelector,
    private val d2CallExecutor: D2CallExecutorInterface,
) {
    suspend fun purge() {
        d2CallExecutor.executeD2CallTransactionally {
            try {
                purgeTrackedEntityInstances()
                purgeEvents()
                purgeDataValues()
                orphanFileResourcePurger.purge(NO_ORPHANS_ALLOWED)
            } catch (d2Error: D2Error) {
                throw d2Error
            } catch (e: Exception) {
                // D2CallExecutor's generic handler discards the original exception's
                // message ("Unexpected error calling ..."). Wrap it here, where the real
                // failure is known, so callers (and the sync error log) get something
                // actionable instead of a description with no diagnostic value.
                throw D2Error.builder()
                    .errorComponent(D2ErrorComponent.SDK)
                    .errorCode(D2ErrorCode.UNEXPECTED)
                    .errorDescription("Retention purge failed: ${e.message}")
                    .originalException(e)
                    .build()
            }
        }
    }

    private companion object {
        // An orphan file resource has no owning record, so there is no retention setting to
        // apply — every orphan is eligible.
        const val NO_ORPHANS_ALLOWED = 0
    }

    private suspend fun purgeTrackedEntityInstances() {
        purgeByProgramAndOrgUnit(
            purger = trackedEntityPurger,
            limitExtractor = { it.teiDBTrimming() },
        )
    }

    private suspend fun purgeEvents() {
        purgeByProgramAndOrgUnit(
            purger = eventPurger,
            limitExtractor = { it.eventsDBTrimming() },
        )
    }

    private suspend fun purgeByProgramAndOrgUnit(
        purger: RetentionPurger,
        limitExtractor: (ProgramSetting) -> Int?,
    ) {
        val candidates = purger.eligibleCandidates().filterIsInstance<RetentionCandidate.ByProgramAndOrgUnit>()
        if (candidates.isEmpty()) return

        val programUids = candidates.flatMap { it.programUids }.distinct()
        val resolvedByProgram = programUids.associateWith { programRetentionLimitResolver.resolve(it, limitExtractor) }
        val mostRestrictive = resolvedByProgram.values.minBy { it.limit }

        val toPurge = when (mostRestrictive.scope) {
            LimitScope.GLOBAL ->
                retentionSelector.select(candidates, mostRestrictive.limit)

            LimitScope.PER_PROGRAM -> {
                val limitByProgram = resolvedByProgram.mapValues { it.value.limit }
                retentionSelector.selectByProgram(candidates, limitByProgram)
            }

            LimitScope.PER_ORG_UNIT, LimitScope.ALL_ORG_UNITS -> {
                val limitByOrgUnit = candidates.map { it.organisationUnitUid }.distinct()
                    .associateWith { mostRestrictive.limit }
                retentionSelector.selectByOrgUnit(candidates, limitByOrgUnit)
            }

            LimitScope.PER_OU_AND_PROGRAM -> {
                val limitByOrgUnitAndProgram = candidates
                    .flatMap { candidate -> candidate.programUids.map { candidate.organisationUnitUid to it } }
                    .distinct()
                    .associateWith { (_, programUid) -> resolvedByProgram.getValue(programUid).limit }
                retentionSelector.selectByOrgUnitAndProgram(candidates, limitByOrgUnitAndProgram)
            }
        }

        purger.purge(toPurge)
    }

    private suspend fun purgeDataValues() {
        val candidates = dataValuePurger.eligibleCandidates().filterIsInstance<RetentionCandidate.ByDataset>()
        if (candidates.isEmpty()) return

        val dataSetUids = candidates.flatMap { it.dataSetUids }.distinct()
        val limitByDataset = dataSetUids.associateWith { dataSetRetentionLimitResolver.resolve(it) }

        val toPurge = retentionSelector.selectByDataset(candidates, limitByDataset)

        dataValuePurger.purge(toPurge)
    }
}
