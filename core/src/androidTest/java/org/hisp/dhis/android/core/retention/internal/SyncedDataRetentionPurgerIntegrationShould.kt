package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutor
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.data.datavalue.DataValueSamples
import org.hisp.dhis.android.core.dataelement.internal.DataElementStore
import org.hisp.dhis.android.core.datavalue.DataValue
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityInstanceStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.dataelement.DataElementStoreImpl
import org.hisp.dhis.android.persistence.datavalue.DataValueStoreImpl
import org.hisp.dhis.android.persistence.enrollment.EnrollmentStoreImpl
import org.hisp.dhis.android.persistence.event.EventStoreImpl
import org.hisp.dhis.android.persistence.fileresource.FileResourceStoreImpl
import org.hisp.dhis.android.persistence.maintenance.D2ErrorStoreImpl
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
class SyncedDataRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val dataValueStore: DataValueStore = DataValueStoreImpl(databaseAdapter)
    private val trackedEntityInstanceStore: TrackedEntityInstanceStore = TrackedEntityInstanceStoreImpl(databaseAdapter)
    private val trackedEntityAttributeValueStore: TrackedEntityAttributeValueStore =
        TrackedEntityAttributeValueStoreImpl(databaseAdapter)
    private val d2CallExecutor = D2CallExecutor(databaseAdapter, D2ErrorStoreImpl(databaseAdapter))
    private val dataElementStore: DataElementStore = DataElementStoreImpl(databaseAdapter)
    private val trackedEntityAttributeStore: TrackedEntityAttributeStore = TrackedEntityAttributeStoreImpl(databaseAdapter)
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val valueFileResourcePurger =
        ValueFileResourcePurger(dataElementStore, trackedEntityAttributeStore, fileResourceStore)

    private val purger = SyncedDataRetentionPurger(
        dataValuePurger = DataValueRetentionPurger(dataValueStore, valueFileResourcePurger),
        trackedEntityPurger = TrackedEntityRetentionPurger(
            trackedEntityInstanceStore,
            trackedEntityAttributeValueStore,
            EnrollmentStoreImpl(databaseAdapter),
            NoteStoreImpl(databaseAdapter),
            EventStoreImpl(databaseAdapter),
            TrackedEntityDataValueStoreImpl(databaseAdapter),
            valueFileResourcePurger,
        ),
        eventPurger = EventRetentionPurger(
            EventStoreImpl(databaseAdapter),
            TrackedEntityDataValueStoreImpl(databaseAdapter),
            NoteStoreImpl(databaseAdapter),
            valueFileResourcePurger,
        ),
        orphanFileResourcePurger = OrphanFileResourceRetentionPurger(
            FileResourceStoreImpl(databaseAdapter),
            dataValueStore,
            trackedEntityAttributeValueStore,
            TrackedEntityDataValueStoreImpl(databaseAdapter),
        ),
        d2CallExecutor = d2CallExecutor,
    )

    @Before
    fun setUp() {
        runBlocking {
            dataValueStore.delete()
            trackedEntityAttributeValueStore.delete()
            trackedEntityInstanceStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            dataValueStore.delete()
            trackedEntityAttributeValueStore.delete()
            trackedEntityInstanceStore.delete()
        }
    }

    @Test
    fun purge_each_data_type_independently_under_its_own_limit_in_a_single_call() = runTest {
        val oldestDataValue = givenADataValue("oldestDataValue", "2026-01-01T00:00:00.000")
        val newestDataValue = givenADataValue("newestDataValue", "2026-02-01T00:00:00.000")

        dataValueStore.insert(listOf(oldestDataValue, newestDataValue))

        val oldestTei = givenATrackedEntityInstance("oldestTei", "2026-01-01T00:00:00.000")
        val newestTei = givenATrackedEntityInstance("newestTei", "2026-02-01T00:00:00.000")

        trackedEntityInstanceStore.insert(oldestTei)
        trackedEntityInstanceStore.insert(newestTei)

        purger.purge(
            RetentionLimits(
                dataValue = 1,
                trackedEntityInstance = 1,
                event = 0,
                fileResource = 0,
            ),
        )

        val remainingDataElements = dataValueStore.selectAll().map { it.dataElement() }
        val remainingTeiUids = trackedEntityInstanceStore.selectUids()

        assertThat(remainingDataElements).containsExactly("newestDataValue")
        assertThat(remainingTeiUids).containsExactly("newestTei")
    }

    @Test
    fun trim_data_values_under_their_limit_without_affecting_an_eligible_tracked_entity_instance() = runTest {
        val oldestDataValue = givenADataValue("oldestDataValue", "2026-01-01T00:00:00.000")
        val newestDataValue = givenADataValue("newestDataValue", "2026-02-01T00:00:00.000")

        dataValueStore.insert(listOf(oldestDataValue, newestDataValue))

        val eligibleTei = givenATrackedEntityInstance("eligibleTei", "2026-01-01T00:00:00.000")

        trackedEntityInstanceStore.insert(eligibleTei)

        purger.purge(
            RetentionLimits(
                dataValue = 0,
                trackedEntityInstance = 1,
                event = 0,
                fileResource = 0,
            ),
        )

        val remainingDataElements = dataValueStore.selectAll().map { it.dataElement() }
        val remainingTeiUids = trackedEntityInstanceStore.selectUids()

        assertThat(remainingDataElements).isEmpty()
        assertThat(remainingTeiUids).containsExactly("eligibleTei")
    }

    @Test
    fun leave_every_data_type_unchanged_when_one_type_fails_partway_through_a_multi_type_purge() = runTest {
        val oldestDataValue = givenADataValue("oldestDataValue", "2026-01-01T00:00:00.000")
        val newestDataValue = givenADataValue("newestDataValue", "2026-02-01T00:00:00.000")

        dataValueStore.insert(listOf(oldestDataValue, newestDataValue))

        val oldestTei = givenATrackedEntityInstance("oldestTei", "2026-01-01T00:00:00.000")
        val newestTei = givenATrackedEntityInstance("newestTei", "2026-02-01T00:00:00.000")

        trackedEntityInstanceStore.insert(oldestTei)
        trackedEntityInstanceStore.insert(newestTei)

        val purgerWithFailingFileResourceStep = SyncedDataRetentionPurger(
            dataValuePurger = DataValueRetentionPurger(dataValueStore, valueFileResourcePurger),
            trackedEntityPurger = TrackedEntityRetentionPurger(
                trackedEntityInstanceStore,
                trackedEntityAttributeValueStore,
                EnrollmentStoreImpl(databaseAdapter),
                NoteStoreImpl(databaseAdapter),
                EventStoreImpl(databaseAdapter),
                TrackedEntityDataValueStoreImpl(databaseAdapter),
                valueFileResourcePurger,
            ),
            eventPurger = EventRetentionPurger(
                EventStoreImpl(databaseAdapter),
                TrackedEntityDataValueStoreImpl(databaseAdapter),
                NoteStoreImpl(databaseAdapter),
                valueFileResourcePurger,
            ),
            orphanFileResourcePurger = GivingAFailingFileResourceRetentionPurger(),
            d2CallExecutor = d2CallExecutor,
        )

        try {
            purgerWithFailingFileResourceStep.purge(
                RetentionLimits(
                    dataValue = 1,
                    trackedEntityInstance = 1,
                    event = 0,
                    fileResource = 0,
                ),
            )
        } catch (expected: Exception) {
            // Expected: the orphanFileResourcePurger step fails after dataValue and
            // trackedEntityInstance already ran, forcing the whole composed
            // transaction to roll back.
        }

        val remainingDataElements = dataValueStore.selectAll().map { it.dataElement() }
        val remainingTeiUids = trackedEntityInstanceStore.selectUids()

        assertThat(remainingDataElements).containsExactly("oldestDataValue", "newestDataValue")
        assertThat(remainingTeiUids).containsExactly("oldestTei", "newestTei")
    }

    /**
     * A [RetentionPurger] that always throws, standing in for
     * [OrphanFileResourceRetentionPurger] to simulate a failure in the last
     * step of a multi-type purge. This proves the composed entry point's
     * transaction covers every purger call, not just the failing one — the
     * earlier dataValue/trackedEntityInstance purgers in the same test run
     * against the real database, so their would-be-committed deletes are what
     * this test verifies get rolled back too.
     */
    private class GivingAFailingFileResourceRetentionPurger : RetentionPurger {
        override suspend fun purge(limit: Int) {
            throw RuntimeException("Simulated failure while purging file resources")
        }
    }

    private fun givenADataValue(
        dataElement: String,
        lastUpdated: String,
    ): DataValue {
        return DataValueSamples.getDataValueDatabase()
            .toBuilder()
            .dataElement(dataElement)
            .syncState(State.SYNCED)
            .lastUpdated(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated))
            .build()
    }

    private fun givenATrackedEntityInstance(
        uid: String,
        lastUpdated: String,
    ): TrackedEntityInstance {
        return TrackedEntityInstance.builder()
            .uid(uid)
            .syncState(State.SYNCED)
            .aggregatedSyncState(State.SYNCED)
            .lastUpdated(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated))
            .deleted(false)
            .build()
    }
}
