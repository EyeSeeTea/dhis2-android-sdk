package org.hisp.dhis.client.sdk.android.api.persistence.flow;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;

import org.hisp.dhis.client.sdk.android.api.persistence.DbDhis;
import org.hisp.dhis.client.sdk.android.common.AbsMapper;
import org.hisp.dhis.client.sdk.android.common.Mapper;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;

@Table(database = DbDhis.class)
public class OrganisationUnitLevelFlow extends BaseModelFlow {
    public static final Mapper<OrganisationUnitLevel, OrganisationUnitLevelFlow> MAPPER =
            new OrganisationUnitLevelMapper();

    @Column(name = "uId")
    String uId;

    @Column(name = "displayName")
    String displayName;

    @Column(name = "level")
    int level;

    public String getUId() {
        return uId;
    }

    public void setUId(String uId) {
        this.uId = uId;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public OrganisationUnitLevelFlow() {
    }

    private static class OrganisationUnitLevelMapper extends
            AbsMapper<OrganisationUnitLevel, OrganisationUnitLevelFlow> {

        @Override
        public OrganisationUnitLevelFlow mapToDatabaseEntity(
                OrganisationUnitLevel organisationUnitLevel) {
            if (organisationUnitLevel == null) {
                return null;
            }

            OrganisationUnitLevelFlow organisationUnitLevelFlow = new OrganisationUnitLevelFlow();
            organisationUnitLevelFlow.setUId(organisationUnitLevel.getUId());
            organisationUnitLevelFlow.setLevel(organisationUnitLevel.getLevel());
            organisationUnitLevelFlow.setDisplayName(organisationUnitLevel.getDisplayName());
            return organisationUnitLevelFlow;
        }

        @Override
        public OrganisationUnitLevel mapToModel(
                OrganisationUnitLevelFlow organisationUnitLevelFlow) {
            if (organisationUnitLevelFlow == null) {
                return null;
            }

            OrganisationUnitLevel organisationUnitLevel = new OrganisationUnitLevel();
            organisationUnitLevel.setUId(organisationUnitLevelFlow.getUId());
            organisationUnitLevel.setLevel(organisationUnitLevelFlow.getLevel());
            organisationUnitLevel.setDisplayName(organisationUnitLevelFlow.getDisplayName());

            return organisationUnitLevel;
        }

        @Override
        public Class<OrganisationUnitLevel> getModelTypeClass() {
            return OrganisationUnitLevel.class;
        }

        @Override
        public Class<OrganisationUnitLevelFlow> getDatabaseEntityTypeClass() {
            return OrganisationUnitLevelFlow.class;
        }
    }
}
