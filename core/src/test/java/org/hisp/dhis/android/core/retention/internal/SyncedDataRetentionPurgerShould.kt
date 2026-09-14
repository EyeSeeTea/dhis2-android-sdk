package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.hisp.dhis.android.core.maintenance.D2Error
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(JUnit4::class)
class SyncedDataRetentionPurgerShould {

    private val dataValuePurger: DataValueRetentionPurger = mock()
    private val trackedEntityPurger: TrackedEntityRetentionPurger = mock()
    private val eventPurger: EventRetentionPurger = mock()
    private val orphanFileResourcePurger: OrphanFileResourceRetentionPurger = mock()
    private val programRetentionLimitResolver: ProgramRetentionLimitResolver = mock()
    private val dataSetRetentionLimitResolver: DataSetRetentionLimitResolver = mock()
    private val retentionSelector: RetentionSelector = mock()

    // A real pass-through, not a mock: it runs the lambda exactly like the production
    // D2CallExecutor would for a non-D2Error failure, so the test exercises the actual
    // wrapping logic in SyncedDataRetentionPurger.purge() instead of asserting on stubs.
    private val runningTheGivenCallDirectly = object : D2CallExecutorInterface {
        override suspend fun <C> executeD2CallTransactionally(call: suspend () -> C): C = call()
        override suspend fun <C> executeD2Call(call: suspend () -> C): C = call()
    }

    private val purger = SyncedDataRetentionPurger(
        dataValuePurger = dataValuePurger,
        trackedEntityPurger = trackedEntityPurger,
        eventPurger = eventPurger,
        orphanFileResourcePurger = orphanFileResourcePurger,
        programRetentionLimitResolver = programRetentionLimitResolver,
        dataSetRetentionLimitResolver = dataSetRetentionLimitResolver,
        retentionSelector = retentionSelector,
        d2CallExecutor = runningTheGivenCallDirectly,
    )

    @Test
    fun wrap_an_unexpected_failure_with_its_original_message_instead_of_a_generic_description() = runTest {
        givenTrackedEntityCandidatesThatFailWhilePurging("Disk is full")

        val actual =
            try {
                purger.purge()
                null
            } catch (d2Error: D2Error) {
                d2Error
            }

        assertThat(actual).isNotNull()
        assertThat(actual!!.errorDescription()).contains("Disk is full")
    }

    private fun givenTrackedEntityCandidatesThatFailWhilePurging(failureMessage: String) {
        trackedEntityPurger.stub {
            onBlocking { eligibleCandidates() } doThrow RuntimeException(failureMessage)
        }
    }
}
