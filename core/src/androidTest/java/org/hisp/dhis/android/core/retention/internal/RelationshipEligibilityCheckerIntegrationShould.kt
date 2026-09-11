package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.enrollment.Enrollment
import org.hisp.dhis.android.core.enrollment.internal.EnrollmentStore
import org.hisp.dhis.android.core.event.internal.EventStore
import org.hisp.dhis.android.core.relationship.Relationship
import org.hisp.dhis.android.core.relationship.RelationshipConstraintType
import org.hisp.dhis.android.core.relationship.RelationshipHelper
import org.hisp.dhis.android.core.relationship.RelationshipItem
import org.hisp.dhis.android.core.relationship.internal.RelationshipItemStore
import org.hisp.dhis.android.core.relationship.internal.RelationshipStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityInstanceStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.enrollment.EnrollmentStoreImpl
import org.hisp.dhis.android.persistence.event.EventStoreImpl
import org.hisp.dhis.android.persistence.relationship.RelationshipItemStoreImpl
import org.hisp.dhis.android.persistence.relationship.RelationshipStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityInstanceStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(D2JunitRunner::class)
class RelationshipEligibilityCheckerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val relationshipStore: RelationshipStore = RelationshipStoreImpl(databaseAdapter)
    private val relationshipItemStore: RelationshipItemStore = RelationshipItemStoreImpl(databaseAdapter)
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore = TrackedEntityInstanceStoreImpl(databaseAdapter)
    private val enrollmentStore: EnrollmentStore = EnrollmentStoreImpl(databaseAdapter)
    private val eventStore: EventStore = EventStoreImpl(databaseAdapter)

    private val checker = RelationshipEligibilityChecker(
        relationshipItemStore,
        trackedEntityInstanceStore,
        enrollmentStore,
        eventStore,
    )

    @Before
    fun setUp() {
        runBlocking {
            relationshipItemStore.delete()
            relationshipStore.delete()
            trackedEntityInstanceStore.delete()
            enrollmentStore.delete()
            eventStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            relationshipItemStore.delete()
            relationshipStore.delete()
            trackedEntityInstanceStore.delete()
            enrollmentStore.delete()
            eventStore.delete()
        }
    }

    @Test
    fun be_not_eligible_when_the_relationship_counterpart_tei_is_not_fully_synced() = runTest {
        trackedEntityInstanceStore.insert(givenATrackedEntityInstance("teiA", State.SYNCED))
        trackedEntityInstanceStore.insert(givenATrackedEntityInstance("teiB", State.TO_UPDATE))
        givenARelationshipBetweenTeis("relationship", "teiA", "teiB")

        assertThat(checker.isEligible("teiA")).isFalse()
    }

    @Test
    fun be_not_eligible_when_the_relationship_counterpart_enrollment_is_not_fully_synced() = runTest {
        trackedEntityInstanceStore.insert(givenATrackedEntityInstance("teiA", State.SYNCED))
        enrollmentStore.insert(givenAnEnrollment("enrollmentB", State.TO_UPDATE))
        givenARelationship("relationship", teiUid = "teiA", enrollmentUid = "enrollmentB")

        assertThat(checker.isEligible("teiA")).isFalse()
    }

    private suspend fun givenARelationshipBetweenTeis(relationshipUid: String, fromUid: String, toUid: String) {
        givenARelationship(relationshipUid)
        insertItem(relationshipUid, RelationshipConstraintType.FROM, RelationshipHelper.teiItem(fromUid))
        insertItem(relationshipUid, RelationshipConstraintType.TO, RelationshipHelper.teiItem(toUid))
    }

    private suspend fun givenARelationship(relationshipUid: String, teiUid: String, enrollmentUid: String) {
        givenARelationship(relationshipUid)
        insertItem(relationshipUid, RelationshipConstraintType.FROM, RelationshipHelper.teiItem(teiUid))
        insertItem(relationshipUid, RelationshipConstraintType.TO, RelationshipHelper.enrollmentItem(enrollmentUid))
    }

    private suspend fun givenARelationship(relationshipUid: String) {
        relationshipStore.insert(
            Relationship.builder()
                .uid(relationshipUid)
                .relationshipType("relationshipType")
                .build(),
        )
    }

    private suspend fun insertItem(
        relationshipUid: String,
        constraintType: RelationshipConstraintType,
        item: RelationshipItem,
    ) {
        relationshipItemStore.insert(
            item.toBuilder()
                .relationship(ObjectWithUid.create(relationshipUid))
                .relationshipItemType(constraintType)
                .build(),
        )
    }

    private fun givenATrackedEntityInstance(uid: String, syncState: State): TrackedEntityInstance {
        return TrackedEntityInstance.builder()
            .uid(uid)
            .syncState(syncState)
            .aggregatedSyncState(syncState)
            .deleted(false)
            .build()
    }

    private fun givenAnEnrollment(uid: String, syncState: State): Enrollment {
        return Enrollment.builder()
            .uid(uid)
            .trackedEntityInstance("trackedEntityInstance-$uid")
            .organisationUnit("organisationUnit")
            .program("program")
            .attributeOptionCombo("attributeOptionCombo")
            .syncState(syncState)
            .aggregatedSyncState(syncState)
            .build()
    }
}
