package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.common.DataColumns
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.persistence.common.querybuilders.WhereClauseBuilder
import org.koin.core.annotation.Singleton
import java.io.File

@Singleton
internal class FileResourceRetentionPurger(
    private val fileResourceStore: FileResourceStore,
) : RetentionPurger {
    override suspend fun purge(limit: Int) {
        val syncedWhereClause = WhereClauseBuilder()
            .appendKeyStringValue(DataColumns.SYNC_STATE, State.SYNCED)
            .build()

        val eligible = fileResourceStore.selectWhere(syncedWhereClause)
            .sortedByDescending { it.lastUpdated() }

        val toPurge = eligible.drop(limit)

        toPurge.forEach { fileResource ->
            fileResource.path()?.let { runCatching { File(it).delete() } }
            fileResourceStore.delete(fileResource.uid()!!)
        }
    }
}
