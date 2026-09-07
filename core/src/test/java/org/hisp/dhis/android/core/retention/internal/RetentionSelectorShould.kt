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

    private fun givenACandidate(uid: String, lastUpdated: String): RetentionCandidate {
        return RetentionCandidate(
            uid = uid,
            lastUpdated = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").parse(lastUpdated),
        )
    }
}
