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

package org.hisp.dhis.android.core.dataset;

import android.database.MatrixCursor;
import android.support.test.runner.AndroidJUnit4;

import org.hisp.dhis.android.core.dataset.DataSetDataElementLinkModel.Columns;
import org.hisp.dhis.android.core.utils.ColumnsArrayUtils;
import org.hisp.dhis.android.core.utils.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class DataSetDataElementLinkModelShould {
    private DataSetDataElementLinkModel model;

    public DataSetDataElementLinkModelShould() {
        this.model = DataSetDataElementLinkModel.create(
                "data_set_uid", "data_element_uid",
                "category_combo_uid");
    }

    private Object[] getModelAsObjectArray() {
        return Utils.appendInNewArray(ColumnsArrayUtils.getModelAsObjectArray(model),
                model.dataSet(), model.dataElement(), model.categoryCombo());
    }

    @Test
    public void create_model_from_cursor() {
        MatrixCursor cursor = new MatrixCursor(ColumnsArrayUtils.getColumnsWithId(Columns.all()));
        cursor.addRow(getModelAsObjectArray());
        cursor.moveToFirst();

        DataSetDataElementLinkModel modelFromDB = DataSetDataElementLinkModel.create(cursor);
        cursor.close();

        assertThat(modelFromDB).isEqualTo(model);
    }

    @Test
    public void have_correct_number_of_columns() {
        assertThat(Columns.all().length).isEqualTo(3);
    }

    @Test
    public void have_data_set_data_element_model_columns() {
        List<String> columnsList = Arrays.asList(Columns.all());

        assertThat(columnsList.contains(Columns.DATA_SET)).isEqualTo(true);
        assertThat(columnsList.contains(Columns.DATA_ELEMENT)).isEqualTo(true);
        assertThat(columnsList.contains(Columns.CATEGORY_COMBO)).isEqualTo(true);
    }
}