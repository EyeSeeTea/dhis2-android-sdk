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

package org.hisp.dhis.android.core.event;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.hisp.dhis.android.core.common.DeletableStore;
import org.hisp.dhis.android.core.common.State;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface EventStore extends DeletableStore {
    long insert(@NonNull String uid,
                @Nullable String enrollmentUid,
                @NonNull Date created,
                @NonNull Date lastUpdated,
                @Nullable String createdAtClient,
                @Nullable String lastUpdatedAtClient,
                @NonNull EventStatus status,
                @Nullable String latitude,
                @Nullable String longitude,
                @NonNull String program,
                @NonNull String programStage,
                @NonNull String organisationUnit,
                @NonNull Date eventDate,
                @Nullable Date completedDate,
                @Nullable Date dueDate,
            @NonNull State state,
            @Nullable String attributeCategoryOptions,
            @NonNull String attributeOptionCombo,
            @Nullable String trackedEntityInstance
    );

    int update(@NonNull String uid,
               @Nullable String enrollmentUid,
               @NonNull Date created,
               @NonNull Date lastUpdated,
               @Nullable String createdAtClient,
               @Nullable String lastUpdatedAtClient,
               @NonNull EventStatus eventStatus,
               @Nullable String latitude,
               @Nullable String longitude,
               @NonNull String program,
               @NonNull String programStage,
               @NonNull String organisationUnit,
               @NonNull Date eventDate,
               @Nullable Date completedDate,
               @Nullable Date dueDate,
               @NonNull State state,
            @Nullable String attributeCategoryOptions,
            @NonNull String attributeOptionCombo,
            @Nullable String trackedEntityInstance,
               @NonNull String whereEventUid
    );

    int delete(@NonNull String uid);

    int setState(@NonNull String uid, @NonNull State state);

    Map<String, List<Event>> queryEventsAttachedToEnrollmentToPost();

    List<Event> querySingleEventsToPost();

    List<Event> querySingleEvents();

    List<Event> queryAll();
}
