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

import org.hisp.dhis.client.sdk.android.api.persistence.flow.OrganisationUnitLevelFlow;
import org.hisp.dhis.client.sdk.android.common.AbsStore;
import org.hisp.dhis.client.sdk.core.common.persistence.TransactionManager;
import org.hisp.dhis.client.sdk.core.organisationunit.OrganisationUnitLevelStore;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;

public class OrganisationUnitLevelStoreImpl extends
        AbsStore<OrganisationUnitLevel, OrganisationUnitLevelFlow> implements
        OrganisationUnitLevelStore {
    private final TransactionManager transactionManager;

    public OrganisationUnitLevelStoreImpl(TransactionManager transactionManager) {
        super(OrganisationUnitLevelFlow.MAPPER);

        this.transactionManager = transactionManager;
    }
}
