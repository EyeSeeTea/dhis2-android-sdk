package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.api.executors.internal.APICallErrorCatcher
import org.hisp.dhis.android.core.arch.api.executors.internal.CoroutineAPICallExecutor
import org.hisp.dhis.android.core.arch.helpers.Result
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.category.internal.CategoryComboStore
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.data.trackedentity.TrackedEntityDataValueSamples
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.maintenance.D2Error
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.settings.internal.DataSetSettingCall
import org.hisp.dhis.android.core.settings.internal.DataSetSettingHandler
import org.hisp.dhis.android.core.settings.internal.DataSetSettingStore
import org.hisp.dhis.android.core.settings.internal.ProgramSettingCall
import org.hisp.dhis.android.core.settings.internal.ProgramSettingHandler
import org.hisp.dhis.android.core.settings.internal.ProgramSettingStore
import org.hisp.dhis.android.core.settings.internal.SettingsAppDataStoreVersion
import org.hisp.dhis.android.core.settings.internal.SettingsAppInfoManager
import org.hisp.dhis.android.core.settings.internal.SettingsNetworkHandler
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import java.text.SimpleDateFormat

internal suspend fun givenAFileDataElement(categoryComboStore: CategoryComboStore, uid: String): DataElement {
    val categoryCombo = CategoryCombo.builder().uid("$uid-categoryCombo").build()
    categoryComboStore.insert(categoryCombo)

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
): TrackedEntityDataValue = TrackedEntityDataValueSamples.get(dataElementUid, eventUid, value)

internal fun givenAnEventNote(uid: String, eventUid: String): Note {
    return Note.builder()
        .uid(uid)
        .noteType(Note.NoteType.EVENT_NOTE)
        .event(eventUid)
        .value("a note")
        .build()
}

/**
 * A real ProgramSettingCall/DataSetSettingCall backed by the given (real, Room-backed) store.
 * *ObjectRepository.blockingGet() only reads that store — it never calls download() — so the
 * network-facing dependencies below (all interfaces) are only here to satisfy the constructor
 * and are never invoked by a test that sticks to blockingGet().
 */
internal fun givenAProgramSettingCall(store: ProgramSettingStore): ProgramSettingCall =
    ProgramSettingCall(
        ProgramSettingHandler(store),
        NoOpSettingsNetworkHandler,
        NoOpCoroutineAPICallExecutor,
        NoOpSettingsAppInfoManager,
    )

internal fun givenADataSetSettingCall(store: DataSetSettingStore): DataSetSettingCall =
    DataSetSettingCall(
        DataSetSettingHandler(store),
        NoOpSettingsNetworkHandler,
        NoOpCoroutineAPICallExecutor,
        NoOpSettingsAppInfoManager,
    )

private object NoOpSettingsNetworkHandler : SettingsNetworkHandler {
    override suspend fun settingsAppInfo() = error("not used in this test")
    override suspend fun generalSettings(version: SettingsAppDataStoreVersion) = error("not used in this test")
    override suspend fun dataSetSettings(version: SettingsAppDataStoreVersion, storeError: Boolean) =
        error("not used in this test")
    override suspend fun programSettings(version: SettingsAppDataStoreVersion, storeError: Boolean) =
        error("not used in this test")
    override suspend fun synchronizationSettings(version: SettingsAppDataStoreVersion, storeError: Boolean) =
        error("not used in this test")
    override suspend fun appearanceSettings(version: SettingsAppDataStoreVersion, storeError: Boolean) =
        error("not used in this test")
    override suspend fun analyticsSettings(version: SettingsAppDataStoreVersion, storeError: Boolean) =
        error("not used in this test")
    override suspend fun customIntents(version: SettingsAppDataStoreVersion, storeError: Boolean) =
        error("not used in this test")
}

private object NoOpCoroutineAPICallExecutor : CoroutineAPICallExecutor {
    override suspend fun <P> wrap(
        storeError: Boolean,
        acceptedErrorCodes: List<Int>?,
        errorCatcher: APICallErrorCatcher?,
        errorClassParser: ((body: String) -> P)?,
        block: suspend () -> P,
    ): Result<P, D2Error> = error("not used in this test")

    override suspend fun <P> wrapTransactionallyRoom(
        cleanForeignKeyErrors: Boolean,
        block: suspend () -> P,
    ): P = error("not used in this test")
}

private object NoOpSettingsAppInfoManager : SettingsAppInfoManager {
    override suspend fun getDataStoreVersion() = error("not used in this test")
    override suspend fun getAppVersion() = error("not used in this test")
    override suspend fun updateAppVersion() = error("not used in this test")
}
