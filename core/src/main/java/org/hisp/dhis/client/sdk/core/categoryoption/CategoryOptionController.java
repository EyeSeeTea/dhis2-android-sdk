
package org.hisp.dhis.client.sdk.core.categoryoption;

import org.hisp.dhis.client.sdk.core.common.network.ApiException;

public interface CategoryOptionController {
    void pull() throws ApiException;
    void pullDefault()throws ApiException;
}
