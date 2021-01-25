/*
 * Copyright (c) 2004-2019, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the HISP project nor the names of its contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.android.core.event.search

import dagger.Reusable
import javax.inject.Inject
import org.hisp.dhis.android.core.common.AssignedUserMode
import org.hisp.dhis.android.core.common.DateFilterPeriodHelper
import org.hisp.dhis.android.core.event.EventCollectionRepository
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitCollectionRepository
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitMode
import org.hisp.dhis.android.core.user.AuthenticatedUserObjectRepository

@Reusable
internal class EventCollectionRepositoryAdapter @Inject constructor(
    private val eventCollectionRepository: EventCollectionRepository,
    private val organisationUnitCollectionRepository: OrganisationUnitCollectionRepository,
    private val userRepository: AuthenticatedUserObjectRepository
) {

    @Suppress("ComplexMethod")
    fun getCollectionRepository(scope: EventQueryRepositoryScope): EventCollectionRepository {
        var repository = eventCollectionRepository

        scope.program()?.let { repository = repository.byProgramUid().eq(it) }
        scope.programStage()?.let { repository = repository.byProgramStageUid().eq(it) }
        scope.followUp()?.let { repository = repository.byFollowUp(it) }
        scope.trackedEntityInstance()?.let { repository = repository.byTrackedEntityInstanceUids(listOf(it)) }
        scope.organisationUnitMode().let { repository = applyOrgunitSelection(repository, scope) }
        scope.assignedUserMode()?.let { repository = applyUserAssignedMode(repository, it) }
        scope.dataFilters().forEach {
            // TODO
        }
        scope.events()?.let { repository = repository.byUid().`in`(it) }
        scope.eventStatus()?.let { repository = repository.byStatus().eq(it) }
        scope.eventDate()?.let { period ->
            DateFilterPeriodHelper.getStartDate(period)?.let { repository = repository.byEventDate().after(it) }
            DateFilterPeriodHelper.getEndDate(period)?.let { repository = repository.byEventDate().before(it) }
        }
        scope.dueDate()?.let { period ->
            DateFilterPeriodHelper.getStartDate(period)?.let { repository = repository.byDueDate().after(it) }
            DateFilterPeriodHelper.getEndDate(period)?.let { repository = repository.byDueDate().before(it) }
        }
        scope.lastUpdatedDate()?.let { period ->
            DateFilterPeriodHelper.getStartDate(period)?.let { repository = repository.byLastUpdated().after(it) }
            DateFilterPeriodHelper.getEndDate(period)?.let { repository = repository.byLastUpdated().before(it) }
        }
        scope.completedDate()?.let { period ->
            DateFilterPeriodHelper.getStartDate(period)?.let { repository = repository.byCompleteDate().after(it) }
            DateFilterPeriodHelper.getEndDate(period)?.let { repository = repository.byCompleteDate().before(it) }
        }
        scope.order().forEach { repository = applyOrderColumn(repository, it) }

        if (!scope.includeDeleted()) {
            repository = repository.byDeleted().isFalse
        }

        return repository
    }

    private fun applyOrgunitSelection(
        repository: EventCollectionRepository,
        scope: EventQueryRepositoryScope
    ): EventCollectionRepository {
        return getOrganisationUnits(scope)?.let { repository.byOrganisationUnitUid().`in`(it) } ?: repository
    }

    fun getOrganisationUnits(scope: EventQueryRepositoryScope): List<String>? {
        return when (scope.organisationUnitMode()) {
            OrganisationUnitMode.ALL, OrganisationUnitMode.ACCESSIBLE ->
                organisationUnitCollectionRepository.blockingGetUids()
            OrganisationUnitMode.CHILDREN ->
                scope.organisationUnit()?.let { orgUnit ->
                    organisationUnitCollectionRepository.byParentUid().like(orgUnit).blockingGetUids() + orgUnit
                }
            OrganisationUnitMode.DESCENDANTS ->
                scope.organisationUnit()?.let { orgUnit ->
                    organisationUnitCollectionRepository.byPath().like(orgUnit).blockingGetUids()
                }
            else ->
                scope.organisationUnit()?.let { listOf(it) }
        }
    }

    private fun applyOrderColumn(
        repository: EventCollectionRepository,
        order: EventQueryScopeOrderByItem
    ): EventCollectionRepository {
        return when (order.column()) {
            EventQueryScopeOrderColumn.EVENT_DATE -> repository.orderByEventDate(order.direction())
            EventQueryScopeOrderColumn.DUE_DATE -> repository.orderByDueDate(order.direction())
            EventQueryScopeOrderColumn.COMPLETED_DATE -> repository.orderByCompleteDate(order.direction())
            EventQueryScopeOrderColumn.CREATED -> repository.orderByCreated(order.direction())
            EventQueryScopeOrderColumn.LAST_UPDATED -> repository.orderByLastUpdated(order.direction())
            EventQueryScopeOrderColumn.ORGUNIT_NAME -> repository.orderByOrganisationUnitName(order.direction())
            EventQueryScopeOrderColumn.TIMELINE -> repository.orderByTimeline(order.direction())
            else -> repository
        }
    }

    private fun applyUserAssignedMode(
        repository: EventCollectionRepository,
        mode: AssignedUserMode
    ): EventCollectionRepository {
        return when (mode) {
            AssignedUserMode.CURRENT -> repository.byAssignedUser().eq(userRepository.blockingGet().user())
            AssignedUserMode.ANY -> repository.byAssignedUser().isNotNull
            AssignedUserMode.NONE -> repository.byAssignedUser().isNull
            // TODO Not implemented yet
            AssignedUserMode.PROVIDED -> repository
        }
    }
}