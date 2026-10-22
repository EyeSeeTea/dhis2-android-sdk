package org.hisp.dhis.android.core.retention.internal

import org.hisp.dhis.android.core.arch.ArchDIModule
import org.hisp.dhis.android.core.settings.SettingsDIModule
import org.hisp.dhis.android.persistence.dependencyinjection.PersistenceDIModule
import org.koin.core.annotation.ComponentScan
import org.koin.core.annotation.Module

// Koin compile safety validates this module on its own: include the modules that declare its stores,
// settings repositories and call executor.
@Module(
    includes = [
        ArchDIModule::class,
        PersistenceDIModule::class,
        SettingsDIModule::class,
    ],
)
@ComponentScan
internal class RetentionDIModule
