package org.hisp.dhis.android.core.program;

import org.hisp.dhis.android.core.common.DeletableStore;
import org.hisp.dhis.android.core.data.database.DatabaseAdapter;
import org.hisp.dhis.android.core.dataelement.DataElementHandler;
import org.hisp.dhis.android.core.dataelement.DataElementStore;
import org.hisp.dhis.android.core.dataelement.DataElementStoreImpl;
import org.hisp.dhis.android.core.option.OptionSetHandler;
import org.hisp.dhis.android.core.relationship.RelationshipTypeHandler;
import org.hisp.dhis.android.core.relationship.RelationshipTypeStore;
import org.hisp.dhis.android.core.relationship.RelationshipTypeStoreImpl;
import org.hisp.dhis.android.core.resource.ResourceHandler;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeHandler;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeStore;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityAttributeStoreImpl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import retrofit2.Retrofit;

public class ProgramFactory {
    private final DatabaseAdapter databaseAdapter;
    private final ProgramService programService;
    private final ResourceHandler resourceHandler;
    private final ProgramHandler programHandler;
    private final ProgramStageStore programStageStore;
    private final ProgramStageHandler programStageHandler;

    private final List<DeletableStore> deletableStores;

    public ProgramFactory(
            Retrofit retrofit, DatabaseAdapter databaseAdapter,
            OptionSetHandler optionSetHandler, ResourceHandler resourceHandler) {
        this.programService = retrofit.create(ProgramService.class);
        this.databaseAdapter = databaseAdapter;
        this.resourceHandler = resourceHandler;

        DataElementStore dataElementStore = new DataElementStoreImpl(databaseAdapter);

        ProgramStageDataElementStore programStageDataElementStore =
                new ProgramStageDataElementStoreImpl(databaseAdapter);

        ProgramIndicatorStore programIndicatorStore = new ProgramIndicatorStoreImpl(
                databaseAdapter);

        ProgramStageSectionProgramIndicatorLinkStore programStageSectionProgramIndicatorLinkStore =
                new ProgramStageSectionProgramIndicatorLinkStoreImpl(databaseAdapter);

        ProgramStageSectionStore programStageSectionStore = new ProgramStageSectionStoreImpl(
                databaseAdapter);

        programStageStore = new ProgramStageStoreImpl(databaseAdapter);

        ProgramRuleVariableStore programRuleVariableStore = new ProgramRuleVariableStoreImpl(
                databaseAdapter);

        ProgramRuleActionStore programRuleActionStore = new ProgramRuleActionStoreImpl(
                databaseAdapter);

        TrackedEntityAttributeStore trackedEntityAttributeStore =
                new TrackedEntityAttributeStoreImpl(databaseAdapter);

        RelationshipTypeStore relationshipTypeStore = new RelationshipTypeStoreImpl(
                databaseAdapter);

        ProgramRuleStore programRuleStore = new ProgramRuleStoreImpl(databaseAdapter);

        ProgramTrackedEntityAttributeStore programTrackedEntityAttributeStore =
                new ProgramTrackedEntityAttributeStoreImpl(databaseAdapter);

        ProgramStore programStore = new ProgramStoreImpl(databaseAdapter);

        DataElementHandler dataElementHandler = new DataElementHandler(dataElementStore,
                optionSetHandler);

        ProgramStageDataElementHandler programStageDataElementHandler =
                new ProgramStageDataElementHandler(programStageDataElementStore,
                        dataElementHandler);

        ProgramIndicatorHandler programIndicatorHandler = new ProgramIndicatorHandler(
                programIndicatorStore, programStageSectionProgramIndicatorLinkStore);

        ProgramStageSectionHandler programStageSectionHandler = new ProgramStageSectionHandler(
                programStageSectionStore, programStageDataElementHandler, programIndicatorHandler);

        programStageHandler = new ProgramStageHandler(
                programStageStore, programStageSectionHandler, programStageDataElementHandler);

        ProgramRuleVariableHandler programRuleVariableHandler = new ProgramRuleVariableHandler(
                programRuleVariableStore);

        ProgramRuleActionHandler programRuleActionHandler = new ProgramRuleActionHandler(
                programRuleActionStore);

        ProgramRuleHandler programRuleHandler = new ProgramRuleHandler(programRuleStore,
                programRuleActionHandler);

        TrackedEntityAttributeHandler trackedEntityAttributeHandler =
                new TrackedEntityAttributeHandler(trackedEntityAttributeStore);


        ProgramTrackedEntityAttributeHandler programTrackedEntityAttributeHandler =
                new ProgramTrackedEntityAttributeHandler(
                        programTrackedEntityAttributeStore, trackedEntityAttributeHandler);

        RelationshipTypeHandler relationshipTypeHandler = new RelationshipTypeHandler(
                relationshipTypeStore);

        this.programHandler =
                new ProgramHandler(programStore, programRuleVariableHandler, programStageHandler,
                        programIndicatorHandler, programRuleHandler,
                        programTrackedEntityAttributeHandler, relationshipTypeHandler);

        this.deletableStores = new ArrayList<>();
        this.deletableStores.add(dataElementStore);
        this.deletableStores.add(programStageDataElementStore);
        this.deletableStores.add(programIndicatorStore);
        this.deletableStores.add(programStageSectionProgramIndicatorLinkStore);
        this.deletableStores.add(programStageSectionStore);
        this.deletableStores.add(programStageStore);
        this.deletableStores.add(programRuleVariableStore);
        this.deletableStores.add(programRuleActionStore);
        this.deletableStores.add(trackedEntityAttributeStore);
        this.deletableStores.add(relationshipTypeStore);
        this.deletableStores.add(programRuleStore);
        this.deletableStores.add(programTrackedEntityAttributeStore);
        this.deletableStores.add(programStore);
    }

    public ProgramCall newEndPointCall(Set<String> programUids, Date serverDate) {
        return new ProgramCall(
                programService, databaseAdapter, resourceHandler, programUids,
                serverDate, programHandler);
    }

    public ProgramHandler getProgramHandler() {
        return programHandler;
    }

    public List<DeletableStore> getDeletableStores() {
        return deletableStores;
    }

    public ProgramStageStore getProgramStageStore() {
        return programStageStore;
    }

    public ProgramStageHandler getProgramStageHandler() {
        return programStageHandler;
    }
}
