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

package org.hisp.dhis.client.sdk.core.organisationunit;

import org.hisp.dhis.client.sdk.core.common.services.Get;
import org.hisp.dhis.client.sdk.core.common.services.ListAll;
import org.hisp.dhis.client.sdk.core.common.services.Remove;
import org.hisp.dhis.client.sdk.core.common.services.Save;
import org.hisp.dhis.client.sdk.core.common.services.Service;
import org.hisp.dhis.client.sdk.models.organisationunit.OrganisationUnitLevel;


public interface OrganisationUnitLevelService extends Service, Save<OrganisationUnitLevel>,
        Remove<OrganisationUnitLevel>, Get<OrganisationUnitLevel>,
        ListAll<OrganisationUnitLevel> {
}
