package org.hisp.dhis.android.core.retention.internal

import java.util.Date

internal data class RetentionCandidate(
    val uid: String,
    val lastUpdated: Date?,
    val programUid: String? = null,
)
