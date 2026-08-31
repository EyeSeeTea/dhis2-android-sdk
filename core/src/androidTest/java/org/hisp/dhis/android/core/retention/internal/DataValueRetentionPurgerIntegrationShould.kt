package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.data.datavalue.DataValueSamples
import org.hisp.dhis.android.core.datavalue.DataValue
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.datavalue.DataValueStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(D2JunitRunner::class)
class DataValueRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val dataValueStore: DataValueStore = DataValueStoreImpl(databaseAdapter)

    @Before
    fun setUp() {
        runBlocking { dataValueStore.delete() }
    }

    @After
    fun tearDown() {
        runBlocking { dataValueStore.delete() }
    }

    @Test
    fun keep_only_the_most_recently_updated_synced_data_values_up_to_the_limit() = runTest {
        val oldestSynced = givenADataValue("oldestSynced", State.SYNCED, "2026-01-01T00:00:00.000")
        val middleSynced = givenADataValue("middleSynced", State.SYNCED, "2026-02-01T00:00:00.000")
        val newestSynced = givenADataValue("newestSynced", State.SYNCED, "2026-03-01T00:00:00.000")
        val pending = givenADataValue("pending", State.TO_UPDATE, "2025-01-01T00:00:00.000")

        dataValueStore.insert(listOf(oldestSynced, middleSynced, newestSynced, pending))

        DataValueRetentionPurger(dataValueStore).purge(limit = 2)

        val remaining = dataValueStore.selectAll()
        val remainingDataElements = remaining.map { it.dataElement() }

        assertThat(remainingDataElements).containsExactly("middleSynced", "newestSynced", "pending")
    }

    @Test
    fun purge_every_synced_data_value_when_limit_is_zero() = runTest {
        val oldestSynced = givenADataValue("oldestSynced", State.SYNCED, "2026-01-01T00:00:00.000")
        val newestSynced = givenADataValue("newestSynced", State.SYNCED, "2026-02-01T00:00:00.000")
        val pending = givenADataValue("pending", State.TO_UPDATE, "2025-01-01T00:00:00.000")

        dataValueStore.insert(listOf(oldestSynced, newestSynced, pending))

        DataValueRetentionPurger(dataValueStore).purge(limit = 0)

        val remaining = dataValueStore.selectAll()
        val remainingDataElements = remaining.map { it.dataElement() }

        assertThat(remainingDataElements).containsExactly("pending")
    }

    @Test
    fun keep_every_synced_data_value_when_eligible_rows_are_at_or_below_the_limit() = runTest {
        val oldestSynced = givenADataValue("oldestSynced", State.SYNCED, "2026-01-01T00:00:00.000")
        val newestSynced = givenADataValue("newestSynced", State.SYNCED, "2026-02-01T00:00:00.000")
        val pending = givenADataValue("pending", State.TO_UPDATE, "2025-01-01T00:00:00.000")

        dataValueStore.insert(listOf(oldestSynced, newestSynced, pending))

        DataValueRetentionPurger(dataValueStore).purge(limit = 2)

        val remaining = dataValueStore.selectAll()
        val remainingDataElements = remaining.map { it.dataElement() }

        assertThat(remainingDataElements).containsExactly("oldestSynced", "newestSynced", "pending")
    }

    private fun givenADataValue(
        dataElement: String,
        syncState: State,
        lastUpdated: String,
    ): DataValue {
        return DataValueSamples.getDataValueDatabase()
            .toBuilder()
            .dataElement(dataElement)
            .syncState(syncState)
            .lastUpdated(java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated))
            .build()
    }
}
