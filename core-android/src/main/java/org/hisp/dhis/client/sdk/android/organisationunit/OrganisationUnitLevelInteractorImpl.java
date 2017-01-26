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

package org.hisp.dhis.client.sdk.android.organisationunit;

import org.hisp.dhis.client.sdk.android.api.utils.DefaultOnSubscribe;
import org.hisp.dhis.client.sdk.core.organisationunit.OrganisationUnitLevelController;
import org.hisp.dhis.client.sdk.core.organisationunit.OrganisationUnitLevelService;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;

import java.util.List;

import rx.Observable;

public class OrganisationUnitLevelInteractorImpl implements OrganisationUnitLevelInteractor {
    private final OrganisationUnitLevelService organisationUnitLevelService;
    private final OrganisationUnitLevelController organisationUnitLevelController;

    public OrganisationUnitLevelInteractorImpl(
            OrganisationUnitLevelService organisationUnitLevelService,
            OrganisationUnitLevelController organisationUnitLevelController) {
        this.organisationUnitLevelService = organisationUnitLevelService;
        this.organisationUnitLevelController = organisationUnitLevelController;
    }

    @Override
    public Observable<OrganisationUnitLevel> get(final long id) {
        return Observable.create(new DefaultOnSubscribe<OrganisationUnitLevel>() {
            @Override
            public OrganisationUnitLevel call() {
                return organisationUnitLevelService.get(id);
            }
        });
    }

    @Override
    public Observable<List<OrganisationUnitLevel>> list() {
        return Observable.create(new DefaultOnSubscribe<List<OrganisationUnitLevel>>() {
            @Override
            public List<OrganisationUnitLevel> call() {
                return organisationUnitLevelService.list();
            }
        });
    }

    @Override
    public Observable<List<OrganisationUnitLevel>> pull() {
        return Observable.create(new DefaultOnSubscribe<List<OrganisationUnitLevel>>() {
            @Override
            public List<OrganisationUnitLevel> call() {
                organisationUnitLevelController.pull();
                return organisationUnitLevelService.list();
            }
        });
    }
}
