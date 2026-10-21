package org.hisp.dhis.android.core.retention.internal

import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(JUnit4::class)
class RetentionModuleImplShould {

    private val syncedDataRetentionPurger: SyncedDataRetentionPurger = mock()

    private val module = RetentionModuleImpl(syncedDataRetentionPurger)

    @Test
    fun delegate_purge_to_synced_data_retention_purger() = runTest {
        module.purge()

        verify(syncedDataRetentionPurger).purge()
    }
}
