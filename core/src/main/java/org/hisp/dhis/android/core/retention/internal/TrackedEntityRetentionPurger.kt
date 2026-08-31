package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.hisp.dhis.android.core.common.DataColumns
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityInstanceStore
import org.hisp.dhis.android.persistence.common.querybuilders.WhereClauseBuilder
import org.koin.core.annotation.Singleton

@Singleton
internal class TrackedEntityRetentionPurger(
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore,
    private val trackedEntityAttributeValueStore: TrackedEntityAttributeValueStore,
    private val d2CallExecutor: D2CallExecutorInterface,
) : RetentionPurger {
    override suspend fun purge(limit: Int) {
        d2CallExecutor.executeD2CallTransactionally {
            val syncedWhereClause = WhereClauseBuilder()
                .appendKeyStringValue(DataColumns.AGGREGATED_SYNC_STATE, State.SYNCED)
                .build()

            val eligible = trackedEntityInstanceStore.selectWhere(syncedWhereClause)
                .sortedByDescending { it.lastUpdated() }

            val toPurge = eligible.drop(limit)

            toPurge.forEach { tei ->
                trackedEntityAttributeValueStore.queryByTrackedEntityInstance(tei.uid())
                    .forEach { trackedEntityAttributeValueStore.deleteWhere(it) }
                trackedEntityInstanceStore.delete(tei.uid())
            }
        }
    }
}
