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

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gabrielittner.auto.value.cursor.ColumnName;
import com.google.auto.value.AutoValue;

import org.hisp.dhis.android.core.common.BaseDataModel;
import org.hisp.dhis.android.core.common.BaseModel;
import org.hisp.dhis.android.core.common.UpdateWhereStatementBinder;
import org.hisp.dhis.android.core.utils.Utils;

import static org.hisp.dhis.android.core.utils.StoreUtils.sqLiteBind;

@AutoValue
public abstract class RelationshipModel extends BaseDataModel implements UpdateWhereStatementBinder {
    public static final String TABLE = "Relationship";

    public static class Columns extends BaseDataModel.Columns {
        public static final String TRACKED_ENTITY_INSTANCE_A = "trackedEntityInstanceA";
        public static final String TRACKED_ENTITY_INSTANCE_B = "trackedEntityInstanceB";
        public static final String RELATIONSHIP_TYPE = "relationshipType";

        private Columns() {}

        public static String[] all() {
            return Utils.appendInNewArray(BaseModel.Columns.all(),
                    TRACKED_ENTITY_INSTANCE_A, TRACKED_ENTITY_INSTANCE_B, RELATIONSHIP_TYPE);
        }

        static String[] whereUpdate() {
            return new String[]{TRACKED_ENTITY_INSTANCE_A, TRACKED_ENTITY_INSTANCE_B, RELATIONSHIP_TYPE};
        }
    }

    public static RelationshipModel create(Cursor cursor) {
        return AutoValue_RelationshipModel.createFromCursor(cursor);
    }

    public static Builder builder() {
        return new $$AutoValue_RelationshipModel.Builder();
    }

    @NonNull
    public abstract ContentValues toContentValues();

    @Nullable
    @ColumnName(Columns.TRACKED_ENTITY_INSTANCE_A)
    public abstract String trackedEntityInstanceA();

    @Nullable
    @ColumnName(Columns.TRACKED_ENTITY_INSTANCE_B)
    public abstract String trackedEntityInstanceB();

    @Nullable
    @ColumnName(Columns.RELATIONSHIP_TYPE)
    public abstract String relationshipType();

    @Override
    public void bindToStatement(@NonNull SQLiteStatement sqLiteStatement) {
        sqLiteBind(sqLiteStatement, 1, trackedEntityInstanceA());
        sqLiteBind(sqLiteStatement, 2, trackedEntityInstanceB());
        sqLiteBind(sqLiteStatement, 3, relationshipType());
    }

    @Override
    public void bindToUpdateWhereStatement(@NonNull SQLiteStatement sqLiteStatement) {
        sqLiteBind(sqLiteStatement, 4, trackedEntityInstanceA());
        sqLiteBind(sqLiteStatement, 5, trackedEntityInstanceB());
        sqLiteBind(sqLiteStatement, 6, relationshipType());
    }

    @AutoValue.Builder
    public static abstract class Builder extends BaseDataModel.Builder<Builder> {
        public abstract Builder trackedEntityInstanceA(@Nullable String trackedEntityInstanceA);

        public abstract Builder trackedEntityInstanceB(@Nullable String trackedEntityInstanceB);

        public abstract Builder relationshipType(@Nullable String relationshipType);

        public abstract RelationshipModel build();
    }
}
