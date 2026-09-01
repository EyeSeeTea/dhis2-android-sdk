package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.common.DataColumns
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
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
) : RetentionPurger {
    override suspend fun purge(limit: Int) {
        val referencedFileResourcesSubQuery =
            "SELECT ${DataValueTableInfo.Columns.VALUE} FROM ${DataValueTableInfo.TABLE_INFO.name()} " +
                "UNION SELECT ${TrackedEntityAttributeValueTableInfo.Columns.VALUE} " +
                "FROM ${TrackedEntityAttributeValueTableInfo.TABLE_INFO.name()} " +
                "UNION SELECT ${TrackedEntityDataValueTableInfo.Columns.VALUE} " +
                "FROM ${TrackedEntityDataValueTableInfo.TABLE_INFO.name()}"

        val orphanWhereClause = WhereClauseBuilder()
            .appendKeyStringValue(DataColumns.SYNC_STATE, State.SYNCED)
            .appendNotInSubQuery(FileResourceTableInfo.Columns.UID, referencedFileResourcesSubQuery)
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
