package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.settings.DataSetSettingsObjectRepository
import org.koin.core.annotation.Singleton

@Singleton
internal class DataSetRetentionLimitResolver(
    private val dataSetSettingsObjectRepository: DataSetSettingsObjectRepository,
) {
    suspend fun resolve(dataSetUid: String): Int {
        val dataSetSettings = dataSetSettingsObjectRepository.blockingGet()

        val specificSetting = dataSetSettings?.specificSettings()?.get(dataSetUid)
        val globalSetting = dataSetSettings?.globalSettings()

        return specificSetting?.periodDSDBTrimming()
            ?: globalSetting?.periodDSDBTrimming()
            ?: DEFAULT_LIMIT
    }

    companion object {
        const val DEFAULT_LIMIT = 500
    }
}
