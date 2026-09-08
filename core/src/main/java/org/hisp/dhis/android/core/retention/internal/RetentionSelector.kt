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

    fun selectByProgram(
        candidates: List<RetentionCandidate>,
        limitByProgram: Map<String, Int>,
    ): List<String> {
        return candidates
            .groupBy { candidate -> candidate.programUids.minBy { limitByProgram.getValue(it) } }
            .flatMap { (programUid, group) ->
                group.sortedByDescending { it.lastUpdated }.drop(limitByProgram.getValue(programUid)).map { it.uid }
            }
    }
}
