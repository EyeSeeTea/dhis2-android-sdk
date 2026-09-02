/*
 *  Copyright (c) 2004-2023, University of Oslo
 *  All rights reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *  Redistributions of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *  Neither the name of the HISP project nor the names of its contributors may
 *  be used to endorse or promote products derived from this software without
 *  specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.android.core.retention.internal

import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.settings.LimitScope
import org.hisp.dhis.android.core.settings.ProgramSetting
import org.hisp.dhis.android.core.settings.ProgramSettings
import org.hisp.dhis.android.core.settings.ProgramSettingsObjectRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class ProgramRetentionLimitResolverShould {

    private val programSettingsObjectRepository: ProgramSettingsObjectRepository = mock()

    private val resolver = ProgramRetentionLimitResolver(programSettingsObjectRepository)

    @Test
    fun use_specific_program_value_and_scope_when_present() = runTest {
        val specificSetting = ProgramSetting.builder()
            .uid("program1")
            .teiDBTrimming(50)
            .eventsDBTrimming(30)
            .settingDBTrimming(LimitScope.PER_PROGRAM)
            .build()

        val globalSetting = ProgramSetting.builder()
            .teiDBTrimming(200)
            .eventsDBTrimming(200)
            .settingDBTrimming(LimitScope.GLOBAL)
            .build()

        val programSettings = ProgramSettings.builder()
            .globalSettings(globalSetting)
            .specificSettings(mapOf("program1" to specificSetting))
            .build()

        whenever(programSettingsObjectRepository.blockingGet()) doReturn programSettings

        val result = resolver.resolve("program1") { it.teiDBTrimming() }

        assertEquals(50, result.limit)
        assertEquals(LimitScope.PER_PROGRAM, result.scope)
    }

    @Test
    fun fall_back_to_global_settings_when_no_specific_setting_for_program() = runTest {
        val globalSetting = ProgramSetting.builder()
            .teiDBTrimming(200)
            .eventsDBTrimming(150)
            .settingDBTrimming(LimitScope.PER_ORG_UNIT)
            .build()

        val programSettings = ProgramSettings.builder()
            .globalSettings(globalSetting)
            .specificSettings(emptyMap())
            .build()

        whenever(programSettingsObjectRepository.blockingGet()) doReturn programSettings

        val result = resolver.resolve("unknownProgram") { it.eventsDBTrimming() }

        assertEquals(150, result.limit)
        assertEquals(LimitScope.PER_ORG_UNIT, result.scope)
    }

    @Test
    fun fall_back_to_hardcoded_default_when_no_global_or_specific_setting() = runTest {
        whenever(programSettingsObjectRepository.blockingGet()) doReturn null

        val result = resolver.resolve("anyProgram") { it.teiDBTrimming() }

        assertEquals(ProgramRetentionLimitResolver.DEFAULT_LIMIT, result.limit)
        assertEquals(LimitScope.GLOBAL, result.scope)
    }

    @Test
    fun fall_back_to_global_scope_when_settingDBTrimming_scope_is_not_set() = runTest {
        val specificSetting = ProgramSetting.builder()
            .uid("program1")
            .teiDBTrimming(50)
            .build()

        val programSettings = ProgramSettings.builder()
            .globalSettings(null)
            .specificSettings(mapOf("program1" to specificSetting))
            .build()

        whenever(programSettingsObjectRepository.blockingGet()) doReturn programSettings

        val result = resolver.resolve("program1") { it.teiDBTrimming() }

        assertEquals(50, result.limit)
        assertEquals(LimitScope.GLOBAL, result.scope)
    }
}
