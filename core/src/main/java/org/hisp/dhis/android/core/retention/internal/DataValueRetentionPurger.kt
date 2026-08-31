package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.koin.core.annotation.Singleton

@Singleton
internal class DataValueRetentionPurger(
    private val dataValueStore: DataValueStore,
    private val d2CallExecutor: D2CallExecutorInterface,
) {
    suspend fun purge(limit: Int) {
        d2CallExecutor.executeD2CallTransactionally {
            val eligible = dataValueStore.getDataValuesWithState(State.SYNCED)
                .sortedByDescending { it.lastUpdated() }

            val toPurge = eligible.drop(limit)

            toPurge.forEach { dataValueStore.deleteWhere(it) }
        }
    }
}
