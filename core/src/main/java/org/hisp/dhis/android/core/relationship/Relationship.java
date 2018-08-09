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

package org.hisp.dhis.android.core.relationship;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.google.auto.value.AutoValue;

import org.hisp.dhis.android.core.trackedentity.TrackedEntityInstance;

@AutoValue
@JsonInclude(Include.NON_NULL)
@JsonDeserialize(builder = AutoValue_Relationship.Builder.class)
public abstract class Relationship {

    @Nullable
    public abstract String trackedEntityInstanceA();

    @Nullable
    public abstract String trackedEntityInstanceB();

    public abstract String relationship();

    @Nullable
    public abstract String relationshipType();

    @Nullable
    public abstract String displayName();

    @Nullable
    public abstract TrackedEntityInstance relative();

    @Nullable
    public abstract RelationshipItem from();

    @Nullable
    public abstract RelationshipItem to();

    public static Builder builder() {
        return new AutoValue_Relationship.Builder();
    }

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder {
        public abstract Builder trackedEntityInstanceA(String trackedEntityInstanceA);

        public abstract Builder trackedEntityInstanceB(String trackedEntityInstanceB);

        public abstract Builder relationship(String relationship);

        public abstract Builder relationshipType(String relationshipType);

        public abstract Builder displayName(String displayName);

        public abstract Builder relative(TrackedEntityInstance relative);

        public abstract Builder from(RelationshipItem from);

        public abstract Builder to(RelationshipItem to);

        public abstract Relationship build();
    }
}
