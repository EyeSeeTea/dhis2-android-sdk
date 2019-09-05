/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
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

package org.hisp.dhis.android.core.program;

import android.database.Cursor;

import androidx.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.gabrielittner.auto.value.cursor.ColumnAdapter;
import com.google.auto.value.AutoValue;

import org.hisp.dhis.android.core.arch.db.adapters.enums.internal.ProgramRuleActionTypeColumnAdapter;
import org.hisp.dhis.android.core.common.BaseIdentifiableObject;
import org.hisp.dhis.android.core.common.Model;
import org.hisp.dhis.android.core.common.ObjectWithUid;
import org.hisp.dhis.android.core.arch.db.adapters.custom.internal.ObjectWithUidColumnAdapter;

@AutoValue
@JsonDeserialize(builder = $$AutoValue_ProgramRuleAction.Builder.class)
public abstract class ProgramRuleAction extends BaseIdentifiableObject implements Model {

    @Nullable
    @JsonProperty()
    public abstract String data();

    @Nullable
    @JsonProperty()
    public abstract String content();

    @Nullable
    @JsonProperty()
    public abstract String location();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid trackedEntityAttribute();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid programIndicator();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid programStageSection();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ProgramRuleActionTypeColumnAdapter.class)
    public abstract ProgramRuleActionType programRuleActionType();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid programStage();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid dataElement();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid programRule();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid option();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid optionGroup();

    public static ProgramRuleAction create(Cursor cursor) {
        return $AutoValue_ProgramRuleAction.createFromCursor(cursor);
    }

    public static Builder builder() {
        return new $$AutoValue_ProgramRuleAction.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public abstract static class Builder extends BaseIdentifiableObject.Builder<Builder> {
        public abstract Builder id(Long id);

        public abstract Builder data(String data);

        public abstract Builder content(String content);

        public abstract Builder location(String location);

        public abstract Builder trackedEntityAttribute(ObjectWithUid trackedEntityAttribute);

        public abstract Builder programIndicator(ObjectWithUid programIndicator);

        public abstract Builder programStageSection(ObjectWithUid programStageSection);

        public abstract Builder programRuleActionType(ProgramRuleActionType programRuleActionType);

        public abstract Builder programStage(ObjectWithUid programStage);

        public abstract Builder dataElement(ObjectWithUid dataElement);

        public abstract Builder programRule(ObjectWithUid programRule);

        public abstract Builder option(ObjectWithUid option);

        public abstract Builder optionGroup(ObjectWithUid optionGroup);

        public abstract ProgramRuleAction build();
    }
}