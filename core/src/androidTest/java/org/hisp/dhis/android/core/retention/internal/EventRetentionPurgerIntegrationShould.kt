package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.dataelement.internal.DataElementStore
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.event.Event
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.note.internal.NoteStore
import org.hisp.dhis.android.core.relationship.Relationship
import org.hisp.dhis.android.core.relationship.RelationshipConstraintType
import org.hisp.dhis.android.core.relationship.RelationshipHelper
import org.hisp.dhis.android.core.relationship.RelationshipItem
import org.hisp.dhis.android.core.relationship.internal.RelationshipItemStore
import org.hisp.dhis.android.core.relationship.internal.RelationshipStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeStore
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
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityDataValueStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityInstanceStoreImpl
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
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore = TrackedEntityInstanceStoreImpl(databaseAdapter)
    private val enrollmentStore: EnrollmentStore = EnrollmentStoreImpl(databaseAdapter)
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
            trackedEntityDataValueStore.delete()
            noteStore.delete()
            eventStore.delete()
            dataElementStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
            relationshipItemStore.delete()
            relationshipStore.delete()
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
            relationshipItemStore.delete()
            relationshipStore.delete()
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        val remainingEventUids = eventStore.selectUids()

        assertThat(remainingEventUids).containsExactly("protectedEvent")
    }

    @Test
    fun purge_the_file_resource_referenced_by_a_purged_tei_less_events_data_value() = runTest {
        val eventToPurge = givenATeiLessEvent("eventToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        eventStore.insert(eventToPurge)

        val fileDataElement = givenAFileDataElement(categoryComboStore, "fileDataElement")
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
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        assertThat(fileResourceStore.selectUids()).isEmpty()
    }

    @Test
    fun purge_a_relationship_when_purging_a_tei_less_event_related_to_an_eligible_tei() = runTest {
        val eventToPurge = givenATeiLessEvent("eventToPurge", State.SYNCED, "2026-01-01T00:00:00.000")
        eventStore.insert(eventToPurge)

        val eligibleTei = givenATrackedEntityInstance("eligibleTei", State.SYNCED)
        trackedEntityInstanceStore.insert(eligibleTei)

        givenARelationship(
            "relationship",
            RelationshipHelper.eventItem(eventToPurge.uid()),
            RelationshipHelper.teiItem(eligibleTei.uid()),
        )

        EventRetentionPurger(
            eventStore,
            trackedEntityDataValueStore,
            noteStore,
            valueFileResourcePurger,
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        assertThat(eventStore.selectUids()).isEmpty()
        assertThat(relationshipStore.selectUids()).isEmpty()
    }

    @Test
    fun keep_a_tei_less_event_that_has_a_relationship_to_a_non_eligible_counterpart() = runTest {
        val protectedEvent = givenATeiLessEvent("protectedEvent", State.SYNCED, "2026-01-01T00:00:00.000")
        eventStore.insert(protectedEvent)

        val nonEligibleTei = givenATrackedEntityInstance("nonEligibleTei", State.TO_UPDATE)
        trackedEntityInstanceStore.insert(nonEligibleTei)

        givenARelationship(
            "relationship",
            RelationshipHelper.eventItem(protectedEvent.uid()),
            RelationshipHelper.teiItem(nonEligibleTei.uid()),
        )

        EventRetentionPurger(
            eventStore,
            trackedEntityDataValueStore,
            noteStore,
            valueFileResourcePurger,
            relationshipEligibilityChecker,
            relationshipRetentionPurger,
        ).purge(limit = 0)

        assertThat(eventStore.selectUids()).containsExactly("protectedEvent")
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

    private fun givenATrackedEntityInstance(
        uid: String,
        syncState: State,
    ): TrackedEntityInstance {
        return TrackedEntityInstance.builder()
            .uid(uid)
            .syncState(syncState)
            .aggregatedSyncState(syncState)
            .deleted(false)
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
}
