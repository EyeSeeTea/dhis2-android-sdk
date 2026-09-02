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
        val specificSetting = givenAProgramSetting(
            uid = "program1",
            teiDBTrimming = 50,
            eventsDBTrimming = 30,
            settingDBTrimming = LimitScope.PER_PROGRAM,
        )
        val globalSetting = givenAGlobalProgramSetting(
            teiDBTrimming = 200,
            eventsDBTrimming = 200,
            settingDBTrimming = LimitScope.GLOBAL,
        )
        val programSettings = givenAProgramSettings(
            globalSettings = globalSetting,
            specificSettings = mapOf("program1" to specificSetting),
        )

        whenever(programSettingsObjectRepository.blockingGet()) doReturn programSettings

        val result = resolver.resolve("program1") { it.teiDBTrimming() }

        assertEquals(50, result.limit)
        assertEquals(LimitScope.PER_PROGRAM, result.scope)
    }

    @Test
    fun fall_back_to_global_settings_when_no_specific_setting_for_program() = runTest {
        val globalSetting = givenAGlobalProgramSetting(
            teiDBTrimming = 200,
            eventsDBTrimming = 150,
            settingDBTrimming = LimitScope.PER_ORG_UNIT,
        )
        val programSettings = givenAProgramSettings(
            globalSettings = globalSetting,
            specificSettings = emptyMap(),
        )

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
        val programSettings = givenAProgramSettings(
            globalSettings = null,
            specificSettings = mapOf("program1" to specificSetting),
        )

        whenever(programSettingsObjectRepository.blockingGet()) doReturn programSettings

        val result = resolver.resolve("program1") { it.teiDBTrimming() }

        assertEquals(50, result.limit)
        assertEquals(LimitScope.GLOBAL, result.scope)
    }

    private fun givenAProgramSetting(
        uid: String,
        teiDBTrimming: Int,
        eventsDBTrimming: Int,
        settingDBTrimming: LimitScope,
    ): ProgramSetting {
        return ProgramSetting.builder()
            .uid(uid)
            .teiDBTrimming(teiDBTrimming)
            .eventsDBTrimming(eventsDBTrimming)
            .settingDBTrimming(settingDBTrimming)
            .build()
    }

    private fun givenAGlobalProgramSetting(
        teiDBTrimming: Int,
        eventsDBTrimming: Int,
        settingDBTrimming: LimitScope,
    ): ProgramSetting {
        return ProgramSetting.builder()
            .teiDBTrimming(teiDBTrimming)
            .eventsDBTrimming(eventsDBTrimming)
            .settingDBTrimming(settingDBTrimming)
            .build()
    }

    private fun givenAProgramSettings(
        globalSettings: ProgramSetting?,
        specificSettings: Map<String, ProgramSetting>,
    ): ProgramSettings {
        return ProgramSettings.builder()
            .globalSettings(globalSettings)
            .specificSettings(specificSettings)
            .build()
    }
}
