/*
 * Copyright (c) 2017, University of Oslo
 *
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.hisp.dhis.android.core;

import org.hisp.dhis.android.core.data.database.DatabaseAdapter;
import org.hisp.dhis.android.core.dataelement.DataElementInternalModule;
import org.hisp.dhis.android.core.datavalue.DataValueInternalModule;
import org.hisp.dhis.android.core.relationship.RelationshipInternalModule;
import org.hisp.dhis.android.core.systeminfo.SystemInfoInternalModule;
import org.hisp.dhis.android.core.wipe.WipeableModule;

import java.util.Arrays;
import java.util.List;

import retrofit2.Retrofit;

public final class D2InternalModules {
    public final SystemInfoInternalModule systemInfo;
    public final RelationshipInternalModule relationshipModule;
    public final DataElementInternalModule dataElementModule;
    public final DataValueInternalModule dataValueModule;

    public D2InternalModules(SystemInfoInternalModule systemInfo,
                             RelationshipInternalModule relationshipModule,
                             DataElementInternalModule dataElementModule,
                             DataValueInternalModule dataValueModule) {
        this.systemInfo = systemInfo;
        this.relationshipModule = relationshipModule;
        this.dataElementModule = dataElementModule;
        this.dataValueModule = dataValueModule;
    }

    public List<WipeableModule> getWipeableModules() {
        return Arrays.asList(systemInfo, relationshipModule, dataElementModule, dataValueModule);
    }

    public static D2InternalModules create(DatabaseAdapter databaseAdapter, Retrofit retrofit) {
        SystemInfoInternalModule systemInfoInternalModule = SystemInfoInternalModule.create(databaseAdapter, retrofit);
        return new D2InternalModules(
                systemInfoInternalModule,
                RelationshipInternalModule.create(
                        databaseAdapter,
                        systemInfoInternalModule.publicModule.versionManager),
                DataElementInternalModule.create(databaseAdapter),
                DataValueInternalModule.create(databaseAdapter)
        );
    }
}