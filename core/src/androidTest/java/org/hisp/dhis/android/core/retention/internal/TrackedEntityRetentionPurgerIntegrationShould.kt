package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.dataelement.internal.DataElementStore
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.note.internal.NoteStore
import org.hisp.dhis.android.core.relationship.Relationship
import org.hisp.dhis.android.core.relationship.RelationshipConstraintType
import org.hisp.dhis.android.core.relationship.RelationshipHelper
import org.hisp.dhis.android.core.relationship.RelationshipItem
import org.hisp.dhis.android.core.relationship.internal.RelationshipItemStore
import org.hisp.dhis.android.core.relationship.internal.RelationshipStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttribute
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue
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
import org.hisp.dhis.android.persistence.relationship.RelationshipItemStoreImpl
import org.hisp.dhis.android.persistence.relationship.RelationshipStoreImpl
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
    private val trackedEntityAttributeStore: TrackedEntityAttributeStore =
        TrackedEntityAttributeStoreImpl(databaseAdapter)
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val categoryComboStore = CategoryComboStoreImpl(databaseAdapter)
    private val relationshipStore: RelationshipStore = RelationshipStoreImpl(databaseAdapter)
    private val relationshipItemStore: RelationshipItemStore = RelationshipItemStoreImpl(databaseAdapter)
    private val valueFileResourcePurger =
        ValueFileResourcePurger(dataElementStore, trackedEntityAttributeStore, fileResourceStore)
    private val relationshipEligibilityChecker = RelationshipEligibilityChecker(
        relationshipItemStore,
        trackedEntityInstanceStore,
        enrollmentStore,
        eventStore,
    )
    private val relationshipRetentionPurger = RelationshipRetentionPurger(relationshipStore, relationshipItemStore)

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
            relationshipItemStore.delete()
            relationshipStore.delete()
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
            relationshipItemStore.delete()
            relationshipStore.delete()
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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

        val fileDataElement = givenAFileDataElement(categoryComboStore, "fileDataElement")
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        assertThat(fileResourceStore.selectUids()).isEmpty()
    }

    @Test
    fun purge_a_relationship_when_purging_both_of_its_fully_synced_teis() = runTest {
        val teiA = givenATrackedEntityInstance("teiA", State.SYNCED, "2026-01-01T00:00:00.000")
        val teiB = givenATrackedEntityInstance("teiB", State.SYNCED, "2025-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(teiA)
        trackedEntityInstanceStore.insert(teiB)
        givenARelationshipBetweenTeis("relationship", "teiA", "teiB")

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        assertThat(trackedEntityInstanceStore.selectUids()).isEmpty()
        assertThat(relationshipStore.selectUids()).isEmpty()
    }

    @Test
    fun keep_a_tei_that_has_a_relationship_to_a_non_eligible_counterpart() = runTest {
        val eligibleTei = givenATrackedEntityInstance("eligibleTei", State.SYNCED, "2026-01-01T00:00:00.000")
        val nonEligibleTei = givenATrackedEntityInstance("nonEligibleTei", State.TO_UPDATE, "2025-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(eligibleTei)
        trackedEntityInstanceStore.insert(nonEligibleTei)
        givenARelationshipBetweenTeis("relationship", "eligibleTei", "nonEligibleTei")

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        assertThat(trackedEntityInstanceStore.selectUids()).containsExactly("eligibleTei", "nonEligibleTei")
        assertThat(relationshipStore.selectUids()).containsExactly("relationship")
    }

    @Test
    fun purge_a_relationship_when_purging_an_enrollment_related_to_an_eligible_event() = runTest {
        val teiToPurge = givenATrackedEntityInstance("teiToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(teiToPurge)
        val enrollment = givenAnEnrollment("enrollment", teiToPurge.uid())
        enrollmentStore.insert(enrollment)

        val unrelatedTei = givenATrackedEntityInstance("unrelatedTei", State.SYNCED, "2026-02-01T00:00:00.000")
        trackedEntityInstanceStore.insert(unrelatedTei)
        val unrelatedEnrollment = givenAnEnrollment("unrelatedEnrollment", unrelatedTei.uid())
        enrollmentStore.insert(unrelatedEnrollment)
        val unrelatedEvent = givenAnEvent("unrelatedEvent", unrelatedEnrollment.uid())
        eventStore.insert(unrelatedEvent)

        givenARelationship(
            "relationship",
            RelationshipHelper.enrollmentItem(enrollment.uid()),
            RelationshipHelper.eventItem(unrelatedEvent.uid()),
        )

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 1)

        assertThat(enrollmentStore.selectUids()).containsExactly("unrelatedEnrollment")
        assertThat(relationshipStore.selectUids()).isEmpty()
    }

    @Test
    fun keep_a_tei_whose_enrollment_has_a_relationship_to_a_non_eligible_counterpart() = runTest {
        val protectedTei = givenATrackedEntityInstance("protectedTei", State.SYNCED, "2026-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(protectedTei)
        val enrollment = givenAnEnrollment("enrollment", protectedTei.uid())
        enrollmentStore.insert(enrollment)

        val nonEligibleTei = givenATrackedEntityInstance("nonEligibleTei", State.TO_UPDATE, "2025-01-01T00:00:00.000")
        trackedEntityInstanceStore.insert(nonEligibleTei)

        givenARelationship(
            "relationship",
            RelationshipHelper.enrollmentItem(enrollment.uid()),
            RelationshipHelper.teiItem(nonEligibleTei.uid()),
        )

        TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            enrollmentStore,
            noteStore,
            eventStore,
            trackedEntityDataValueStore,
            valueFileResourcePurger,
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        assertThat(trackedEntityInstanceStore.selectUids()).contains("protectedTei")
        assertThat(enrollmentStore.selectUids()).containsExactly("enrollment")
    }

    private suspend fun givenARelationshipBetweenTeis(relationshipUid: String, fromUid: String, toUid: String) {
        givenARelationship(relationshipUid, RelationshipHelper.teiItem(fromUid), RelationshipHelper.teiItem(toUid))
    }

    private suspend fun givenARelationship(relationshipUid: String, from: RelationshipItem, to: RelationshipItem) {
        relationshipStore.insert(
            Relationship.builder()
                .uid(relationshipUid)
                .relationshipType("relationshipType")
                .build(),
        )
        relationshipItemStore.insert(
            from.toBuilder()
                .relationship(ObjectWithUid.create(relationshipUid))
                .relationshipItemType(RelationshipConstraintType.FROM)
                .build(),
        )
        relationshipItemStore.insert(
            to.toBuilder()
                .relationship(ObjectWithUid.create(relationshipUid))
                .relationshipItemType(RelationshipConstraintType.TO)
                .build(),
        )
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
}
