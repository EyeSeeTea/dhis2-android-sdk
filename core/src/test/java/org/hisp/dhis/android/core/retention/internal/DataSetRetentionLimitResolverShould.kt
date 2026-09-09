package org.hisp.dhis.android.core.retention.internal

import kotlinx.coroutines.test.runTest
import org.hisp.dhis.android.core.settings.DataSetSetting
import org.hisp.dhis.android.core.settings.DataSetSettings
import org.hisp.dhis.android.core.settings.DataSetSettingsObjectRepository
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(JUnit4::class)
class DataSetRetentionLimitResolverShould {

    private val dataSetSettingsObjectRepository: DataSetSettingsObjectRepository = mock()

    private val resolver = DataSetRetentionLimitResolver(dataSetSettingsObjectRepository)

    @Test
    fun use_specific_data_set_value_when_present() = runTest {
        val specificSetting = givenADataSetSetting(uid = "dataSet1", periodDSDBTrimming = 50)
        val globalSetting = givenAGlobalDataSetSetting(periodDSDBTrimming = 200)
        val dataSetSettings = givenADataSetSettings(
            globalSettings = globalSetting,
            specificSettings = mapOf("dataSet1" to specificSetting),
        )

        whenever(dataSetSettingsObjectRepository.blockingGet()) doReturn dataSetSettings

        val limit = resolver.resolve("dataSet1")

        assertEquals(50, limit)
    }

    @Test
    fun fall_back_to_global_settings_when_no_specific_setting_for_data_set() = runTest {
        val globalSetting = givenAGlobalDataSetSetting(periodDSDBTrimming = 200)
        val dataSetSettings = givenADataSetSettings(
            globalSettings = globalSetting,
            specificSettings = emptyMap(),
        )

        whenever(dataSetSettingsObjectRepository.blockingGet()) doReturn dataSetSettings

        val limit = resolver.resolve("unknownDataSet")

        assertEquals(200, limit)
    }

    @Test
    fun fall_back_to_hardcoded_default_when_no_global_or_specific_setting() = runTest {
        whenever(dataSetSettingsObjectRepository.blockingGet()) doReturn null

        val limit = resolver.resolve("anyDataSet")

        assertEquals(DataSetRetentionLimitResolver.DEFAULT_LIMIT, limit)
    }

    private fun givenADataSetSetting(uid: String, periodDSDBTrimming: Int): DataSetSetting {
        return DataSetSetting.builder()
            .uid(uid)
            .periodDSDBTrimming(periodDSDBTrimming)
            .build()
    }

    private fun givenAGlobalDataSetSetting(periodDSDBTrimming: Int): DataSetSetting {
        return DataSetSetting.builder()
            .periodDSDBTrimming(periodDSDBTrimming)
            .build()
    }

    private fun givenADataSetSettings(
        globalSettings: DataSetSetting?,
        specificSettings: Map<String, DataSetSetting>,
    ): DataSetSettings {
        return DataSetSettings.builder()
            .globalSettings(globalSettings)
            .specificSettings(specificSettings)
            .build()
    }
}
