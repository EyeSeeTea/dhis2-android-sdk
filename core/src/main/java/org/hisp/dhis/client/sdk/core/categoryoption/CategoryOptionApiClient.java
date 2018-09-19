
package org.hisp.dhis.client.sdk.core.categoryoption;

import org.hisp.dhis.client.sdk.core.common.Fields;
import org.hisp.dhis.client.sdk.core.common.network.ApiException;
import org.hisp.dhis.client.sdk.models.attribute.Attribute;
import org.hisp.dhis.client.sdk.models.category.CategoryOption;
import org.joda.time.DateTime;

import java.util.List;
import java.util.Set;

public interface CategoryOptionApiClient {
    List<CategoryOption> getCategoryOptions(
            Fields fields, DateTime lastUpdated, Set<String> uids) throws ApiException;

    CategoryOption getDefaultCategoryOption() throws ApiException;
}
