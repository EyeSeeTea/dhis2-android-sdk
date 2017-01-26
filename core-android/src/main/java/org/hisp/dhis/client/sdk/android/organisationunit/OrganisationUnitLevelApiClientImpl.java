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

import static org.hisp.dhis.client.sdk.android.api.network.NetworkUtils.getCollection;

import org.hisp.dhis.client.sdk.android.api.network.ApiResource;
import org.hisp.dhis.client.sdk.core.common.Fields;
import org.hisp.dhis.client.sdk.core.common.network.ApiException;
import org.hisp.dhis.client.sdk.core.organisationunit.OrganisationUnitLevelApiClient;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;

import java.util.List;
import java.util.Map;

import retrofit2.Call;

public class OrganisationUnitLevelApiClientImpl implements OrganisationUnitLevelApiClient {
    private final OrganisationUnitLevelApiClientRetrofit organisationUnitLevelApiClientRetrofit;

    public OrganisationUnitLevelApiClientImpl(
            OrganisationUnitLevelApiClientRetrofit organisationUnitLevelApiClientRetrofit) {
        this.organisationUnitLevelApiClientRetrofit = organisationUnitLevelApiClientRetrofit;
    }

    @Override
    public List<OrganisationUnitLevel> getOrganisationUnitLevels(Fields fields)
            throws ApiException {
        ApiResource<OrganisationUnitLevel> apiResource = new ApiResource<OrganisationUnitLevel>() {

            @Override
            public String getResourceName() {
                return "organisationUnitLevels";
            }

            @Override
            public String getBasicProperties() {
                return "id";
            }

            @Override
            public String getAllProperties() {
                return "id,displayName,level";
            }

            @Override
            public String getDescendantProperties() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Call<Map<String, List<OrganisationUnitLevel>>> getEntities(
                    Map<String, String> queryMap, List<String> filters) throws ApiException {
                return organisationUnitLevelApiClientRetrofit.getOrganisationUnitLevels(queryMap,
                        filters);
            }
        };

        return getCollection(apiResource, fields, null, null);
    }
}
