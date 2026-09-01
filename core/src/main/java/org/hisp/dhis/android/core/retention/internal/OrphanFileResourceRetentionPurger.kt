package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.common.DataColumns
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityDataValueStore
import org.hisp.dhis.android.persistence.common.querybuilders.WhereClauseBuilder
import org.hisp.dhis.android.persistence.datavalue.DataValueTableInfo
import org.hisp.dhis.android.persistence.fileresource.FileResourceTableInfo
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeValueTableInfo
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityDataValueTableInfo
import org.koin.core.annotation.Singleton
import java.io.File

@Singleton
internal class OrphanFileResourceRetentionPurger(
    private val fileResourceStore: FileResourceStore,
    private val dataValueStore: DataValueStore,
    private val trackedEntityAttributeValueStore: TrackedEntityAttributeValueStore,
    private val trackedEntityDataValueStore: TrackedEntityDataValueStore,
) : RetentionPurger {
    override suspend fun purge(limit: Int) {
        val referencedFileResourceUids = (
            dataValueStore.selectStringColumnsWhereClause(DataValueTableInfo.Columns.VALUE, "1") +
                trackedEntityAttributeValueStore.selectStringColumnsWhereClause(
                    TrackedEntityAttributeValueTableInfo.Columns.VALUE,
                    "1",
                ) +
                trackedEntityDataValueStore.selectStringColumnsWhereClause(
                    TrackedEntityDataValueTableInfo.Columns.VALUE,
                    "1",
                )
            ).toSet()

        val orphanWhereClause = WhereClauseBuilder()
            .appendKeyStringValue(DataColumns.SYNC_STATE, State.SYNCED)
            .appendNotInKeyStringValues(FileResourceTableInfo.Columns.UID, referencedFileResourceUids.toList())
            .build()

        val eligible = fileResourceStore.selectWhere(orphanWhereClause)
            .sortedByDescending { it.lastUpdated() }

        val toPurge = eligible.drop(limit)

        toPurge.forEach { fileResource ->
            fileResource.path()?.let { runCatching { File(it).delete() } }
            fileResourceStore.delete(fileResource.uid()!!)
        }
    }
}
