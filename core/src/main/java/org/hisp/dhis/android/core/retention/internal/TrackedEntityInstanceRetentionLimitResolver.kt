package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.settings.ProgramSetting
import org.koin.core.annotation.Singleton

@Singleton
internal class TrackedEntityInstanceRetentionLimitResolver(
    private val programRetentionLimitResolver: ProgramRetentionLimitResolver,
) {
    suspend fun resolve(programUids: List<String>): ResolvedRetentionLimit {
        return programUids
            .map { programRetentionLimitResolver.resolve(it) { setting: ProgramSetting -> setting.teiDBTrimming() } }
            .minBy { it.limit }
    }
}
