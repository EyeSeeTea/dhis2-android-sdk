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
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.note.internal.NoteStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityDataValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityInstanceStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.category.CategoryComboStoreImpl
import org.hisp.dhis.android.persistence.dataelement.DataElementStoreImpl
import org.hisp.dhis.android.persistence.enrollment.EnrollmentStoreImpl
import org.hisp.dhis.android.persistence.event.EventStoreImpl
import org.hisp.dhis.android.persistence.fileresource.FileResourceStoreImpl
import org.hisp.dhis.android.persistence.note.NoteStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeValueStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityDataValueStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityInstanceStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.text.SimpleDateFormat

@RunWith(D2JunitRunner::class)
class TrackedEntityRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore = TrackedEntityInstanceStoreImpl(databaseAdapter)
    private val trackedEntityAttributeValueStore: TrackedEntityAttributeValueStore =
        TrackedEntityAttributeValueStoreImpl(databaseAdapter)
    private val enrollmentStore: EnrollmentStore = EnrollmentStoreImpl(databaseAdapter)
    private val noteStore: NoteStore = NoteStoreImpl(databaseAdapter)
    private val eventStore: EventStore = EventStoreImpl(databaseAdapter)
    private val trackedEntityDataValueStore: TrackedEntityDataValueStore =
        TrackedEntityDataValueStoreImpl(databaseAdapter)
    private val dataElementStore: DataElementStore = DataElementStoreImpl(databaseAdapter)
    private val trackedEntityAttributeStore: TrackedEntityAttributeStore = TrackedEntityAttributeStoreImpl(databaseAdapter)
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val categoryComboStore = CategoryComboStoreImpl(databaseAdapter)
    private val valueFileResourcePurger =
        ValueFileResourcePurger(dataElementStore, trackedEntityAttributeStore, fileResourceStore)

    @Before
    fun setUp() {
        runBlocking {
            trackedEntityAttributeValueStore.delete()
            trackedEntityDataValueStore.delete()
            noteStore.delete()
            eventStore.delete()
            enrollmentStore.delete()
            trackedEntityInstanceStore.delete()
            trackedEntityAttributeStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            trackedEntityAttributeValueStore.delete()
            trackedEntityDataValueStore.delete()
            noteStore.delete()
            eventStore.delete()
            enrollmentStore.delete()
            trackedEntityInstanceStore.delete()
            trackedEntityAttributeStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @Test
    fun purge_a_fully_synced_tracked_entity_instance_together_with_its_attribute_values() = runTest {
        val teiToPurge = givenATrackedEntityInstance("teiToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        val teiToKeep = givenATrackedEntityInstance("teiToKeep", State.SYNCED, "2026-02-01T00:00:00.000")

        trackedEntityInstanceStore.insert(teiToPurge)
        trackedEntityInstanceStore.insert(teiToKeep)

        val attributeValueToPurge = givenATrackedEntityAttributeValue(teiToPurge.uid())
        val attributeValueToKeep = givenATrackedEntityAttributeValue(teiToKeep.uid())

        trackedEntityAttributeValueStore.insert(listOf(attributeValueToPurge, attributeValueToKeep))

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 1)

        val remainingTeiUids = trackedEntityInstanceStore.selectUids()
        val remainingAttributeValueTeiUids = trackedEntityAttributeValueStore.selectAll()
            .map { it.trackedEntityInstance() }

        assertThat(remainingTeiUids).containsExactly("teiToKeep")
        assertThat(remainingAttributeValueTeiUids).containsExactly("teiToKeep")
    }

    @Test
    fun keep_a_tracked_entity_instance_whose_aggregated_sync_state_is_not_synced() = runTest {
        val protectedTei =
            givenATrackedEntityInstance("protectedTei", State.TO_UPDATE, "2025-01-01T00:00:00.000")
        val syncedTei = givenATrackedEntityInstance("syncedTei", State.SYNCED, "2026-02-01T00:00:00.000")

        trackedEntityInstanceStore.insert(protectedTei)
        trackedEntityInstanceStore.insert(syncedTei)

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 0)

        val remainingTeiUids = trackedEntityInstanceStore.selectUids()

        assertThat(remainingTeiUids).containsExactly("protectedTei")
    }

    @Test
    fun purge_the_enrollments_and_their_notes_of_a_purged_tracked_entity_instance() = runTest {
        val teiToPurge = givenATrackedEntityInstance("teiToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        val teiToKeep = givenATrackedEntityInstance("teiToKeep", State.SYNCED, "2026-02-01T00:00:00.000")

        trackedEntityInstanceStore.insert(teiToPurge)
        trackedEntityInstanceStore.insert(teiToKeep)

        val enrollmentToPurge = givenAnEnrollment("enrollmentToPurge", teiToPurge.uid())
        val enrollmentToKeep = givenAnEnrollment("enrollmentToKeep", teiToKeep.uid())

        enrollmentStore.insert(enrollmentToPurge)
        enrollmentStore.insert(enrollmentToKeep)

        val noteToPurge = givenAnEnrollmentNote("noteToPurge", enrollmentToPurge.uid())
        val noteToKeep = givenAnEnrollmentNote("noteToKeep", enrollmentToKeep.uid())

        noteStore.insert(noteToPurge)
        noteStore.insert(noteToKeep)

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 1)

        val remainingEnrollmentUids = enrollmentStore.selectUids()
        val remainingNoteUids = noteStore.selectUids()

        assertThat(remainingEnrollmentUids).containsExactly("enrollmentToKeep")
        assertThat(remainingNoteUids).containsExactly("noteToKeep")
    }

    @Test
    fun keep_the_whole_tree_of_a_tei_that_is_not_eligible_even_if_its_enrollment_and_event_are_synced() = runTest {
        val protectedTei =
            givenATrackedEntityInstance("protectedTei", State.TO_UPDATE, "2025-01-01T00:00:00.000")

        trackedEntityInstanceStore.insert(protectedTei)

        val enrollmentOfProtectedTei = givenAnEnrollment("enrollmentOfProtectedTei", protectedTei.uid())

        enrollmentStore.insert(enrollmentOfProtectedTei)

        val eventOfProtectedTei = givenAnEvent("eventOfProtectedTei", enrollmentOfProtectedTei.uid())

        eventStore.insert(eventOfProtectedTei)

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 0)

        val remainingTeiUids = trackedEntityInstanceStore.selectUids()
        val remainingEnrollmentUids = enrollmentStore.selectUids()
        val remainingEventUids = eventStore.selectUids()

        assertThat(remainingTeiUids).containsExactly("protectedTei")
        assertThat(remainingEnrollmentUids).containsExactly("enrollmentOfProtectedTei")
        assertThat(remainingEventUids).containsExactly("eventOfProtectedTei")
    }

    @Test
    fun purge_the_events_their_data_values_and_their_notes_of_a_purged_tracked_entity_instance() = runTest {
        val teiToPurge = givenATrackedEntityInstance("teiToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        val teiToKeep = givenATrackedEntityInstance("teiToKeep", State.SYNCED, "2026-02-01T00:00:00.000")

        trackedEntityInstanceStore.insert(teiToPurge)
        trackedEntityInstanceStore.insert(teiToKeep)

        val enrollmentToPurge = givenAnEnrollment("enrollmentToPurge", teiToPurge.uid())
        val enrollmentToKeep = givenAnEnrollment("enrollmentToKeep", teiToKeep.uid())

        enrollmentStore.insert(enrollmentToPurge)
        enrollmentStore.insert(enrollmentToKeep)

        val eventToPurge = givenAnEvent("eventToPurge", enrollmentToPurge.uid())
        val eventToKeep = givenAnEvent("eventToKeep", enrollmentToKeep.uid())

        eventStore.insert(eventToPurge)
        eventStore.insert(eventToKeep)

        val dataValueToPurge = givenATrackedEntityDataValue(eventToPurge.uid())
        val dataValueToKeep = givenATrackedEntityDataValue(eventToKeep.uid())

        trackedEntityDataValueStore.insert(listOf(dataValueToPurge, dataValueToKeep))

        val eventNoteToPurge = givenAnEventNote("eventNoteToPurge", eventToPurge.uid())
        val eventNoteToKeep = givenAnEventNote("eventNoteToKeep", eventToKeep.uid())

        noteStore.insert(eventNoteToPurge)
        noteStore.insert(eventNoteToKeep)

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 1)

        val remainingEventUids = eventStore.selectUids()
        val remainingDataValueEventUids = trackedEntityDataValueStore.selectAll().map { it.event() }
        val remainingNoteUids = noteStore.selectUids()

        assertThat(remainingEventUids).containsExactly("eventToKeep")
        assertThat(remainingDataValueEventUids).containsExactly("eventToKeep")
        assertThat(remainingNoteUids).containsExactly("eventNoteToKeep")
    }

    @Test
    fun keep_the_file_resource_of_a_protected_tei_even_if_its_own_sync_state_is_synced() = runTest {
        val protectedTei =
            givenATrackedEntityInstance("protectedTei", State.TO_UPDATE, "2025-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(protectedTei)

        val fileAttribute = givenAFileTrackedEntityAttribute("fileAttribute")
        trackedEntityAttributeStore.insert(fileAttribute)

        val referencedFileResource = givenAFileResource("referencedFile")
        fileResourceStore.insert(referencedFileResource)

        val attributeValue = givenATrackedEntityAttributeValue(protectedTei.uid(), "fileAttribute", "referencedFile")
        trackedEntityAttributeValueStore.insert(attributeValue)

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 0)

        assertThat(fileResourceStore.selectUids()).containsExactly("referencedFile")
    }

    @Test
    fun purge_the_file_resource_referenced_by_an_attribute_value_of_a_purged_tei() = runTest {
        val teiToPurge = givenATrackedEntityInstance("teiToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(teiToPurge)

        val fileAttribute = givenAFileTrackedEntityAttribute("fileAttribute")
        trackedEntityAttributeStore.insert(fileAttribute)

        val referencedFileResource = givenAFileResource("referencedFile")
        fileResourceStore.insert(referencedFileResource)

        val attributeValue = givenATrackedEntityAttributeValue(teiToPurge.uid(), "fileAttribute", "referencedFile")
        trackedEntityAttributeValueStore.insert(attributeValue)

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 0)

        assertThat(fileResourceStore.selectUids()).isEmpty()
    }

    @Test
    fun purge_the_file_resource_referenced_by_an_events_data_value_of_a_purged_tei() = runTest {
        val teiToPurge = givenATrackedEntityInstance("teiToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(teiToPurge)

        val enrollment = givenAnEnrollment("enrollment", teiToPurge.uid())
        enrollmentStore.insert(enrollment)

        val event = givenAnEvent("event", enrollment.uid())
        eventStore.insert(event)

        val fileDataElement = givenAFileDataElement("fileDataElement")
        dataElementStore.insert(fileDataElement)

        val referencedFileResource = givenAFileResource("referencedFile")
        fileResourceStore.insert(referencedFileResource)

        val dataValue = givenATrackedEntityDataValue(event.uid(), "fileDataElement", "referencedFile")
        trackedEntityDataValueStore.insert(dataValue)

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
        ).purge(limit = 0)

        assertThat(fileResourceStore.selectUids()).isEmpty()
    }

    private fun givenAnEvent(
        uid: String,
        enrollmentUid: String,
        syncState: State = State.SYNCED,
    ): Event {
        return Event.builder()
            .uid(uid)
            .enrollment(enrollmentUid)
            .program("program")
            .programStage("programStage")
            .organisationUnit("organisationUnit")
            .attributeOptionCombo("attributeOptionCombo")
            .syncState(syncState)
            .aggregatedSyncState(syncState)
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

    private fun givenAnEnrollment(
        uid: String,
        trackedEntityInstanceUid: String,
        syncState: State = State.SYNCED,
    ): Enrollment {
        return Enrollment.builder()
            .uid(uid)
            .trackedEntityInstance(trackedEntityInstanceUid)
            .organisationUnit("organisationUnit")
            .program("program")
            .attributeOptionCombo("attributeOptionCombo")
            .syncState(syncState)
            .aggregatedSyncState(syncState)
            .build()
    }

    private fun givenAnEnrollmentNote(
        uid: String,
        enrollmentUid: String,
    ): Note {
        return Note.builder()
            .uid(uid)
            .noteType(Note.NoteType.ENROLLMENT_NOTE)
            .enrollment(enrollmentUid)
            .value("a note")
            .build()
    }

    private fun givenATrackedEntityInstance(
        uid: String,
        syncState: State,
        lastUpdated: String,
    ): TrackedEntityInstance {
        return TrackedEntityInstance.builder()
            .uid(uid)
            .syncState(syncState)
            .aggregatedSyncState(syncState)
            .lastUpdated(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated))
            .deleted(false)
            .build()
    }

    private fun givenATrackedEntityAttributeValue(
        trackedEntityInstanceUid: String,
        trackedEntityAttributeUid: String = "attribute",
        value: String = "value",
    ): TrackedEntityAttributeValue {
        return TrackedEntityAttributeValue.builder()
            .trackedEntityAttribute(trackedEntityAttributeUid)
            .trackedEntityInstance(trackedEntityInstanceUid)
            .value(value)
            .build()
    }

    private fun givenAFileTrackedEntityAttribute(uid: String): TrackedEntityAttribute {
        return TrackedEntityAttribute.builder()
            .uid(uid)
            .valueType(ValueType.FILE_RESOURCE)
            .build()
    }

    private fun givenAFileResource(uid: String): FileResource {
        return FileResource.builder()
            .uid(uid)
            .syncState(State.SYNCED)
            .build()
    }
}
