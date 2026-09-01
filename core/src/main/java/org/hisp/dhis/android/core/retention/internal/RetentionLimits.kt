package org.hisp.dhis.android.core.retention.internal

internal data class RetentionLimits(
    val dataValue: Int,
    val trackedEntityInstance: Int,
    val event: Int,
    val fileResource: Int,
)
