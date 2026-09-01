package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.dataelement.internal.DataElementStore
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.note.internal.NoteStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityDataValueStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.category.CategoryComboStoreImpl
import org.hisp.dhis.android.persistence.dataelement.DataElementStoreImpl
import org.hisp.dhis.android.persistence.event.EventStoreImpl
import org.hisp.dhis.android.persistence.fileresource.FileResourceStoreImpl
import org.hisp.dhis.android.persistence.note.NoteStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityDataValueStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat

@RunWith(D2JunitRunner::class)
class EventRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val eventStore: EventStore = EventStoreImpl(databaseAdapter)
    private val trackedEntityDataValueStore: TrackedEntityDataValueStore =
        TrackedEntityDataValueStoreImpl(databaseAdapter)
    private val noteStore: NoteStore = NoteStoreImpl(databaseAdapter)
    private val dataElementStore: DataElementStore = DataElementStoreImpl(databaseAdapter)
    private val trackedEntityAttributeStore: TrackedEntityAttributeStore =
        TrackedEntityAttributeStoreImpl(databaseAdapter)
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val categoryComboStore = CategoryComboStoreImpl(databaseAdapter)
    private val valueFileResourcePurger =
        ValueFileResourcePurger(dataElementStore, trackedEntityAttributeStore, fileResourceStore)

    @Before
    fun setUp() {
        runBlocking {
            trackedEntityDataValueStore.delete()
            noteStore.delete()
            eventStore.delete()
            dataElementStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            trackedEntityDataValueStore.delete()
            noteStore.delete()
            eventStore.delete()
            dataElementStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @Test
    fun purge_the_oldest_synced_tei_less_events_beyond_the_limit_with_their_data_values_and_notes() = runTest {
        val eventToPurge = givenATeiLessEvent("eventToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        val eventToKeep = givenATeiLessEvent("eventToKeep", State.SYNCED, "2026-02-01T00:00:00.000")

        eventStore.insert(eventToPurge)
        eventStore.insert(eventToKeep)

        val dataValueToPurge = givenATrackedEntityDataValue(eventToPurge.uid())
        val dataValueToKeep = givenATrackedEntityDataValue(eventToKeep.uid())

        trackedEntityDataValueStore.insert(listOf(dataValueToPurge, dataValueToKeep))

        val noteToPurge = givenAnEventNote("noteToPurge", eventToPurge.uid())
        val noteToKeep = givenAnEventNote("noteToKeep", eventToKeep.uid())

        noteStore.insert(noteToPurge)
        noteStore.insert(noteToKeep)

        EventRetentionPurger(
            eventStore,
            trackedEntityDataValueStore,
            noteStore,
            valueFileResourcePurger,
        ).purge(limit = 1)

        val remainingEventUids = eventStore.selectUids()
        val remainingDataValueEventUids = trackedEntityDataValueStore.selectAll().map { it.event() }
        val remainingNoteUids = noteStore.selectUids()

        assertThat(remainingEventUids).containsExactly("eventToKeep")
        assertThat(remainingDataValueEventUids).containsExactly("eventToKeep")
        assertThat(remainingNoteUids).containsExactly("noteToKeep")
    }

    @Test
    fun keep_a_tei_less_event_whose_own_aggregated_sync_state_is_not_synced() = runTest {
        val protectedEvent = givenATeiLessEvent("protectedEvent", State.TO_UPDATE, "2025-01-01T00:00:00.000")
        val syncedEvent = givenATeiLessEvent("syncedEvent", State.SYNCED, "2026-02-01T00:00:00.000")

        eventStore.insert(protectedEvent)
        eventStore.insert(syncedEvent)

        EventRetentionPurger(
            eventStore,
            trackedEntityDataValueStore,
            noteStore,
            valueFileResourcePurger,
        ).purge(limit = 0)

        val remainingEventUids = eventStore.selectUids()

        assertThat(remainingEventUids).containsExactly("protectedEvent")
    }

    @Test
    fun purge_the_file_resource_referenced_by_a_purged_tei_less_events_data_value() = runTest {
        val eventToPurge = givenATeiLessEvent("eventToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        eventStore.insert(eventToPurge)

        val fileDataElement = givenAFileDataElement("fileDataElement")
        dataElementStore.insert(fileDataElement)

        val referencedFileResource = givenAFileResource("referencedFile")
        fileResourceStore.insert(referencedFileResource)

        val dataValue = givenATrackedEntityDataValue(eventToPurge.uid(), "fileDataElement", "referencedFile")
        trackedEntityDataValueStore.insert(dataValue)

        EventRetentionPurger(
            eventStore,
            trackedEntityDataValueStore,
            noteStore,
            valueFileResourcePurger,
        ).purge(limit = 0)

        assertThat(fileResourceStore.selectUids()).isEmpty()
    }

    private fun givenAFileDataElement(uid: String): DataElement {
        val categoryCombo = CategoryCombo.builder().uid("$uid-categoryCombo").build()
        runBlocking { categoryComboStore.insert(categoryCombo) }

        return DataElement.builder()
            .uid(uid)
            .valueType(ValueType.FILE_RESOURCE)
            .categoryCombo(ObjectWithUid.fromIdentifiable(categoryCombo))
            .domainType("AGGREGATE")
            .build()
    }

    private fun givenAFileResource(uid: String): FileResource {
        return FileResource.builder()
            .uid(uid)
            .syncState(State.SYNCED)
            .build()
    }

    private fun givenATeiLessEvent(
        uid: String,
        syncState: State,
        lastUpdated: String,
    ): Event {
        return Event.builder()
            .uid(uid)
            .enrollment(null)
            .program("program")
            .programStage("programStage")
            .organisationUnit("organisationUnit")
            .attributeOptionCombo("attributeOptionCombo")
            .syncState(syncState)
            .aggregatedSyncState(syncState)
            .lastUpdated(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated))
            .build()
    }

    private fun givenATrackedEntityDataValue(
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

    private fun givenAnEventNote(
        uid: String,
        eventUid: String,
    ): Note {
        return Note.builder()
            .uid(uid)
            .noteType(Note.NoteType.EVENT_NOTE)
            .event(eventUid)
            .value("a note")
            .build()
    }
}
