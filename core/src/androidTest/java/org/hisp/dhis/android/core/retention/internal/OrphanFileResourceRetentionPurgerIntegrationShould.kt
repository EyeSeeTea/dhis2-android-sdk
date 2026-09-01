package org.hisp.dhis.android.core.retention.internal

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.data.datavalue.DataValueSamples
import org.hisp.dhis.android.core.datavalue.DataValue
import org.hisp.dhis.android.core.datavalue.internal.DataValueStore
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityAttributeValueStore
import org.hisp.dhis.android.core.trackedentity.internal.TrackedEntityDataValueStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.datavalue.DataValueStoreImpl
import org.hisp.dhis.android.persistence.fileresource.FileResourceStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityAttributeValueStoreImpl
import org.hisp.dhis.android.persistence.trackedentity.TrackedEntityDataValueStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat

@RunWith(D2JunitRunner::class)
class OrphanFileResourceRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val dataValueStore: DataValueStore = DataValueStoreImpl(databaseAdapter)
    private val trackedEntityAttributeValueStore: TrackedEntityAttributeValueStore =
        TrackedEntityAttributeValueStoreImpl(databaseAdapter)
    private val trackedEntityDataValueStore: TrackedEntityDataValueStore =
        TrackedEntityDataValueStoreImpl(databaseAdapter)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val purger = OrphanFileResourceRetentionPurger(fileResourceStore)

    @Before
    fun setUp() {
        runBlocking {
            fileResourceStore.delete()
            dataValueStore.delete()
            trackedEntityAttributeValueStore.delete()
            trackedEntityDataValueStore.delete()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            fileResourceStore.delete()
            dataValueStore.delete()
            trackedEntityAttributeValueStore.delete()
            trackedEntityDataValueStore.delete()
        }
    }

    @Test
    fun purge_an_eligible_file_resource_beyond_the_limit_together_with_its_physical_file() = runTest {
        val physicalFile = File.createTempFile("fileToPurge", ".txt", context.cacheDir)
        physicalFile.writeText("content")

        val fileToPurge = givenAFileResource("fileToPurge", State.SYNCED, "2026-01-01T00:00:00.000", physicalFile.path)
        val fileToKeep = givenAFileResource("fileToKeep", State.SYNCED, "2026-02-01T00:00:00.000", path = null)

        fileResourceStore.insert(fileToPurge)
        fileResourceStore.insert(fileToKeep)

        purger.purge(limit = 1)

        val remainingUids = fileResourceStore.selectUids()

        assertThat(remainingUids).containsExactly("fileToKeep")
        assertThat(physicalFile.exists()).isFalse()
    }

    @Test
    fun purge_an_eligible_file_resource_whose_physical_file_is_already_missing_without_raising_an_error() = runTest {
        val missingFilePath = context.cacheDir.path + "/does-not-exist.txt"
        val fileWithMissingPhysicalFile =
            givenAFileResource("fileWithMissingPhysicalFile", State.SYNCED, "2026-01-01T00:00:00.000", missingFilePath)

        fileResourceStore.insert(fileWithMissingPhysicalFile)

        purger.purge(limit = 0)

        val remainingUids = fileResourceStore.selectUids()

        assertThat(remainingUids).isEmpty()
    }

    @Test
    fun keep_a_file_resource_still_referenced_by_a_live_data_value_regardless_of_its_own_limit() = runTest {
        val referencedFile = givenAFileResource("referencedFile", State.SYNCED, "2026-01-01T00:00:00.000", path = null)
        fileResourceStore.insert(referencedFile)

        val referencingDataValue = givenADataValue("referencedFile")
        dataValueStore.insert(referencingDataValue)

        purger.purge(limit = 0)

        assertThat(fileResourceStore.selectUids()).containsExactly("referencedFile")
    }

    private fun givenAFileResource(
        uid: String,
        syncState: State,
        lastUpdated: String,
        path: String?,
    ): FileResource {
        val builder = FileResource.builder()
            .uid(uid)
            .syncState(syncState)
            .lastUpdated(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated))
        path?.let { builder.path(it) }
        return builder.build()
    }

    private fun givenADataValue(value: String): DataValue {
        return DataValueSamples.getDataValueDatabase()
            .toBuilder()
            .dataElement("dataElement")
            .value(value)
            .syncState(State.SYNCED)
            .build()
    }
}
