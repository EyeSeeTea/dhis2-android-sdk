package org.hisp.dhis.android.core.retention

interface RetentionModule {
    suspend fun purge()
}
