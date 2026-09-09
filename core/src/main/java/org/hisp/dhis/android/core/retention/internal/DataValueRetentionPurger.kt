package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.dataset.internal.DataSetElementStore
import org.hisp.dhis.android.core.datavalue.DataValue
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.koin.core.annotation.Singleton

@Singleton
internal class DataValueRetentionPurger(
    private val dataValueStore: DataValueStore,
    private val dataSetElementStore: DataSetElementStore,
    private val valueFileResourcePurger: ValueFileResourcePurger,
) : RetentionPurger {
    override suspend fun eligibleCandidates(): List<RetentionCandidate> {
        val eligible = dataValueStore.getDataValuesWithState(State.SYNCED)
        val dataSetUidsByDataElement = eligible.mapNotNull { it.dataElement() }.distinct()
            .associateWith { dataSetElementStore.getDataSetsForDataElement(it) }

        return eligible.mapNotNull {
            val dataSetUids = dataSetUidsByDataElement.getValue(it.dataElement()!!)
            if (dataSetUids.isEmpty()) {
                null
            } else {
                RetentionCandidate.ByDataset(
                    uid = dataValueUid(it),
                    lastUpdated = it.lastUpdated(),
                    dataSetUids = dataSetUids,
                )
            }
        }
    }

    override suspend fun purge(uids: List<String>) {
        val byUid = dataValueStore.getDataValuesWithState(State.SYNCED).associateBy { dataValueUid(it) }

        uids.forEach { uid ->
            byUid[uid]?.let {
                dataValueStore.deleteWhere(it)
                valueFileResourcePurger.purgeIfDataElementReferencesFile(it.dataElement(), it.value())
            }
        }
    }

    private fun dataValueUid(dataValue: DataValue): String = listOf(
        dataValue.dataElement(),
        dataValue.period(),
        dataValue.organisationUnit(),
        dataValue.categoryOptionCombo(),
        dataValue.attributeOptionCombo(),
    ).joinToString("_")
}
