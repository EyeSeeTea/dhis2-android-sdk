
package org.hisp.dhis.client.sdk.core.organisationunit;

import org.hisp.dhis.client.sdk.core.common.Fields;
import org.hisp.dhis.client.sdk.core.common.network.ApiException;
import org.hisp.dhis.client.sdk.core.common.persistence.DbOperation;
import org.hisp.dhis.client.sdk.core.common.persistence.DbOperationImpl;
import org.hisp.dhis.client.sdk.core.common.persistence.TransactionManager;
import org.hisp.dhis.client.sdk.core.common.preferences.DateType;
import org.hisp.dhis.client.sdk.core.common.preferences.LastUpdatedPreferences;
import org.hisp.dhis.client.sdk.core.common.preferences.ResourceType;
import org.hisp.dhis.client.sdk.core.systeminfo.SystemInfoController;
import org.hisp.dhis.client.sdk.core.user.UserApiClient;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class OrganisationUnitLevelControllerImpl
        implements OrganisationUnitLevelController {

    /* Controllers */
    private final SystemInfoController systemInfoController;

    /* Api clients */
    private final OrganisationUnitLevelApiClient organisationUnitLevelApiClient;
    private final UserApiClient userApiClient;

    private final OrganisationUnitLevelStore organisationUnitLevelStore;
    private final LastUpdatedPreferences lastUpdatedPreferences;
    /* Utilities */
    private final TransactionManager transactionManager;

    public OrganisationUnitLevelControllerImpl(SystemInfoController systemInfoController,
            OrganisationUnitLevelApiClient organisationUnitLevelApiClient,
            UserApiClient userApiClient,
            OrganisationUnitLevelStore organisationUnitLevelStore,
            LastUpdatedPreferences lastUpdatedPreferences,
            TransactionManager transactionManager) {
        this.organisationUnitLevelStore = organisationUnitLevelStore;
        this.systemInfoController = systemInfoController;
        this.organisationUnitLevelApiClient = organisationUnitLevelApiClient;
        this.userApiClient = userApiClient;
        this.lastUpdatedPreferences = lastUpdatedPreferences;
        this.transactionManager = transactionManager;
    }


    @Override
    public void pull() throws ApiException {

        DateTime serverTime = systemInfoController.getSystemInfo().getServerDate();
        List<OrganisationUnitLevel> allOrganisationUnitLevel = organisationUnitLevelApiClient
                .getOrganisationUnitLevels(Fields.ALL);

        List<DbOperation> dbOperations = new ArrayList<>();
        for (OrganisationUnitLevel organisationUnitLevel : allOrganisationUnitLevel) {
            dbOperations.add(DbOperationImpl.with(organisationUnitLevelStore)
                    .insert(organisationUnitLevel));
        }
        transactionManager.transact(dbOperations);

        lastUpdatedPreferences.save(ResourceType.ORGANISATION_UNIT_LEVEL, DateType.SERVER,
                serverTime);
    }
}
