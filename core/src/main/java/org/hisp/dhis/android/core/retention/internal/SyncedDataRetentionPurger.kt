package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.koin.core.annotation.Singleton

@Singleton
internal class SyncedDataRetentionPurger(
    private val dataValuePurger: RetentionPurger,
    private val trackedEntityPurger: RetentionPurger,
    private val eventPurger: RetentionPurger,
    private val orphanFileResourcePurger: OrphanFileResourceRetentionPurger,
    private val retentionSelector: RetentionSelector,
    private val d2CallExecutor: D2CallExecutorInterface,
) {
    suspend fun purge(limits: RetentionLimits) {
        d2CallExecutor.executeD2CallTransactionally {
            purgeWithLimit(dataValuePurger, limits.dataValue)
            purgeWithLimit(trackedEntityPurger, limits.trackedEntityInstance)
            purgeWithLimit(eventPurger, limits.event)
            orphanFileResourcePurger.purge(limits.fileResource)
        }
    }

    private suspend fun purgeWithLimit(purger: RetentionPurger, limit: Int) {
        val candidates = purger.eligibleCandidates()
        val toPurge = retentionSelector.select(candidates, limit)
        purger.purge(toPurge)
    }
}
