package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.settings.LimitScope
import org.hisp.dhis.android.core.settings.ProgramSetting
import org.hisp.dhis.android.core.settings.ProgramSettingsObjectRepository
import org.koin.core.annotation.Singleton

internal data class ResolvedRetentionLimit(
    val limit: Int,
    val scope: LimitScope,
)

@Singleton
internal class ProgramRetentionLimitResolver(
    private val programSettingsObjectRepository: ProgramSettingsObjectRepository,
) {
    suspend fun resolve(
        programUid: String,
        limitExtractor: (ProgramSetting) -> Int?,
    ): ResolvedRetentionLimit {
        val programSettings = programSettingsObjectRepository.blockingGet()

        val specificSetting = programSettings?.specificSettings()?.get(programUid)
        val globalSetting = programSettings?.globalSettings()

        val limit = specificSetting?.let(limitExtractor)
            ?: globalSetting?.let(limitExtractor)
            ?: DEFAULT_LIMIT

        val scope = specificSetting?.settingDBTrimming()
            ?: globalSetting?.settingDBTrimming()
            ?: LimitScope.GLOBAL

        return ResolvedRetentionLimit(limit, scope)
    }

    companion object {
        const val DEFAULT_LIMIT = 500
    }
}
