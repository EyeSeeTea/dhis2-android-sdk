
package org.hisp.dhis.client.sdk.core.organisationunit;

import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;
import org.hisp.dhis.client.sdk.utils.Preconditions;

import java.util.List;

public class OrganisationUnitLevelServiceImpl implements OrganisationUnitLevelService {
    private final OrganisationUnitLevelStore organisationUnitLevelStore;

    public OrganisationUnitLevelServiceImpl(OrganisationUnitLevelStore organisationUnitLevelStore) {
        this.organisationUnitLevelStore = organisationUnitLevelStore;
    }


    @Override
    public OrganisationUnitLevel get(long id) {
        return organisationUnitLevelStore.queryById(id);
    }

    @Override
    public List<OrganisationUnitLevel> list() {
        return organisationUnitLevelStore.queryAll();
    }

    @Override
    public boolean remove(OrganisationUnitLevel object) {
        Preconditions.isNull(object, "Object must not be null");
        return organisationUnitLevelStore.delete(object);
    }

    @Override
    public boolean save(OrganisationUnitLevel object) {
        Preconditions.isNull(object, "Object must not be null");
        return organisationUnitLevelStore.save(object);
    }
}