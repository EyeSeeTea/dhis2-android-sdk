package org.hisp.dhis.android.core.retention.internal

import org.koin.core.annotation.Singleton

@Singleton
internal class RetentionSelector {

    fun select(candidates: List<RetentionCandidate>, limit: Int): List<String> {
        return candidates
            .sortedByDescending { it.lastUpdated }
            .drop(limit)
            .map { it.uid }
    }
}
