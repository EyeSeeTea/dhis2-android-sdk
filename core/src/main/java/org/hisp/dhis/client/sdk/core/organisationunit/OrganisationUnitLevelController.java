package org.hisp.dhis.client.sdk.core.organisationunit;

import org.hisp.dhis.client.sdk.core.common.network.ApiException;

public interface OrganisationUnitLevelController {
    void pull() throws ApiException;
}
