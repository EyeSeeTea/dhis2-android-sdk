package org.hisp.dhis.android.core.retention.internal

import kotlinx.coroutines.runBlocking
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.category.internal.CategoryComboStore
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import java.text.SimpleDateFormat

internal fun givenAFileDataElement(categoryComboStore: CategoryComboStore, uid: String): DataElement {
    val categoryCombo = CategoryCombo.builder().uid("$uid-categoryCombo").build()
    runBlocking { categoryComboStore.insert(categoryCombo) }

    return DataElement.builder()
        .uid(uid)
        .valueType(ValueType.FILE_RESOURCE)
        .categoryCombo(ObjectWithUid.fromIdentifiable(categoryCombo))
        .domainType("AGGREGATE")
        .build()
}

internal fun givenAFileResource(
    uid: String,
    syncState: State = State.SYNCED,
    lastUpdated: String? = null,
    path: String? = null,
): FileResource {
    val builder = FileResource.builder()
        .uid(uid)
        .syncState(syncState)
    lastUpdated?.let { builder.lastUpdated(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(it)) }
    path?.let { builder.path(it) }
    return builder.build()
}

internal fun givenATrackedEntityDataValue(
    eventUid: String,
    dataElementUid: String = "dataElement",
    value: String = "value",
): TrackedEntityDataValue {
    return TrackedEntityDataValue.builder()
        .event(eventUid)
        .dataElement(dataElementUid)
        .value(value)
        .build()
}

internal fun givenAnEventNote(uid: String, eventUid: String): Note {
    return Note.builder()
        .uid(uid)
        .noteType(Note.NoteType.EVENT_NOTE)
        .event(eventUid)
        .value("a note")
        .build()
}
