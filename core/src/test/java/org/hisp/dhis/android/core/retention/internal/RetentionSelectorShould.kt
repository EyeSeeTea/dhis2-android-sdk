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

    private fun givenACandidate(uid: String, lastUpdated: String): RetentionCandidate {
        return RetentionCandidate(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
        )
    }

    private fun givenACandidateForPrograms(
        uid: String,
        lastUpdated: String,
        programUids: List<String>,
    ): RetentionCandidate {
        return RetentionCandidate(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
            programUids = programUids,
        )
    }
}
