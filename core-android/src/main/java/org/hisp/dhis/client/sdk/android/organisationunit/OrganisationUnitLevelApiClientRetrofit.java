package org.hisp.dhis.client.sdk.android.organisationunit;

import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface OrganisationUnitLevelApiClientRetrofit {

    @GET("organisationUnitLevels")
    Call<Map<String, List<OrganisationUnitLevel>>> getOrganisationUnitLevels(
            @QueryMap Map<String, String> queryMap, @Query("filter") List<String> filters);
}
