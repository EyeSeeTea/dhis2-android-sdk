package org.hisp.dhis.android.core.deletedobject;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_CATEGORY_OPTION_COMBO;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_DATA_ELEMENTS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_OPTIONS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_OPTION_SETS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_PROGRAM_INDICATORS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_PROGRAM_RULES;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_PROGRAM_RULE_ACTIONS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_PROGRAM_RULE_VARIABLES;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_PROGRAM_STAGES;
import static org.hisp.dhis.android.core.common.MockedCalls
        .DELETED_OBJECT_PROGRAM_STAGE_DATA_ELEMENTS;
import static org.hisp.dhis.android.core.common.MockedCalls.EMPTY_OPTION_SETS;
import static org.hisp.dhis.android.core.common.MockedCalls.EMPTY_PROGRAMS;
import static org.hisp.dhis.android.core.common.MockedCalls.EMPTY_TRACKED_ENTITIES;
import static org.hisp.dhis.android.core.common.MockedCalls.CATEGORY_COMBOS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_CATEGORIES;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_CATEGORY_COMBO;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_CATEGORY_OPTIONS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_EMPTY;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_ORGANISATION_UNITS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_PROGRAMS;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_TRACKED_ENTITY;
import static org.hisp.dhis.android.core.common.MockedCalls.DELETED_OBJECT_USER;
import static org.hisp.dhis.android.core.common.MockedCalls.ALTERNATIVE_USER;
import static org.hisp.dhis.android.core.common.MockedCalls.EMPTY_CATEGORIES;
import static org.hisp.dhis.android.core.common.MockedCalls.EMPTY_CATEGORY_COMBOS;
import static org.hisp.dhis.android.core.common.MockedCalls.EMPTY_ORGANISATION_UNITS;
import static org.hisp.dhis.android.core.common.MockedCalls.MULTIPLE_ORGANISATION_UNITS;
import static org.hisp.dhis.android.core.common.MockedCalls.MULTIPLE_PROGRAMS;
import static org.hisp.dhis.android.core.common.MockedCalls.OPTION_SETS;
import static org.hisp.dhis.android.core.common.MockedCalls.SIMPLE_CATEGORIES;
import static org.hisp.dhis.android.core.common.MockedCalls.SYSTEM_INFO;
import static org.hisp.dhis.android.core.common.MockedCalls.TRACKED_ENTITIES;

import android.support.test.filters.MediumTest;
import android.support.test.runner.AndroidJUnit4;

import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.CategoryComboStoreImpl;
import org.hisp.dhis.android.core.category.CategoryOptionComboStoreImpl;
import org.hisp.dhis.android.core.category.CategoryOptionStoreImpl;
import org.hisp.dhis.android.core.category.CategoryStoreImpl;
import org.hisp.dhis.android.core.common.D2Factory;
import org.hisp.dhis.android.core.common.DeletableStore;
import org.hisp.dhis.android.core.common.MockedCalls;
import org.hisp.dhis.android.core.data.database.AbsStoreTestCase;
import org.hisp.dhis.android.core.data.file.AssetsFileReader;
import org.hisp.dhis.android.core.data.server.Dhis2MockServer;
import org.hisp.dhis.android.core.dataelement.DataElementStoreImpl;
import org.hisp.dhis.android.core.option.OptionSetStoreImpl;
import org.hisp.dhis.android.core.option.OptionStoreImpl;
import org.hisp.dhis.android.core.organisationunit.OrganisationUnitStoreImpl;
import org.hisp.dhis.android.core.program.ProgramIndicatorStoreImpl;
import org.hisp.dhis.android.core.program.ProgramRuleActionStoreImpl;
import org.hisp.dhis.android.core.program.ProgramRuleStoreImpl;
import org.hisp.dhis.android.core.program.ProgramRuleVariableStoreImpl;
import org.hisp.dhis.android.core.program.ProgramStageDataElement;
import org.hisp.dhis.android.core.program.ProgramStageDataElementStoreImpl;
import org.hisp.dhis.android.core.program.ProgramStageStoreImpl;
import org.hisp.dhis.android.core.program.ProgramStoreImpl;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityStoreImpl;
import org.hisp.dhis.android.core.user.UserStoreImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class DeletedObjectEndpointCallMockIntegrationShould extends AbsStoreTestCase {

    String[] metadataJsonWithDeletedObjects = new String[]{
            SYSTEM_INFO,
            DELETED_OBJECT_USER, ALTERNATIVE_USER,
            DELETED_OBJECT_ORGANISATION_UNITS, EMPTY_ORGANISATION_UNITS,
            DELETED_OBJECT_CATEGORIES,
            DELETED_OBJECT_CATEGORY_OPTIONS, EMPTY_CATEGORIES,
            DELETED_OBJECT_CATEGORY_COMBO,
            DELETED_OBJECT_EMPTY, EMPTY_CATEGORY_COMBOS,
            DELETED_OBJECT_PROGRAM_RULES,
            DELETED_OBJECT_PROGRAM_RULE_ACTIONS,
            DELETED_OBJECT_PROGRAM_RULE_VARIABLES,
            DELETED_OBJECT_PROGRAM_INDICATORS,
            DELETED_OBJECT_DATA_ELEMENTS,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_PROGRAMS, EMPTY_PROGRAMS,
            DELETED_OBJECT_TRACKED_ENTITY, EMPTY_TRACKED_ENTITIES,
            DELETED_OBJECT_OPTIONS,
            DELETED_OBJECT_OPTION_SETS, EMPTY_OPTION_SETS};

    String[] metadataJsonWithDeletedCategoryComboOptionsObjects = new String[]{
            SYSTEM_INFO,
            DELETED_OBJECT_USER, ALTERNATIVE_USER,
            DELETED_OBJECT_ORGANISATION_UNITS, EMPTY_ORGANISATION_UNITS,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY, EMPTY_CATEGORIES,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_CATEGORY_OPTION_COMBO, EMPTY_CATEGORY_COMBOS,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_PROGRAMS, EMPTY_PROGRAMS,
            DELETED_OBJECT_TRACKED_ENTITY, EMPTY_TRACKED_ENTITIES,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_OPTION_SETS, EMPTY_OPTION_SETS};

    String[] metadataJsonWithDeletedProgramStagesObjects = new String[]{
            SYSTEM_INFO,
            DELETED_OBJECT_USER, ALTERNATIVE_USER,
            DELETED_OBJECT_ORGANISATION_UNITS, EMPTY_ORGANISATION_UNITS,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY, EMPTY_CATEGORIES,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_CATEGORY_OPTION_COMBO, EMPTY_CATEGORY_COMBOS,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_PROGRAM_STAGES,
            DELETED_OBJECT_PROGRAM_STAGE_DATA_ELEMENTS,
            DELETED_OBJECT_EMPTY, EMPTY_PROGRAMS,
            DELETED_OBJECT_TRACKED_ENTITY, EMPTY_TRACKED_ENTITIES,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_OPTION_SETS, EMPTY_OPTION_SETS};

    public final static String[] commonMetadataWithMultipleObjectsJsonFiles = new String[]{
            SYSTEM_INFO,
            DELETED_OBJECT_EMPTY, ALTERNATIVE_USER,
            DELETED_OBJECT_EMPTY, MULTIPLE_ORGANISATION_UNITS,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY, SIMPLE_CATEGORIES,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY, CATEGORY_COMBOS,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY, MULTIPLE_PROGRAMS,
            DELETED_OBJECT_EMPTY, TRACKED_ENTITIES,
            DELETED_OBJECT_EMPTY,
            DELETED_OBJECT_EMPTY, OPTION_SETS};

    private Dhis2MockServer dhis2MockServer;
    private D2 d2;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();

        dhis2MockServer = new Dhis2MockServer(new AssetsFileReader());

        d2 = D2Factory.create(dhis2MockServer.getBaseEndpoint(), databaseAdapter());
    }

    @Override
    @After
    public void tearDown() throws IOException {
        super.tearDown();

        dhis2MockServer.shutdown();
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_user() throws Exception {
        UserStoreImpl userStore = new UserStoreImpl(databaseAdapter());
        MockedCalls.givenAMetadataInDatabase(dhis2MockServer);
        d2.syncMetaData().call();
        verifyIfIsPersisted("DXyJmlo9rge", userStore);

        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        verifyIfIsDeleted("DXyJmlo9rge", userStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_organisation_unit() throws Exception {
        OrganisationUnitStoreImpl organisationUnitStore = new OrganisationUnitStoreImpl(
                databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("YuQRtpLP102", organisationUnitStore);
        verifyIfIsPersisted("YuQRtpLP10I", organisationUnitStore);

        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        verifyIfIsPersisted("DiszpKrYNg8", organisationUnitStore);
        verifyIfIsDeleted("YuQRtpLP102", organisationUnitStore);
        verifyIfIsDeleted("YuQRtpLP10I", organisationUnitStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_categories() throws Exception {
        CategoryStoreImpl categoryStore = new CategoryStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("vGs6omsRekv", categoryStore);
        verifyIfIsPersisted("cX5k9anHEHd", categoryStore);

        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        verifyIfIsPersisted("KfdsGBcoiCa", categoryStore);

        verifyIfIsDeleted("vGs6omsRekv", categoryStore);
        verifyIfIsDeleted("cX5k9anHEHd", categoryStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_categoryOptions() throws Exception {
        //given
        CategoryStoreImpl categoryStore = new CategoryStoreImpl(databaseAdapter());
        CategoryOptionStoreImpl categoryOptionStore = new CategoryOptionStoreImpl(
                databaseAdapter());
        MockedCalls.givenAMetadataInDatabase(dhis2MockServer);
        d2.syncMetaData().call();
        verifyIfIsPersisted("TXGfLxZlInA", categoryOptionStore);
        verifyIfIsPersisted("uZUnebiT5DI", categoryOptionStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsPersisted("KfdsGBcoiCa", categoryStore);
        verifyIfIsPersisted("TNYQzTHdoxL", categoryOptionStore);

        verifyIfIsDeleted("TXGfLxZlInA", categoryOptionStore);
        verifyIfIsDeleted("uZUnebiT5DI", categoryOptionStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_option_sets() throws Exception {
        //given
        OptionSetStoreImpl optionSetStore = new OptionSetStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("VQ2lai3OfVG", optionSetStore);
        verifyIfIsPersisted("R3mpvjqJ81H", optionSetStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsPersisted("xjA5E9MimMU", optionSetStore);

        verifyIfIsDeleted("VQ2lai3OfVG", optionSetStore);
        verifyIfIsDeleted("R3mpvjqJ81H", optionSetStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_options() throws Exception {
        //given
        OptionSetStoreImpl optionSetStore = new OptionSetStoreImpl(databaseAdapter());
        OptionStoreImpl optionStore = new OptionStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("Yjte6foKMny", optionStore);
        verifyIfIsPersisted("wfkKVdPBzho", optionStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsPersisted("xjA5E9MimMU", optionSetStore);

        verifyIfIsDeleted("Yjte6foKMny", optionStore);
        verifyIfIsDeleted("wfkKVdPBzho", optionStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_category_combo() throws Exception {
        //given
        CategoryComboStoreImpl categoryComboStore = new CategoryComboStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("p0KPaWEg3cf", categoryComboStore);
        verifyIfIsPersisted("m2jTvAj5kkm", categoryComboStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("p0KPaWEg3cf", categoryComboStore);
        verifyIfIsDeleted("m2jTvAj5kkm", categoryComboStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_category_option_combo() throws Exception {
        //given
        CategoryOptionComboStoreImpl categoryOptionComboStore = new CategoryOptionComboStoreImpl(
                databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("bRowv6yZOF2", categoryOptionComboStore);
        verifyIfIsPersisted("Gmbgme7z9BF", categoryOptionComboStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                metadataJsonWithDeletedCategoryComboOptionsObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("bRowv6yZOF2", categoryOptionComboStore);
        verifyIfIsDeleted("Gmbgme7z9BF", categoryOptionComboStore);
    }


    @Test
    @MediumTest
    public void delete_the_given_deleted_data_element() throws Exception {
        //given
        DataElementStoreImpl dataElementStore = new DataElementStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("Ok9OQpitjQr", dataElementStore);
        verifyIfIsPersisted("sWoqcoByYmD", dataElementStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("Ok9OQpitjQr", dataElementStore);
        verifyIfIsDeleted("sWoqcoByYmD", dataElementStore);
    }


    @Test
    @MediumTest
    public void delete_the_given_deleted_programs() throws Exception {
        //given
        ProgramStoreImpl programStore = new ProgramStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("q04UBOqq3rp", programStore);
        verifyIfIsPersisted("kla3mAPgvCH", programStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("q04UBOqq3rp", programStore);
        verifyIfIsDeleted("kla3mAPgvCH", programStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_program_stages() throws Exception {
        //given
        ProgramStageStoreImpl programStageStore = new ProgramStageStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("dBwrot7S420", programStageStore);
        verifyIfIsPersisted("A03MvHHogjR", programStageStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedProgramStagesObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("dBwrot7S420", programStageStore);
        verifyIfIsDeleted("A03MvHHogjR", programStageStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_program_stage_data_elements() throws Exception {
        //given
        ProgramStageDataElementStoreImpl programStageDataElementStore = new ProgramStageDataElementStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("ztoQtbuXzsI", programStageDataElementStore);
        verifyIfIsPersisted("vdc1saaN2ma", programStageDataElementStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedProgramStagesObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("ztoQtbuXzsI", programStageDataElementStore);
        verifyIfIsDeleted("vdc1saaN2ma", programStageDataElementStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_program_indicators() throws Exception {
        //given
        ProgramIndicatorStoreImpl programIndicatorStore = new ProgramIndicatorStoreImpl(
                databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("Kswd1r4qWLh", programIndicatorStore);
        verifyIfIsPersisted("hAHF3BEHGjM", programIndicatorStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("Kswd1r4qWLh", programIndicatorStore);
        verifyIfIsDeleted("hAHF3BEHGjM", programIndicatorStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_program_rules() throws Exception {
        //given
        ProgramRuleStoreImpl programRuleStore = new ProgramRuleStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("fd3wL1quxGb", programRuleStore);
        verifyIfIsPersisted("OfWLsxH5ylF", programRuleStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("fd3wL1quxGb", programRuleStore);
        verifyIfIsDeleted("OfWLsxH5ylF", programRuleStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_program_rule_actions() throws Exception {
        //given
        ProgramRuleActionStoreImpl programRuleActionStore = new ProgramRuleActionStoreImpl(
                databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(
                commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("hLOEPUPseEv", programRuleActionStore);
        verifyIfIsPersisted("actonrw1065", programRuleActionStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsDeleted("hLOEPUPseEv", programRuleActionStore);
        verifyIfIsDeleted("actonrw1065", programRuleActionStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_deleted_tracked_entity() throws Exception {
        //given
        TrackedEntityStoreImpl trackedEntityStore = new TrackedEntityStoreImpl(databaseAdapter());
        MockedCalls.givenAMetadataInDatabase(dhis2MockServer);
        d2.syncMetaData().call();
        verifyIfIsPersisted("nEenWmSyUE2", trackedEntityStore);
        verifyIfIsPersisted("nEenWmSyUE3", trackedEntityStore);

        //when
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();

        //then
        verifyIfIsPersisted("nEenWmSyUEp", trackedEntityStore);
        verifyIfIsDeleted("nEenWmSyUE2", trackedEntityStore);
        verifyIfIsDeleted("nEenWmSyUE3", trackedEntityStore);
    }

    @Test
    @MediumTest
    public void delete_the_given_program_rule_variables() throws Exception {
        ProgramRuleVariableStoreImpl
                programRuleVariableStore = new ProgramRuleVariableStoreImpl(databaseAdapter());
        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(commonMetadataWithMultipleObjectsJsonFiles);
        d2.syncMetaData().call();
        verifyIfIsPersisted("RycV5uDi66i", programRuleVariableStore);
        verifyIfIsPersisted("zINGRka3g9N", programRuleVariableStore);

        dhis2MockServer.enqueueMockedResponsesFromArrayFiles(metadataJsonWithDeletedObjects);
        d2.syncMetaData().call();
        verifyIfIsDeleted("RycV5uDi66i", programRuleVariableStore);
        verifyIfIsDeleted("zINGRka3g9N", programRuleVariableStore);
    }

    private void verifyIfIsPersisted(String uid, DeletableStore store) {
        Boolean isPersisted = store.exists(uid);
        assertThat(isPersisted, is(true));
    }

    private void verifyIfIsDeleted(String uid, DeletableStore store) {
        Boolean isPersisted = store.exists(uid);
        assertThat(isPersisted, is(false));
    }
}
