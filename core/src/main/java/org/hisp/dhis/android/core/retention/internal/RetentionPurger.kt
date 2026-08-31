package org.hisp.dhis.android.core.retention.internal

internal interface RetentionPurger {
    suspend fun purge(limit: Int)
}
