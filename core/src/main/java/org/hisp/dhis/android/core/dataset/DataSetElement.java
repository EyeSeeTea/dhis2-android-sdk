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

package org.hisp.dhis.android.core.dataset;

import android.database.Cursor;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.gabrielittner.auto.value.cursor.ColumnAdapter;
import com.google.auto.value.AutoValue;

import org.hisp.dhis.android.core.common.BaseModel;
import org.hisp.dhis.android.core.common.Model;
import org.hisp.dhis.android.core.common.ObjectWithUid;
import org.hisp.dhis.android.core.arch.db.adapters.custom.internal.ObjectWithUidColumnAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@AutoValue
@JsonDeserialize(builder = AutoValue_DataSetElement.Builder.class)
public abstract class DataSetElement implements Model {

    @NonNull
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid dataSet();

    @NonNull
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid dataElement();

    @Nullable
    @JsonProperty()
    @ColumnAdapter(ObjectWithUidColumnAdapter.class)
    public abstract ObjectWithUid categoryCombo();

    public static Builder builder() {
        return new $$AutoValue_DataSetElement.Builder();
    }

    public static DataSetElement create(Cursor cursor) {
        return $AutoValue_DataSetElement.createFromCursor(cursor);
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    @JsonPOJOBuilder(withPrefix = "")
    public static abstract class Builder extends BaseModel.Builder<Builder> {
        public abstract Builder id(Long id);

        public abstract Builder dataSet(ObjectWithUid dataSet);

        public abstract Builder dataElement(ObjectWithUid dataElement);

        public abstract Builder categoryCombo(ObjectWithUid categoryCombo);

        public abstract DataSetElement build();
    }
}