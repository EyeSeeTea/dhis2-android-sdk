
package org.hisp.dhis.client.sdk.android.categoryoption;

import org.hisp.dhis.client.sdk.models.category.CategoryOption;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.QueryMap;

public interface CategoryOptionApiClientRetrofit {

    @GET("categoryOptions")
    Call<Map<String, List<CategoryOption>>> getCategoryOptions(
            @QueryMap Map<String, String> queryMap);

    @GET("categoryOptions?&filter=displayName:eq:default")
    Call<Map<String, List<CategoryOption>>> getDefaultCategoryOption(
            @QueryMap Map<String, String> queryMap);
}
