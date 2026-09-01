package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.category.CategoryCombo
import org.hisp.dhis.android.core.common.ObjectWithUid
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.common.ValueType
import org.hisp.dhis.android.core.data.datavalue.DataValueSamples
import org.hisp.dhis.android.core.dataelement.DataElement
import org.hisp.dhis.android.core.dataelement.internal.DataElementStore
import org.hisp.dhis.android.core.datavalue.DataValue
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.category.CategoryComboStoreImpl
import org.hisp.dhis.android.persistence.dataelement.DataElementStoreImpl
import org.hisp.dhis.android.persistence.datavalue.DataValueStoreImpl
import org.hisp.dhis.android.persistence.fileresource.FileResourceStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(D2JunitRunner::class)
class DataValueRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val dataValueStore: DataValueStore = DataValueStoreImpl(databaseAdapter)
    private val dataElementStore: DataElementStore = DataElementStoreImpl(databaseAdapter)
    private val trackedEntityAttributeStore: TrackedEntityAttributeStore = TrackedEntityAttributeStoreImpl(databaseAdapter)
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val categoryComboStore = CategoryComboStoreImpl(databaseAdapter)
    private val valueFileResourcePurger =
        ValueFileResourcePurger(dataElementStore, trackedEntityAttributeStore, fileResourceStore)

    @Before
    fun setUp() {
        runBlocking {
            dataValueStore.delete()
            dataElementStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            dataValueStore.delete()
            dataElementStore.delete()
            fileResourceStore.delete()
            categoryComboStore.delete()
        }
    }

    @Test
    fun keep_only_the_most_recently_updated_synced_data_values_up_to_the_limit() = runTest {
        val oldestSynced = givenADataValue("oldestSynced", State.SYNCED, "2026-01-01T00:00:00.000")
        val middleSynced = givenADataValue("middleSynced", State.SYNCED, "2026-02-01T00:00:00.000")
        val newestSynced = givenADataValue("newestSynced", State.SYNCED, "2026-03-01T00:00:00.000")
        val pending = givenADataValue("pending", State.TO_UPDATE, "2025-01-01T00:00:00.000")

        dataValueStore.insert(listOf(oldestSynced, middleSynced, newestSynced, pending))

        DataValueRetentionPurger(dataValueStore, valueFileResourcePurger).purge(limit = 2)

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

        DataValueRetentionPurger(dataValueStore, valueFileResourcePurger).purge(limit = 0)

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

        DataValueRetentionPurger(dataValueStore, valueFileResourcePurger).purge(limit = 2)

        val remaining = dataValueStore.selectAll()
        val remainingDataElements = remaining.map { it.dataElement() }

        assertThat(remainingDataElements).containsExactly("oldestSynced", "newestSynced", "pending")
    }

    @Test
    fun purge_the_file_resource_referenced_by_a_purged_data_value() = runTest {
        val fileDataElement = givenAFileDataElement("fileDataElement")
        dataElementStore.insert(fileDataElement)

        val referencedFileResource = givenAFileResource("referencedFile")
        fileResourceStore.insert(referencedFileResource)

        val dataValueToPurge = givenADataValue("fileDataElement", State.SYNCED, "2026-01-01T00:00:00.000")
            .toBuilder().value("referencedFile").build()

        dataValueStore.insert(listOf(dataValueToPurge))

        DataValueRetentionPurger(dataValueStore, valueFileResourcePurger).purge(limit = 0)

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
