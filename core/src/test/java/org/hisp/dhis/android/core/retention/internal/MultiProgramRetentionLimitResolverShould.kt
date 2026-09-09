package org.hisp.dhis.android.core.retention.internal

import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.settings.LimitScope
import org.hisp.dhis.android.core.settings.ProgramSetting
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class MultiProgramRetentionLimitResolverShould {

    private val programRetentionLimitResolver: ProgramRetentionLimitResolver = mock()

    private val resolver = MultiProgramRetentionLimitResolver(programRetentionLimitResolver)

    private val anyLimitExtractor: (ProgramSetting) -> Int? = { it.teiDBTrimming() }

    @Test
    fun use_the_smallest_resolved_limit_across_all_enrolled_programs() = runTest {
        whenever(programRetentionLimitResolver.resolve(eq("program1"), any())) doReturn
            ResolvedRetentionLimit(50, LimitScope.PER_PROGRAM)
        whenever(programRetentionLimitResolver.resolve(eq("program2"), any())) doReturn
            ResolvedRetentionLimit(200, LimitScope.PER_PROGRAM)

        val result = resolver.resolve(listOf("program1", "program2"), anyLimitExtractor)

        assertEquals(50, result.limit)
        assertEquals(LimitScope.PER_PROGRAM, result.scope)
    }

    @Test
    fun return_the_single_program_resolved_limit_when_enrolled_in_only_one_program() = runTest {
        whenever(programRetentionLimitResolver.resolve(eq("program1"), any())) doReturn
            ResolvedRetentionLimit(50, LimitScope.PER_PROGRAM)

        val result = resolver.resolve(listOf("program1"), anyLimitExtractor)

        assertEquals(50, result.limit)
        assertEquals(LimitScope.PER_PROGRAM, result.scope)
    }
}
