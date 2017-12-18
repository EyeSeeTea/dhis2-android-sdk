package org.hisp.dhis.android.core.data.database.migrations;

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.android.core.data.database.SqliteCheckerUtility.ifTableExist;
import static org.hisp.dhis.android.core.data.database.SqliteCheckerUtility.ifValueExist;
import static org.hisp.dhis.android.core.data.database.SqliteCheckerUtility.isFieldExist;
import static org.junit.Assert.assertThat;

import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.category.CategoryComboLinkModel;
import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryModel;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionLinkModel;
import org.hisp.dhis.android.core.category.CategoryOptionModel;
import org.hisp.dhis.android.core.configuration.ConfigurationModel;
import org.hisp.dhis.android.core.data.api.BasicAuthenticatorFactory;
import org.hisp.dhis.android.core.data.database.DatabaseAdapter;
import org.hisp.dhis.android.core.data.database.DbOpenHelper;
import org.hisp.dhis.android.core.data.database.SqLiteDatabaseAdapter;
import org.hisp.dhis.android.core.dataelement.DataElementModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.user.UserModel;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;

@RunWith(AndroidJUnit4.class)
public class DataBaseMigrationShould {
    private MockWebServer mockWebServer;

    private D2 d2;
    private DatabaseAdapter databaseAdapter;

    public static final String realMigrationDir = "migrations/real_migrations";
    public static final String exampleMigrationsDir = "migrations/example_migrations";
    public static final String databaseSqlVersion1 = "db_version_1.sql";
    public static final String databaseSqlVersion2 = "db_version_2.sql";
    static String dbName= "null.db";
    
    @Before
    public void deleteDB(){
        mockWebServer = new MockWebServer();
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(dbName!=null) {
            InstrumentationRegistry.getContext().deleteDatabase(dbName);
        }
    }

    @After
    public void tearDown() throws Exception {
        if(dbName!=null) {
            InstrumentationRegistry.getContext().deleteDatabase(dbName);
        }
    }

    public DatabaseAdapter initCoreDataBase(String dbName, int databaseVersion, String testPath, String databaseSqlVersion1){
        DbOpenHelper dbOpenHelper = new DbOpenHelper(InstrumentationRegistry.getTargetContext().getApplicationContext()
                , dbName, databaseVersion, testPath, databaseSqlVersion1);
        databaseAdapter = new SqLiteDatabaseAdapter(dbOpenHelper);
        return databaseAdapter;
    }

    private void buildD2(DatabaseAdapter databaseAdapter) {
        ConfigurationModel config = ConfigurationModel.builder()
                .serverUrl(mockWebServer.url("/"))
                .build();
        d2 = new D2.Builder()
                .configuration(config)
                .okHttpClient(new OkHttpClient.Builder()
                        .addInterceptor(BasicAuthenticatorFactory.create(databaseAdapter))
                        .build())
                .databaseAdapter(databaseAdapter).build();
    }

    @Test
    public void have_user_table_after_first_migration() throws IOException {
        initCoreDataBase(dbName, 1, exampleMigrationsDir, databaseSqlVersion1);
        assertThat(ifTableExist(UserModel.TABLE, databaseAdapter), is(true));
    }

    @Test
    public void have_category_table_after_first_migration() throws IOException {
        initCoreDataBase(dbName, 1, realMigrationDir, databaseSqlVersion1);
        initCoreDataBase(dbName, 2, realMigrationDir, "");
        assertThat(ifTableExist(CategoryModel.TABLE, databaseAdapter), is(true));
        assertThat(ifTableExist(CategoryComboModel.TABLE, databaseAdapter), is(true));
        assertThat(ifTableExist(CategoryOptionComboModel.TABLE, databaseAdapter), is(true));
        assertThat(ifTableExist(CategoryOptionModel.TABLE, databaseAdapter), is(true));
        assertThat(ifTableExist(CategoryComboLinkModel.TABLE, databaseAdapter), is(true));
        assertThat(ifTableExist(CategoryOptionLinkModel.TABLE, databaseAdapter), is(true));
    }

    @Test
    public void have_categoryCombo_columns_after_first_migration() throws IOException {
        initCoreDataBase(dbName, 1, realMigrationDir, databaseSqlVersion1);
        initCoreDataBase(dbName, 2, realMigrationDir, "");
        assertThat(isFieldExist(DataElementModel.TABLE, DataElementModel.Columns.CATEGORY_COMBO, databaseAdapter), is(true));
        assertThat(isFieldExist(ProgramModel.TABLE, ProgramModel.Columns.CATEGORY_COMBO, databaseAdapter), is(true));
    }

    @Test
    public void have_categoryCombo_columns_after_create_version_2() throws IOException {
        buildD2(initCoreDataBase(dbName, 2, realMigrationDir, databaseSqlVersion2));
        assertThat(isFieldExist(DataElementModel.TABLE, DataElementModel.Columns.CATEGORY_COMBO, databaseAdapter), is(true));
        assertThat(isFieldExist(ProgramModel.TABLE, ProgramModel.Columns.CATEGORY_COMBO, databaseAdapter), is(true));
    }
    @Test
    public void have_categoryCombo_columns_after_create_last_version() throws IOException {
        buildD2(initCoreDataBase(dbName, 2, realMigrationDir, ""));
        assertThat(isFieldExist(DataElementModel.TABLE, DataElementModel.Columns.CATEGORY_COMBO, d2.databaseAdapter()), is(true));
        assertThat(isFieldExist(ProgramModel.TABLE, ProgramModel.Columns.CATEGORY_COMBO, d2.databaseAdapter()), is(true));
    }

    @Test
    public void have_new_column_when_up_migration_add_column() throws IOException {
        initCoreDataBase(dbName, 1, exampleMigrationsDir, databaseSqlVersion1);
        initCoreDataBase(dbName, 2, exampleMigrationsDir, "");
        assertThat(isFieldExist(UserModel.TABLE, "testColumn", databaseAdapter), is(true));
    }

    @Test
    public void have_new_value_when_seed_migration_add_row() {
        initCoreDataBase(dbName, 1, exampleMigrationsDir, databaseSqlVersion1);
        initCoreDataBase(dbName, 3, exampleMigrationsDir, "");
        assertThat(isFieldExist(UserModel.TABLE, "testColumn", databaseAdapter), is(true));
        assertThat(ifTableExist("TestTable", databaseAdapter), is(true));
        assertThat(ifValueExist("TestTable", "testColumn","1", databaseAdapter), is(true));
    }

    @Test
    public synchronized void have_dropped_table_when_down_migration_drop_table() {
        initCoreDataBase(dbName, 1, exampleMigrationsDir, databaseSqlVersion1);
        initCoreDataBase(dbName, 2, exampleMigrationsDir, "");
        initCoreDataBase(dbName, 1, exampleMigrationsDir, "");
        assertThat(isFieldExist(UserModel.TABLE, "testColumn", databaseAdapter), is(true));
        assertThat(ifTableExist("TestTable", databaseAdapter), is(false));
    }
}