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
        candidates: List<RetentionCandidate.ByProgramAndOrgUnit>,
        limitByProgram: Map<String, Int>,
    ): List<String> {
        return candidates
            .groupBy { candidate -> candidate.programUids.minBy { limitByProgram.getValue(it) } }
            .flatMap { (programUid, group) ->
                group.sortedByDescending { it.lastUpdated }.drop(limitByProgram.getValue(programUid)).map { it.uid }
            }
    }

    fun selectByOrgUnit(
        candidates: List<RetentionCandidate.ByProgramAndOrgUnit>,
        limitByOrgUnit: Map<String, Int>,
    ): List<String> {
        return candidates
            .groupBy { it.organisationUnitUid }
            .flatMap { (orgUnitUid, group) ->
                group.sortedByDescending { it.lastUpdated }.drop(limitByOrgUnit.getValue(orgUnitUid)).map { it.uid }
            }
    }

    fun selectByDataset(
        candidates: List<RetentionCandidate.ByDataset>,
        limitByDataset: Map<String, Int>,
    ): List<String> {
        return candidates
            .groupBy { candidate -> candidate.dataSetUids.minBy { limitByDataset.getValue(it) } }
            .flatMap { (dataSetUid, group) ->
                group.sortedByDescending { it.lastUpdated }.drop(limitByDataset.getValue(dataSetUid)).map { it.uid }
            }
    }
}
