package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.internal.DataElementStore
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeStore
import org.hisp.dhis.android.persistence.common.querybuilders.WhereClauseBuilder
import org.hisp.dhis.android.persistence.dataelement.DataElementTableInfo
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeTableInfo
import org.koin.core.annotation.Singleton
import java.io.File

@Singleton
internal class ValueFileResourcePurger(
    private val dataElementStore: DataElementStore,
    private val trackedEntityAttributeStore: TrackedEntityAttributeStore,
    private val fileResourceStore: FileResourceStore,
) {
    private var fileDataElementUids: Set<String>? = null
    private var fileAttributeUids: Set<String>? = null

    suspend fun purgeIfDataElementReferencesFile(dataElementUid: String?, value: String?) =
        purgeIfFieldReferencesFile(dataElementUid, value, ::fileDataElementUids)

    suspend fun purgeIfAttributeReferencesFile(attributeUid: String?, value: String?) =
        purgeIfFieldReferencesFile(attributeUid, value, ::fileAttributeUids)

    private suspend fun purgeIfFieldReferencesFile(
        fieldUid: String?,
        value: String?,
        fileFieldUids: suspend () -> Set<String>,
    ) {
        if (fieldUid != null && fieldUid in fileFieldUids()) {
            purgeFileResource(value)
        }
    }

    private suspend fun fileDataElementUids(): Set<String> {
        return fileDataElementUids ?: dataElementStore.selectUidsWhere(
            WhereClauseBuilder()
                .appendInKeyEnumValues(DataElementTableInfo.Columns.VALUE_TYPE, fileValueTypes)
                .build(),
        ).toSet().also { fileDataElementUids = it }
    }

    private suspend fun fileAttributeUids(): Set<String> {
        return fileAttributeUids ?: trackedEntityAttributeStore.selectUidsWhere(
            WhereClauseBuilder()
                .appendInKeyEnumValues(TrackedEntityAttributeTableInfo.Columns.VALUE_TYPE, fileValueTypes)
                .build(),
        ).toSet().also { fileAttributeUids = it }
    }

    private suspend fun purgeFileResource(uid: String?) {
        uid ?: return
        val fileResource = fileResourceStore.selectByUid(uid) ?: return
        fileResource.path()?.let { runCatching { File(it).delete() } }
        fileResourceStore.delete(uid)
    }

    companion object {
        private val fileValueTypes = ValueType.entries.filter { it.isFile }
    }
}
