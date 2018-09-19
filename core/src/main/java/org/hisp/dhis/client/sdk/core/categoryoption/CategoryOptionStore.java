
package org.hisp.dhis.client.sdk.core.categoryoption;

import org.hisp.dhis.client.sdk.core.common.persistence.Store;
import org.hisp.dhis.client.sdk.models.category.CategoryOption;

public interface CategoryOptionStore extends Store<CategoryOption> {
    CategoryOption queryDefault();
}
