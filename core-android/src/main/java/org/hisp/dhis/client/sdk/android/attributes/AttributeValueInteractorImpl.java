/*
 * Copyright (c) 2017.
 *
 * This file is part of QA App.
 *
 *  Health Network QIS App is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Health Network QIS App is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.hisp.dhis.client.sdk.android.attributes;

import org.hisp.dhis.client.sdk.android.api.utils.DefaultOnSubscribe;
import org.hisp.dhis.client.sdk.core.attribute.AttributeValueService;
import org.hisp.dhis.client.sdk.models.attribute.AttributeValue;

import java.util.List;

import rx.Observable;

public class AttributeValueInteractorImpl implements AttributeValueInteractor {
    private final AttributeValueService attributeValueService;

    public AttributeValueInteractorImpl(
            AttributeValueService attributeValueService) {
        this.attributeValueService = attributeValueService;
    }

    @Override
    public Observable<List<AttributeValue>> pull() {
        return Observable.create(new DefaultOnSubscribe<List<AttributeValue>>() {
            @Override
            public List<AttributeValue> call() {
                return attributeValueService.list();
            }
        });
    }

    @Override
    public Observable<AttributeValue> get(final long id) {
        return Observable.create(new DefaultOnSubscribe<AttributeValue>() {
            @Override
            public AttributeValue call() {
                return attributeValueService.get(id);
            }
        });
    }

    @Override
    public Observable<List<AttributeValue>> list() {
        return Observable.create(new DefaultOnSubscribe<List<AttributeValue>>() {
            @Override
            public List<AttributeValue> call() {
                return attributeValueService.list();
            }
        });
    }
}