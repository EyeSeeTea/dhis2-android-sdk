package org.hisp.dhis.android.core.retention.internal

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.arch.call.executors.internal.D2CallExecutorInterface
import org.hisp.dhis.android.core.maintenance.D2Error
import org.hisp.dhis.android.core.settings.LimitScope
import org.hisp.dhis.android.core.settings.ProgramSetting
import org.hisp.dhis.android.core.settings.ProgramSettings
import org.hisp.dhis.android.core.settings.ProgramSettingsObjectRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat

@RunWith(JUnit4::class)
class SyncedDataRetentionPurgerShould {

    private val dataValuePurger: DataValueRetentionPurger = mock()
    private val trackedEntityPurger: TrackedEntityRetentionPurger = mock()
    private val eventPurger: EventRetentionPurger = mock()
    private val orphanFileResourcePurger: OrphanFileResourceRetentionPurger = mock()
    private val programSettingsObjectRepository: ProgramSettingsObjectRepository = mock()
    private val dataSetRetentionLimitResolver: DataSetRetentionLimitResolver = mock()

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
        programRetentionLimitResolver = ProgramRetentionLimitResolver(programSettingsObjectRepository),
        dataSetRetentionLimitResolver = dataSetRetentionLimitResolver,
        retentionSelector = RetentionSelector(),
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

    @Test
    fun wrap_an_unexpected_failure_without_a_message_with_its_exception_type_instead_of_null() = runTest {
        givenTrackedEntityCandidatesThatFailWithoutAMessage()

        val actual =
            try {
                purger.purge()
                null
            } catch (d2Error: D2Error) {
                d2Error
            }

        assertThat(actual).isNotNull()
        assertThat(actual!!.errorDescription()).contains("NoSuchElementException")
    }

    @Test
    fun trim_tracked_entities_under_the_global_limit_when_none_of_them_has_a_program() = runTest {
        givenAGlobalTeiLimit(teiDBTrimming = 1, settingDBTrimming = LimitScope.PER_PROGRAM)
        givenTrackedEntityCandidates(
            givenACandidateWithoutProgram(uid = "oldestWithoutProgram", lastUpdated = "2026-01-01T00:00:00.000"),
            givenACandidateWithoutProgram(uid = "newestWithoutProgram", lastUpdated = "2026-02-01T00:00:00.000"),
        )
        givenNoEventOrDataValueCandidates()

        purger.purge()

        verifyBlocking(trackedEntityPurger) { purge(listOf("oldestWithoutProgram")) }
    }

    private fun givenTrackedEntityCandidatesThatFailWhilePurging(failureMessage: String) {
        trackedEntityPurger.stub {
            onBlocking { eligibleCandidates() } doThrow RuntimeException(failureMessage)
        }
    }

    private fun givenTrackedEntityCandidatesThatFailWithoutAMessage() {
        trackedEntityPurger.stub {
            onBlocking { eligibleCandidates() } doThrow NoSuchElementException()
        }
    }

    private fun givenAGlobalTeiLimit(teiDBTrimming: Int, settingDBTrimming: LimitScope) {
        val globalSetting = ProgramSetting.builder()
            .teiDBTrimming(teiDBTrimming)
            .settingDBTrimming(settingDBTrimming)
            .build()
        val programSettings = ProgramSettings.builder()
            .globalSettings(globalSetting)
            .specificSettings(emptyMap())
            .build()

        whenever(programSettingsObjectRepository.blockingGet()) doReturn programSettings
    }

    private fun givenTrackedEntityCandidates(vararg candidates: RetentionCandidate) {
        trackedEntityPurger.stub {
            onBlocking { eligibleCandidates() } doReturn candidates.toList()
        }
    }

    private fun givenNoEventOrDataValueCandidates() {
        eventPurger.stub {
            onBlocking { eligibleCandidates() } doReturn emptyList()
        }
        dataValuePurger.stub {
            onBlocking { eligibleCandidates() } doReturn emptyList()
        }
    }

    private fun givenACandidateWithoutProgram(uid: String, lastUpdated: String): RetentionCandidate {
        return RetentionCandidate.ByProgramAndOrgUnit(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
            programUids = emptyList(),
            organisationUnitUid = "orgUnit",
        )
    }
}
