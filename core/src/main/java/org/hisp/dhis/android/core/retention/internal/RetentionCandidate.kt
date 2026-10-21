package org.hisp.dhis.android.core.retention.internal

import java.util.Date

internal sealed class RetentionCandidate {
    abstract val uid: String
    abstract val lastUpdated: Date?

    data class ByProgramAndOrgUnit(
        override val uid: String,
        override val lastUpdated: Date?,
        val programUids: List<String>,
        val organisationUnitUid: String,
    ) : RetentionCandidate()

    data class ByDataset(
        override val uid: String,
        override val lastUpdated: Date?,
        val dataSetUids: List<String>,
    ) : RetentionCandidate()
}
