package org.hisp.dhis.android.core.retention.internal

import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.common.State
import org.hisp.dhis.android.core.fileresource.FileResource
import org.hisp.dhis.android.core.fileresource.internal.FileResourceStore
import org.hisp.dhis.android.core.utils.integration.mock.TestDatabaseAdapterFactory
import org.hisp.dhis.android.core.utils.runner.D2JunitRunner
import org.hisp.dhis.android.persistence.fileresource.FileResourceStoreImpl
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.text.SimpleDateFormat

@RunWith(D2JunitRunner::class)
class FileResourceRetentionPurgerIntegrationShould {

    private val databaseAdapter = TestDatabaseAdapterFactory.get()
    private val fileResourceStore: FileResourceStore = FileResourceStoreImpl(databaseAdapter)
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        runBlocking { fileResourceStore.delete() }
    }

    @After
    fun tearDown() {
        runBlocking { fileResourceStore.delete() }
    }

    @Test
    fun purge_an_eligible_file_resource_beyond_the_limit_together_with_its_physical_file() = runTest {
        val physicalFile = File.createTempFile("fileToPurge", ".txt", context.cacheDir)
        physicalFile.writeText("content")

        val fileToPurge = givenAFileResource("fileToPurge", State.SYNCED, "2026-01-01T00:00:00.000", physicalFile.path)
        val fileToKeep = givenAFileResource("fileToKeep", State.SYNCED, "2026-02-01T00:00:00.000", path = null)

        fileResourceStore.insert(fileToPurge)
        fileResourceStore.insert(fileToKeep)

        FileResourceRetentionPurger(fileResourceStore).purge(limit = 1)

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

        FileResourceRetentionPurger(fileResourceStore).purge(limit = 0)

        val remainingUids = fileResourceStore.selectUids()

        assertThat(remainingUids).isEmpty()
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
}
