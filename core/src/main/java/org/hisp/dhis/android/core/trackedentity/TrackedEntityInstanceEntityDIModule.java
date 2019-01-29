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

import org.hisp.dhis.android.core.arch.repositories.collection.ReadOnlyWithUploadWithUidCollectionRepository;
import org.hisp.dhis.android.core.common.BaseDataModel;
import org.hisp.dhis.android.core.common.DataOrphanCleanerImpl;
import org.hisp.dhis.android.core.common.OrphanCleaner;
import org.hisp.dhis.android.core.data.database.DatabaseAdapter;
import org.hisp.dhis.android.core.enrollment.Enrollment;
import org.hisp.dhis.android.core.enrollment.EnrollmentFields;
import org.hisp.dhis.android.core.enrollment.EnrollmentTableInfo;

import dagger.Module;
import dagger.Provides;
import dagger.Reusable;
import retrofit2.Retrofit;

@Module
public final class TrackedEntityInstanceEntityDIModule {

    @Provides
    @Reusable
    public TrackedEntityInstanceStore store(DatabaseAdapter databaseAdapter) {
        return TrackedEntityInstanceStoreImpl.create(databaseAdapter);
    }

    @Provides
    @Reusable
    ReadOnlyWithUploadWithUidCollectionRepository<TrackedEntityInstance> repository(
            TrackedEntityInstanceCollectionRepository impl) {
        return impl;
    }

    @Provides
    @Reusable
    TrackedEntityInstanceService service(Retrofit retrofit) {
        return retrofit.create(TrackedEntityInstanceService.class);
    }

    @Provides
    @Reusable
    TrackedEntityInstanceUidHelper uidHelper(TrackedEntityInstanceUidHelperImpl impl) {
        return impl;
    }

    @Provides
    @Reusable
    OrphanCleaner<TrackedEntityInstance, Enrollment> enrollmentOrphanCleaner(DatabaseAdapter databaseAdapter) {
        return new DataOrphanCleanerImpl<>(EnrollmentTableInfo.TABLE_INFO.name(),
                EnrollmentFields.TRACKED_ENTITY_INSTANCE, BaseDataModel.Columns.STATE, databaseAdapter);
    }
}