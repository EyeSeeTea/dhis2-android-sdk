package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.settings.ProgramSetting
import org.koin.core.annotation.Singleton

@Singleton
internal class MultiProgramRetentionLimitResolver(
    private val programRetentionLimitResolver: ProgramRetentionLimitResolver,
) {
    suspend fun resolve(
        programUids: List<String>,
        limitExtractor: (ProgramSetting) -> Int?,
    ): ResolvedRetentionLimit {
        return programUids
            .map { programRetentionLimitResolver.resolve(it, limitExtractor) }
            .minBy { it.limit }
    }
}
