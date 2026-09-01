package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.koin.core.annotation.Singleton

@Singleton
internal class SyncedDataRetentionPurger(
    private val dataValuePurger: RetentionPurger,
    private val trackedEntityPurger: RetentionPurger,
    private val eventPurger: RetentionPurger,
    private val fileResourcePurger: RetentionPurger,
    private val d2CallExecutor: D2CallExecutorInterface,
) {
    suspend fun purge(limits: RetentionLimits) {
        d2CallExecutor.executeD2CallTransactionally {
            dataValuePurger.purge(limits.dataValue)
            trackedEntityPurger.purge(limits.trackedEntityInstance)
            eventPurger.purge(limits.event)
            fileResourcePurger.purge(limits.fileResource)
        }
    }
}
