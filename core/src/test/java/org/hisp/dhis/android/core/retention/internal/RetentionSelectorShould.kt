package org.hisp.dhis.android.core.retention.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.text.SimpleDateFormat

@RunWith(JUnit4::class)
class RetentionSelectorShould {

    private val selector = RetentionSelector()

    @Test
    fun select_the_oldest_candidates_beyond_the_limit_from_one_combined_pool() {
        val oldest = givenACandidate("oldest", "2026-01-01T00:00:00.000")
        val middle = givenACandidate("middle", "2026-02-01T00:00:00.000")
        val newest = givenACandidate("newest", "2026-03-01T00:00:00.000")

        val toPurge = selector.select(candidates = listOf(oldest, middle, newest), limit = 1)

        assertEquals(listOf("middle", "oldest"), toPurge)
    }

    @Test
    fun trim_each_programs_excess_candidates_independently_of_the_other_programs_eligible_count() {
        val overLimitCandidate = givenACandidateForPrograms(
            uid = "overLimitCandidate",
            lastUpdated = "2026-01-01T00:00:00.000",
            programUids = listOf("programOverLimit"),
        )
        val withinLimitCandidate = givenACandidateForPrograms(
            uid = "withinLimitCandidate",
            lastUpdated = "2026-02-01T00:00:00.000",
            programUids = listOf("programWithinLimit"),
        )

        val toPurge = selector.selectByProgram(
            candidates = listOf(overLimitCandidate, withinLimitCandidate),
            limitByProgram = mapOf(
                "programOverLimit" to 0,
                "programWithinLimit" to 1,
            ),
        )

        assertEquals(listOf("overLimitCandidate"), toPurge)
    }

    @Test
    fun group_a_multi_program_candidate_under_its_most_restrictive_programs_limit() {
        val singleProgramCandidate = givenACandidateForPrograms(
            uid = "singleProgramCandidate",
            lastUpdated = "2026-01-01T00:00:00.000",
            programUids = listOf("restrictiveProgram"),
        )
        val multiProgramCandidate = givenACandidateForPrograms(
            uid = "multiProgramCandidate",
            lastUpdated = "2026-02-01T00:00:00.000",
            programUids = listOf("restrictiveProgram", "permissiveProgram"),
        )

        val toPurge = selector.selectByProgram(
            candidates = listOf(singleProgramCandidate, multiProgramCandidate),
            limitByProgram = mapOf(
                "restrictiveProgram" to 1,
                "permissiveProgram" to 5,
            ),
        )

        assertEquals(listOf("singleProgramCandidate"), toPurge)
    }

    @Test
    fun trim_each_org_units_excess_candidates_independently_of_the_other_org_units_eligible_count() {
        val overLimitCandidate = givenACandidateForOrgUnit(
            uid = "overLimitCandidate",
            lastUpdated = "2026-01-01T00:00:00.000",
            organisationUnitUid = "orgUnitOverLimit",
        )
        val withinLimitCandidate = givenACandidateForOrgUnit(
            uid = "withinLimitCandidate",
            lastUpdated = "2026-02-01T00:00:00.000",
            organisationUnitUid = "orgUnitWithinLimit",
        )

        val toPurge = selector.selectByOrgUnit(
            candidates = listOf(overLimitCandidate, withinLimitCandidate),
            limitByOrgUnit = mapOf(
                "orgUnitOverLimit" to 0,
                "orgUnitWithinLimit" to 1,
            ),
        )

        assertEquals(listOf("overLimitCandidate"), toPurge)
    }

    @Test
    fun trim_each_data_sets_excess_candidates_independently_of_the_other_data_sets_eligible_count() {
        val overLimitCandidate = givenACandidateForDataSet(
            uid = "overLimitCandidate",
            lastUpdated = "2026-01-01T00:00:00.000",
            dataSetUid = "dataSetOverLimit",
        )
        val withinLimitCandidate = givenACandidateForDataSet(
            uid = "withinLimitCandidate",
            lastUpdated = "2026-02-01T00:00:00.000",
            dataSetUid = "dataSetWithinLimit",
        )

        val toPurge = selector.selectByDataset(
            candidates = listOf(overLimitCandidate, withinLimitCandidate),
            limitByDataset = mapOf(
                "dataSetOverLimit" to 0,
                "dataSetWithinLimit" to 1,
            ),
        )

        assertEquals(listOf("overLimitCandidate"), toPurge)
    }

    @Test
    fun trim_each_org_unit_and_program_combinations_excess_candidates_independently() {
        val overLimitCandidate = givenACandidateForOrgUnitAndProgram(
            uid = "overLimitCandidate",
            lastUpdated = "2026-01-01T00:00:00.000",
            organisationUnitUid = "orgUnitA",
            programUid = "programOverLimit",
        )
        val withinLimitCandidate = givenACandidateForOrgUnitAndProgram(
            uid = "withinLimitCandidate",
            lastUpdated = "2026-02-01T00:00:00.000",
            organisationUnitUid = "orgUnitA",
            programUid = "programWithinLimit",
        )

        val toPurge = selector.selectByOrgUnitAndProgram(
            candidates = listOf(overLimitCandidate, withinLimitCandidate),
            limitByOrgUnitAndProgram = mapOf(
                ("orgUnitA" to "programOverLimit") to 0,
                ("orgUnitA" to "programWithinLimit") to 1,
            ),
        )

        assertEquals(listOf("overLimitCandidate"), toPurge)
    }

    @Test
    fun keep_org_units_of_the_same_program_as_separate_groups() {
        val orgUnitACandidate = givenACandidateForOrgUnitAndProgram(
            uid = "orgUnitACandidate",
            lastUpdated = "2026-01-01T00:00:00.000",
            organisationUnitUid = "orgUnitA",
            programUid = "sharedProgram",
        )
        val orgUnitBCandidate = givenACandidateForOrgUnitAndProgram(
            uid = "orgUnitBCandidate",
            lastUpdated = "2026-02-01T00:00:00.000",
            organisationUnitUid = "orgUnitB",
            programUid = "sharedProgram",
        )

        val toPurge = selector.selectByOrgUnitAndProgram(
            candidates = listOf(orgUnitACandidate, orgUnitBCandidate),
            limitByOrgUnitAndProgram = mapOf(
                ("orgUnitA" to "sharedProgram") to 0,
                ("orgUnitB" to "sharedProgram") to 1,
            ),
        )

        assertEquals(listOf("orgUnitACandidate"), toPurge)
    }

    @Test
    fun group_a_multi_program_candidate_under_its_org_units_most_restrictive_program_limit() {
        val singleProgramCandidate = givenACandidateForOrgUnitAndProgram(
            uid = "singleProgramCandidate",
            lastUpdated = "2026-01-01T00:00:00.000",
            organisationUnitUid = "orgUnitA",
            programUid = "restrictiveProgram",
        )
        val multiProgramCandidate = RetentionCandidate.ByProgramAndOrgUnit(
            uid = "multiProgramCandidate",
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse("2026-02-01T00:00:00.000"),
            programUids = listOf("restrictiveProgram", "permissiveProgram"),
            organisationUnitUid = "orgUnitA",
        )

        val toPurge = selector.selectByOrgUnitAndProgram(
            candidates = listOf(singleProgramCandidate, multiProgramCandidate),
            limitByOrgUnitAndProgram = mapOf(
                ("orgUnitA" to "restrictiveProgram") to 1,
                ("orgUnitA" to "permissiveProgram") to 5,
            ),
        )

        assertEquals(listOf("singleProgramCandidate"), toPurge)
    }

    private fun givenACandidate(uid: String, lastUpdated: String): RetentionCandidate {
        return RetentionCandidate.ByProgramAndOrgUnit(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
            programUids = emptyList(),
            organisationUnitUid = "orgUnit",
        )
    }

    private fun givenACandidateForPrograms(
        uid: String,
        lastUpdated: String,
        programUids: List<String>,
    ): RetentionCandidate.ByProgramAndOrgUnit {
        return RetentionCandidate.ByProgramAndOrgUnit(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
            programUids = programUids,
            organisationUnitUid = "orgUnit",
        )
    }

    private fun givenACandidateForOrgUnit(
        uid: String,
        lastUpdated: String,
        organisationUnitUid: String,
    ): RetentionCandidate.ByProgramAndOrgUnit {
        return RetentionCandidate.ByProgramAndOrgUnit(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
            programUids = emptyList(),
            organisationUnitUid = organisationUnitUid,
        )
    }

    private fun givenACandidateForOrgUnitAndProgram(
        uid: String,
        lastUpdated: String,
        organisationUnitUid: String,
        programUid: String,
    ): RetentionCandidate.ByProgramAndOrgUnit {
        return RetentionCandidate.ByProgramAndOrgUnit(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
            programUids = listOf(programUid),
            organisationUnitUid = organisationUnitUid,
        )
    }

    private fun givenACandidateForDataSet(
        uid: String,
        lastUpdated: String,
        dataSetUid: String,
    ): RetentionCandidate.ByDataset {
        return RetentionCandidate.ByDataset(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
            dataSetUids = listOf(dataSetUid),
        )
    }
}
