package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.relationship.internal.RelationshipItemStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityInstanceStore
import org.hisp.dhis.android.persistence.relationship.RelationshipItemTableInfo
import org.koin.core.annotation.Singleton

@Singleton
internal class RelationshipEligibilityChecker(
    private val relationshipItemStore: RelationshipItemStore,
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore,
    private val enrollmentStore: EnrollmentStore,
    private val eventStore: EventStore,
) {
    suspend fun isEligible(entityUid: String): Boolean {
        return relationshipItemStore.getByEntityUid(entityUid).all { item ->
            val relationshipUid = item.relationship()?.uid() ?: return@all true

            relationshipItemStore.getForRelationshipUid(relationshipUid)
                .filterNot { it.elementUid() == entityUid }
                .all { counterpart -> isSynced(counterpart.elementType(), counterpart.elementUid()) }
        }
    }

    private suspend fun isSynced(elementType: String?, elementUid: String?): Boolean {
        elementUid ?: return true

        val aggregatedSyncState = when (elementType) {
            RelationshipItemTableInfo.Columns.TRACKED_ENTITY_INSTANCE ->
                trackedEntityInstanceStore.selectByUid(elementUid)?.aggregatedSyncState()
            RelationshipItemTableInfo.Columns.ENROLLMENT ->
                enrollmentStore.selectByUid(elementUid)?.aggregatedSyncState()
            RelationshipItemTableInfo.Columns.EVENT ->
                eventStore.selectByUid(elementUid)?.aggregatedSyncState()
            else -> null
        }

        return aggregatedSyncState == null || aggregatedSyncState == State.SYNCED
    }
}
