package org.hisp.dhis.android.core.retention.internal

internal interface RetentionPurger {
    suspend fun eligibleCandidates(): List<RetentionCandidate>
    suspend fun purge(uids: List<String>)
}
