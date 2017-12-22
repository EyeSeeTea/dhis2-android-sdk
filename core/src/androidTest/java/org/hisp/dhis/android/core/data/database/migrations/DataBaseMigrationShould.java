package org.hisp.dhis.android.core.data.database.migrations;

import static org.hamcrest.CoreMatchers.is;
import static org.hisp.dhis.android.core.data.database.SqliteCheckerUtility.ifTableExist;
import static org.hisp.dhis.android.core.data.database.SqliteCheckerUtility.ifValueExist;
import static org.hisp.dhis.android.core.data.database.SqliteCheckerUtility.isFieldExist;
import static org.junit.Assert.assertThat;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.hisp.dhis.android.core.D2;
import org.hisp.dhis.android.core.configuration.ConfigurationModel;
import org.hisp.dhis.android.core.data.api.BasicAuthenticatorFactory;
import org.hisp.dhis.android.core.data.database.DatabaseAdapter;
import org.hisp.dhis.android.core.data.database.DbOpenHelper;
import org.hisp.dhis.android.core.data.database.SqLiteDatabaseAdapter;
import org.hisp.dhis.android.core.user.UserModel;
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

    public static final String realMigrationDir = "migrations/real_migrations";
    public static final String exampleMigrationsDir = "migrations/example_migrations";
    public static final String databaseSqlVersion1 = "db_version_1.sql";
    static String dbName= null;
    
    @Before
    public void deleteDB(){
        mockWebServer = new MockWebServer();
        try {
            mockWebServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public DbOpenHelper initCoreDataBase(String dbName, int databaseVersion, String testPath, String databaseSqlVersion1){
        DbOpenHelper dbOpenHelper = new DbOpenHelper(InstrumentationRegistry.getTargetContext().getApplicationContext()
                , dbName, databaseVersion, testPath, databaseSqlVersion1);
        return dbOpenHelper;
    }

    private void buildD2(DbOpenHelper dbOpenHelper) {
        ConfigurationModel config = ConfigurationModel.builder()
                .serverUrl(mockWebServer.url("/"))
                .build();
        DatabaseAdapter databaseAdapter = new SqLiteDatabaseAdapter(dbOpenHelper);
        d2 = new D2.Builder()
                .configuration(config)
                .okHttpClient(new OkHttpClient.Builder()
                        .addInterceptor(BasicAuthenticatorFactory.create(databaseAdapter))
                        .build())
                .databaseAdapter(databaseAdapter).build();
    }

    @Test
    public void have_user_table_after_first_migration() throws IOException {
        buildD2(initCoreDataBase(dbName, 1, exampleMigrationsDir, databaseSqlVersion1));
        assertThat(ifTableExist(UserModel.TABLE, d2), is(true));
    }


    @Test
    public void have_category_table_after_first_migration() throws IOException {
        buildD2(initCoreDataBase(dbName, 2, realMigrationDir, databaseSqlVersion1));
        //TODO : check Category table using the CategoryModel.TABLE
        assertThat(ifTableExist("Category", d2), is(true));
    }

    @Test
    public void have_new_column_when_up_migration_add_column() throws IOException {
        buildD2(initCoreDataBase(dbName, 1, exampleMigrationsDir, databaseSqlVersion1));
        assertThat(isFieldExist(UserModel.TABLE, "testColumn", d2), is(true));
    }


    @Test
    public void have_new_value_when_seed_migration_add_row() {
        buildD2(initCoreDataBase(dbName, 2, exampleMigrationsDir, databaseSqlVersion1));
        assertThat(isFieldExist(UserModel.TABLE, "testColumn", d2), is(true));
        assertThat(ifTableExist("TestTable", d2), is(true));
        assertThat(ifValueExist("TestTable", "testColumn","1", d2), is(true));
    }

    @Test
    public synchronized void have_dropped_table_when_down_migration_drop_table() {
        buildD2(initCoreDataBase(dbName, 2, exampleMigrationsDir, databaseSqlVersion1));
        buildD2(initCoreDataBase(dbName, 1, exampleMigrationsDir, ""));
        assertThat(isFieldExist(UserModel.TABLE, "testColumn", d2), is(true));
        assertThat(ifTableExist("TestTable", d2), is(false));
    }
}
