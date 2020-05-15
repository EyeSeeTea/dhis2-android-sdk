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

package org.hisp.dhis.android.core.data.dataelement;

import org.hisp.dhis.android.core.arch.helpers.UidGeneratorImpl;
import org.hisp.dhis.android.core.common.ObjectWithUid;
import org.hisp.dhis.android.core.common.ValueType;
import org.hisp.dhis.android.core.dataelement.DataElement;

import static org.hisp.dhis.android.core.data.utils.FillPropertiesTestUtils.fillNameableProperties;

public class DataElementSamples {

    public static DataElement getDataElement() {
        DataElement.Builder dataElementBuilder = DataElement.builder();

        fillNameableProperties(dataElementBuilder);
        dataElementBuilder
                .id(1L)
                .formName("form-name")
                .displayFormName("display-form-name")
                .valueType(ValueType.TEXT)
                .zeroIsSignificant(Boolean.TRUE)
                .domainType("TRACKER")
                .aggregationType("AVERAGE")
                .optionSet(ObjectWithUid.create("bWuNrMHEoZ0"))
                .categoryCombo(ObjectWithUid.create("cWuNrMHEoZ1"))
                .fieldMask("XXXX");
        return dataElementBuilder.build();
    }

    public static DataElement getDataElement(String name, ObjectWithUid optionSet, ObjectWithUid categoryCombo) {
        return getDataElement().toBuilder()
                .uid(new UidGeneratorImpl().generate())
                .name(name)
                .optionSet(optionSet)
                .categoryCombo(categoryCombo)
                .build();
    }
}