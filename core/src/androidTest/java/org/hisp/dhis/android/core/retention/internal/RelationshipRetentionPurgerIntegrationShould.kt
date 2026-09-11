package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.relationship.Relationship
import org.hisp.dhis.android.core.relationship.RelationshipConstraintType
import org.hisp.dhis.android.core.relationship.RelationshipHelper
import org.hisp.dhis.android.core.relationship.internal.RelationshipItemStore
import org.hisp.dhis.android.core.relationship.internal.RelationshipStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.relationship.RelationshipItemStoreImpl
import org.hisp.dhis.android.persistence.relationship.RelationshipStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(D2JunitRunner::class)
class RelationshipRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val relationshipStore: RelationshipStore = RelationshipStoreImpl(databaseAdapter)
    private val relationshipItemStore: RelationshipItemStore = RelationshipItemStoreImpl(databaseAdapter)

    private val purger = RelationshipRetentionPurger(relationshipStore, relationshipItemStore)

    @Before
    fun setUp() {
        runBlocking {
            relationshipItemStore.delete()
            relationshipStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            relationshipItemStore.delete()
            relationshipStore.delete()
        }
    }

    @Test
    fun purge_a_relationship_and_its_items_when_purging_one_of_its_two_teis() = runTest {
        givenARelationshipBetweenTwoTeis("relationship", "teiA", "teiB")

        purger.purgeForEntity("teiA")

        assertThat(relationshipStore.selectUids()).isEmpty()
        assertThat(relationshipItemStore.getForRelationshipUid("relationship")).isEmpty()
    }

    @Test
    fun purge_a_relationship_the_same_way_regardless_of_which_side_triggers_the_purge() = runTest {
        givenARelationshipBetweenTwoTeis("relationship", "teiA", "teiB")

        // teiB is the TO side of the relationship, not the FROM side used in
        // the other tests — purging from either side must behave identically.
        purger.purgeForEntity("teiB")

        assertThat(relationshipStore.selectUids()).isEmpty()
        assertThat(relationshipItemStore.getForRelationshipUid("relationship")).isEmpty()
    }

    @Test
    fun purge_a_relationship_without_error_when_its_relationship_row_is_already_missing() = runTest {
        // Only the RelationshipItem for teiA exists; the Relationship row itself
        // (and teiB's item) is already gone, simulating a pre-existing orphan.
        val orphanedItem = RelationshipHelper.teiItem("teiA").toBuilder()
            .relationship(ObjectWithUid.create("missingRelationship"))
            .relationshipItemType(RelationshipConstraintType.FROM)
            .build()
        relationshipItemStore.insert(orphanedItem)

        purger.purgeForEntity("teiA")

        assertThat(relationshipItemStore.getByEntityUid("teiA")).isEmpty()
    }

    private suspend fun givenARelationshipBetweenTwoTeis(relationshipUid: String, fromUid: String, toUid: String) {
        val relationship = Relationship.builder()
            .uid(relationshipUid)
            .relationshipType("relationshipType")
            .build()
        relationshipStore.insert(relationship)

        val fromItem = RelationshipHelper.teiItem(fromUid).toBuilder()
            .relationship(ObjectWithUid.create(relationshipUid))
            .relationshipItemType(RelationshipConstraintType.FROM)
            .build()
        val toItem = RelationshipHelper.teiItem(toUid).toBuilder()
            .relationship(ObjectWithUid.create(relationshipUid))
            .relationshipItemType(RelationshipConstraintType.TO)
            .build()

        relationshipItemStore.insert(fromItem)
        relationshipItemStore.insert(toItem)
    }
}
