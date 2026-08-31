package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.hisp.dhis.android.core.common.DataColumns
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.note.internal.NoteStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityDataValueStore
import org.hisp.dhis.android.persistence.common.querybuilders.WhereClauseBuilder
import org.hisp.dhis.android.persistence.event.EventTableInfo
import org.koin.core.annotation.Singleton

@Singleton
internal class EventRetentionPurger(
    private val eventStore: EventStore,
    private val trackedEntityDataValueStore: TrackedEntityDataValueStore,
    private val noteStore: NoteStore,
    private val d2CallExecutor: D2CallExecutorInterface,
) : RetentionPurger {
    override suspend fun purge(limit: Int) {
        d2CallExecutor.executeD2CallTransactionally {
            val teiLessSyncedWhereClause = WhereClauseBuilder()
                .appendIsNullValue(EventTableInfo.Columns.ENROLLMENT)
                .appendKeyStringValue(DataColumns.AGGREGATED_SYNC_STATE, State.SYNCED)
                .build()

            val eligible = eventStore.selectWhere(teiLessSyncedWhereClause)
                .sortedByDescending { it.lastUpdated() }

            val toPurge = eligible.drop(limit)

            toPurge.forEach { event ->
                trackedEntityDataValueStore.deleteByEvent(event.uid())
                noteStore.getForEvent(event.uid()).forEach { noteStore.delete(it.uid()) }
                eventStore.delete(event.uid())
            }
        }
    }
}
