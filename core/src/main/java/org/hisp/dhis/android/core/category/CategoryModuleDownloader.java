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
package org.hisp.dhis.android.core.category;

import org.hisp.dhis.android.core.arch.modules.MetadataModuleDownloader;
import org.hisp.dhis.android.core.calls.factories.UidsCallFactory;
import org.hisp.dhis.android.core.common.Unit;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import dagger.Reusable;

@Reusable
public class CategoryModuleDownloader implements MetadataModuleDownloader<Unit> {

    private final UidsCallFactory<Category> categoryCallFactory;
    private final UidsCallFactory<CategoryCombo> categoryComboCallFactory;
    private final CategoryComboUidsSeeker categoryComboUidsSeeker;

    @Inject
    CategoryModuleDownloader(UidsCallFactory<Category> categoryCallFactory,
                             UidsCallFactory<CategoryCombo> categoryComboCallFactory,
                             CategoryComboUidsSeeker categoryComboUidsSeeker) {
        this.categoryCallFactory = categoryCallFactory;
        this.categoryComboCallFactory = categoryComboCallFactory;
        this.categoryComboUidsSeeker = categoryComboUidsSeeker;
    }

    @Override
    public Callable<Unit> downloadMetadata() {
        return new Callable<Unit>() {
            @Override
            public Unit call() throws Exception {
                Set<String> comboUids = categoryComboUidsSeeker.seekUids();
                List<CategoryCombo> categoryCombos = categoryComboCallFactory.create(comboUids).call();
                categoryCallFactory.create(CategoryParentUidsHelper.getCategoryUids(categoryCombos)).call();

                return new Unit();
            }
        };
    }
}