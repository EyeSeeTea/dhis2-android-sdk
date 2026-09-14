package org.hisp.dhis.android.core.retention.internal

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
    private val valueFileResourcePurger: ValueFileResourcePurger,
    private val relationshipEligibilityChecker: RelationshipEligibilityChecker,
    private val relationshipRetentionPurger: RelationshipRetentionPurger,
) : RetentionPurger {
    override suspend fun eligibleCandidates(): List<RetentionCandidate> {
        val syncedWhereClause = WhereClauseBuilder()
            .appendKeyStringValue(DataColumns.AGGREGATED_SYNC_STATE, State.SYNCED)
            .build()

        return trackedEntityInstanceStore.selectWhere(syncedWhereClause)
            .filter { isTreeRelationshipEligible(it.uid()) }
            .map {
                RetentionCandidate.ByProgramAndOrgUnit(
                    uid = it.uid(),
                    lastUpdated = it.lastUpdated(),
                    programUids = enrollmentsOf(it.uid()).mapNotNull { enrollment -> enrollment.program() }.distinct(),
                    organisationUnitUid = it.organisationUnit()!!,
                )
            }
    }

    override suspend fun purge(uids: List<String>) {
        uids.forEach { teiUid ->
            trackedEntityAttributeValueStore.queryByTrackedEntityInstance(teiUid).forEach {
                trackedEntityAttributeValueStore.deleteWhere(it)
                valueFileResourcePurger.purgeIfAttributeReferencesFile(it.trackedEntityAttribute(), it.value())
            }

            enrollmentsOf(teiUid).forEach { enrollment ->
                val eventsWhereClause = WhereClauseBuilder()
                    .appendKeyStringValue(EventTableInfo.Columns.ENROLLMENT, enrollment.uid())
                    .build()
                val events = eventStore.selectWhere(eventsWhereClause)

                events.forEach { event ->
                    purgeEvent(event.uid())
                    relationshipRetentionPurger.purgeForEntity(event.uid())
                }

                noteStore.getForEnrollment(enrollment.uid()).forEach { noteStore.delete(it.uid()) }
                enrollmentStore.delete(enrollment.uid())
                relationshipRetentionPurger.purgeForEntity(enrollment.uid())
            }

            trackedEntityInstanceStore.delete(teiUid)
            relationshipRetentionPurger.purgeForEntity(teiUid)
        }
    }

    private suspend fun isTreeRelationshipEligible(teiUid: String): Boolean {
        if (!relationshipEligibilityChecker.isEligible(teiUid)) return false

        return enrollmentsOf(teiUid).all { enrollment ->
            relationshipEligibilityChecker.isEligible(enrollment.uid()) &&
                isEnrollmentsEventsRelationshipEligible(enrollment.uid())
        }
    }

    private suspend fun enrollmentsOf(teiUid: String) = enrollmentStore.selectWhere(
        WhereClauseBuilder()
            .appendKeyStringValue(EnrollmentTableInfo.Columns.TRACKED_ENTITY_INSTANCE, teiUid)
            .build(),
    )

    private suspend fun isEnrollmentsEventsRelationshipEligible(enrollmentUid: String): Boolean {
        val eventsWhereClause = WhereClauseBuilder()
            .appendKeyStringValue(EventTableInfo.Columns.ENROLLMENT, enrollmentUid)
            .build()

        return eventStore.selectWhere(eventsWhereClause)
            .all { relationshipEligibilityChecker.isEligible(it.uid()) }
    }

    private suspend fun purgeEvent(eventUid: String) {
        trackedEntityDataValueStore.getForEvent(eventUid).forEach {
            trackedEntityDataValueStore.deleteWhere(it)
            valueFileResourcePurger.purgeIfDataElementReferencesFile(it.dataElement(), it.value())
        }
        noteStore.getForEvent(eventUid).forEach { noteStore.delete(it.uid()) }
        eventStore.delete(eventUid)
    }
}
