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
package org.hisp.dhis.android.core.trackedentity;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.hisp.dhis.android.core.calls.Call;
import org.hisp.dhis.android.core.common.APICallExecutor;
import org.hisp.dhis.android.core.common.D2CallException;
import org.hisp.dhis.android.core.common.GenericCallData;
import org.hisp.dhis.android.core.common.Payload;
import org.hisp.dhis.android.core.common.SyncCall;
import org.hisp.dhis.android.core.common.UidsCallFactory;
import org.hisp.dhis.android.core.data.api.Fields;
import org.hisp.dhis.android.core.data.database.DatabaseAdapter;
import org.hisp.dhis.android.core.data.database.Transaction;
import org.hisp.dhis.android.core.resource.ResourceHandler;
import org.hisp.dhis.android.core.resource.ResourceModel;
import org.hisp.dhis.android.core.resource.ResourceStore;
import org.hisp.dhis.android.core.resource.ResourceStoreImpl;

import java.util.Date;
import java.util.List;
import java.util.Set;

public class TrackedEntityTypeCall extends SyncCall<List<TrackedEntityType>> {

    private final TrackedEntityTypeService service;
    private final DatabaseAdapter databaseAdapter;
    private final TrackedEntityTypeStore trackedEntityTypeStore;
    private final ResourceStore resourceStore;
    private final Set<String> uidSet;
    private final Date serverDate;
    private final ResourceModel.Type resourceType = ResourceModel.Type.TRACKED_ENTITY;

    TrackedEntityTypeCall(@Nullable Set<String> uidSet,
                          @NonNull DatabaseAdapter databaseAdapter,
                          @NonNull TrackedEntityTypeStore trackedEntityTypeStore,
                          @NonNull ResourceStore resourceStore,
                          @NonNull TrackedEntityTypeService service,
                          @NonNull Date serverDate) {
        this.uidSet = uidSet;
        this.databaseAdapter = databaseAdapter;
        this.trackedEntityTypeStore = trackedEntityTypeStore;
        this.resourceStore = resourceStore;
        this.service = service;
        this.serverDate = new Date(serverDate.getTime());
    }

    @Override
    public List<TrackedEntityType> call() throws D2CallException {
        setExecuted();

        if (uidSet.size() > MAX_UIDS) {
            throw new IllegalArgumentException("Can't handle the amount of tracked entities: " + uidSet.size() + ". " +
                    "Max size is: " + MAX_UIDS);
        }
        ResourceHandler resourceHandler = new ResourceHandler(resourceStore);

        String lastUpdated = resourceHandler.getLastUpdated(resourceType);

        List<TrackedEntityType> trackedEntities
                = new APICallExecutor().executePayloadCall(getTrackedEntities(lastUpdated));

        TrackedEntityTypeHandler trackedEntityTypeHandler = new TrackedEntityTypeHandler(trackedEntityTypeStore);
        Transaction transaction = databaseAdapter.beginNewTransaction();
        try {

            int size = trackedEntities.size();

            for (int i = 0; i < size; i++) {
                TrackedEntityType trackedEntityType = trackedEntities.get(i);

                trackedEntityTypeHandler.handleTrackedEntity(trackedEntityType);
            }
            resourceHandler.handleResource(
                    resourceType,
                    serverDate
            );
            transaction.setSuccessful();
        } finally {
            transaction.end();
        }

        return trackedEntities;
    }

    private retrofit2.Call<Payload<TrackedEntityType>> getTrackedEntities(String lastUpdated) {
        return service.trackedEntities(
                Fields.<TrackedEntityType>builder().fields(
                        TrackedEntityType.uid, TrackedEntityType.code, TrackedEntityType.name,
                        TrackedEntityType.displayName, TrackedEntityType.created, TrackedEntityType.lastUpdated,
                        TrackedEntityType.shortName, TrackedEntityType.displayShortName,
                        TrackedEntityType.description, TrackedEntityType.displayDescription,
                        TrackedEntityType.deleted
                ).build(),
                TrackedEntityType.uid.in(uidSet),
                TrackedEntityType.lastUpdated.gt(lastUpdated),
                false
        );
    }

    public static final UidsCallFactory<TrackedEntityType> FACTORY = new UidsCallFactory<TrackedEntityType>() {

        private static final int UID_LIMIT = 64;

        @Override
        protected int getUidLimit() {
            return UID_LIMIT;
        }

        @Override
        public Call<List<TrackedEntityType>> create(GenericCallData genericCallData, Set<String> uids) {
            return new TrackedEntityTypeCall(
                    uids,
                    genericCallData.databaseAdapter(),
                    new TrackedEntityTypeStoreImpl(genericCallData.databaseAdapter()),
                    new ResourceStoreImpl(genericCallData.databaseAdapter()),
                    genericCallData.retrofit().create(TrackedEntityTypeService.class),
                    genericCallData.serverDate()
            );
        }
    };
}
