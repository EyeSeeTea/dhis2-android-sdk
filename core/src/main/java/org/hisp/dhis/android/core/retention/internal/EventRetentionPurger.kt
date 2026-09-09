package org.hisp.dhis.android.core.retention.internal

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
    private val valueFileResourcePurger: ValueFileResourcePurger,
    private val relationshipEligibilityChecker: RelationshipEligibilityChecker,
    private val relationshipRetentionPurger: RelationshipRetentionPurger,
) : RetentionPurger {
    override suspend fun eligibleCandidates(): List<RetentionCandidate> {
        val teiLessSyncedWhereClause = WhereClauseBuilder()
            .appendIsNullValue(EventTableInfo.Columns.ENROLLMENT)
            .appendKeyStringValue(DataColumns.AGGREGATED_SYNC_STATE, State.SYNCED)
            .build()

        return eventStore.selectWhere(teiLessSyncedWhereClause)
            .filter { relationshipEligibilityChecker.isEligible(it.uid()) }
            .map {
                RetentionCandidate.ByProgramAndOrgUnit(
                    uid = it.uid(),
                    lastUpdated = it.lastUpdated(),
                    programUids = listOf(it.program()!!),
                    organisationUnitUid = it.organisationUnit()!!,
                )
            }
    }

    override suspend fun purge(uids: List<String>) {
        uids.forEach { eventUid ->
            trackedEntityDataValueStore.getForEvent(eventUid).forEach {
                trackedEntityDataValueStore.deleteWhere(it)
                valueFileResourcePurger.purgeIfDataElementReferencesFile(it.dataElement(), it.value())
            }
            noteStore.getForEvent(eventUid).forEach { noteStore.delete(it.uid()) }
            eventStore.delete(eventUid)
            relationshipRetentionPurger.purgeForEntity(eventUid)
        }
    }
}
