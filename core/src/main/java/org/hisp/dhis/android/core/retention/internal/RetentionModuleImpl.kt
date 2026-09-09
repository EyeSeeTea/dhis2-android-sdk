package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.retention.RetentionModule
import org.koin.core.annotation.Singleton

@Singleton
internal class RetentionModuleImpl(
    private val syncedDataRetentionPurger: SyncedDataRetentionPurger,
) : RetentionModule {
    override suspend fun purge() = syncedDataRetentionPurger.purge()
}
