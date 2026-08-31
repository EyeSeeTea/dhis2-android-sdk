package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutor
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.note.Note
import org.hisp.dhis.android.core.note.internal.NoteStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeValue
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityInstanceStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.enrollment.EnrollmentStoreImpl
import org.hisp.dhis.android.persistence.maintenance.D2ErrorStoreImpl
import org.hisp.dhis.android.persistence.note.NoteStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeValueStoreImpl
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
    private val d2CallExecutor = D2CallExecutor(databaseAdapter, D2ErrorStoreImpl(databaseAdapter))

    @Before
    fun setUp() {
        runBlocking {
            trackedEntityAttributeValueStore.delete()
            noteStore.delete()
            enrollmentStore.delete()
            trackedEntityInstanceStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            trackedEntityAttributeValueStore.delete()
            noteStore.delete()
            enrollmentStore.delete()
            trackedEntityInstanceStore.delete()
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
            d2CallExecutor,
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
            d2CallExecutor,
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
            d2CallExecutor,
        ).purge(limit = 1)

        val remainingEnrollmentUids = enrollmentStore.selectUids()
        val remainingNoteUids = noteStore.selectUids()

        assertThat(remainingEnrollmentUids).containsExactly("enrollmentToKeep")
        assertThat(remainingNoteUids).containsExactly("noteToKeep")
    }

    private fun givenAnEnrollment(
        uid: String,
        trackedEntityInstanceUid: String,
    ): Enrollment {
        return Enrollment.builder()
            .uid(uid)
            .trackedEntityInstance(trackedEntityInstanceUid)
            .organisationUnit("organisationUnit")
            .program("program")
            .attributeOptionCombo("attributeOptionCombo")
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
    ): TrackedEntityAttributeValue {
        return TrackedEntityAttributeValue.builder()
            .trackedEntityAttribute("attribute")
            .trackedEntityInstance(trackedEntityInstanceUid)
            .value("value")
            .build()
    }
}
