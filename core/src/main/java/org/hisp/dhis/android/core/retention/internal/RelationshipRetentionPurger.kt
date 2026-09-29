package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.relationship.internal.RelationshipItemStore
import org.hisp.dhis.android.core.relationship.internal.RelationshipStore
import org.koin.core.annotation.Singleton

@Singleton
internal class RelationshipRetentionPurger(
    private val relationshipStore: RelationshipStore,
    private val relationshipItemStore: RelationshipItemStore,
) {
    suspend fun purgeForEntity(entityUid: String) {
        relationshipItemStore.getByEntityUid(entityUid).forEach { item ->
            val relationshipUid = item.relationship()?.uid() ?: return@forEach

            relationshipItemStore.getForRelationshipUid(relationshipUid).forEach {
                relationshipItemStore.deleteWhereIfExists(it)
            }
            relationshipStore.deleteIfExists(relationshipUid)
        }
    }
}
