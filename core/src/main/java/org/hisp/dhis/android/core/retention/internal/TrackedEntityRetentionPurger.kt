package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.hisp.dhis.android.core.common.DataColumns
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.note.internal.NoteStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityDataValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityInstanceStore
import org.hisp.dhis.android.persistence.common.querybuilders.WhereClauseBuilder
import org.hisp.dhis.android.persistence.enrollment.EnrollmentTableInfo
import org.hisp.dhis.android.persistence.event.EventTableInfo
import org.koin.core.annotation.Singleton

@Singleton
internal class TrackedEntityRetentionPurger(
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore,
    private val trackedEntityAttributeValueStore: TrackedEntityAttributeValueStore,
    private val enrollmentStore: EnrollmentStore,
    private val noteStore: NoteStore,
    private val eventStore: EventStore,
    private val trackedEntityDataValueStore: TrackedEntityDataValueStore,
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

                val enrollmentsWhereClause = WhereClauseBuilder()
                    .appendKeyStringValue(EnrollmentTableInfo.Columns.TRACKED_ENTITY_INSTANCE, tei.uid())
                    .build()
                val enrollments = enrollmentStore.selectWhere(enrollmentsWhereClause)

                enrollments.forEach { enrollment ->
                    val eventsWhereClause = WhereClauseBuilder()
                        .appendKeyStringValue(EventTableInfo.Columns.ENROLLMENT, enrollment.uid())
                        .build()
                    val events = eventStore.selectWhere(eventsWhereClause)

                    events.forEach { event ->
                        trackedEntityDataValueStore.deleteByEvent(event.uid())
                        noteStore.getForEvent(event.uid()).forEach { noteStore.delete(it.uid()) }
                        eventStore.delete(event.uid())
                    }

                    noteStore.getForEnrollment(enrollment.uid()).forEach { noteStore.delete(it.uid()) }
                    enrollmentStore.delete(enrollment.uid())
                }

                trackedEntityInstanceStore.delete(tei.uid())
            }
        }
    }
}
