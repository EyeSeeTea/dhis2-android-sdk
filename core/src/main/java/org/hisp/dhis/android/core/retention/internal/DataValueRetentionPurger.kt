package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.koin.core.annotation.Singleton

@Singleton
internal class DataValueRetentionPurger(
    private val dataValueStore: DataValueStore,
    private val valueFileResourcePurger: ValueFileResourcePurger,
) : RetentionPurger {
    override suspend fun purge(limit: Int) {
        val eligible = dataValueStore.getDataValuesWithState(State.SYNCED)
            .sortedByDescending { it.lastUpdated() }

        val toPurge = eligible.drop(limit)

        toPurge.forEach {
            dataValueStore.deleteWhere(it)
            valueFileResourcePurger.purgeIfDataElementReferencesFile(it.dataElement(), it.value())
        }
    }
}
