/*
 * Copyright (c) 2017, University of Oslo
 *
 * All rights reserved.
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

package org.hisp.dhis.android.core.data.database.migration;

import static com.google.common.truth.Truth.assertThat;

import static org.hisp.dhis.android.core.data.database.DbOpenHelper.putMigration;
import static org.hisp.dhis.android.core.data.database.SqliteChecker.ifTableExist;
import static org.hisp.dhis.android.core.data.database.SqliteChecker.ifValueExist;
import static org.hisp.dhis.android.core.data.database.SqliteChecker.isFieldExist;

import android.support.test.runner.AndroidJUnit4;

import org.hisp.dhis.android.core.data.database.IMigration;
import org.hisp.dhis.android.core.data.database.migration.incremental_migration.TestMigration2AddColumn;
import org.hisp.dhis.android.core.data.database.migration.incremental_migration.TestMigration3AddTable;
import org.hisp.dhis.android.core.data.database.migration.incremental_migration.TestMigration4RenameTable;
import org.hisp.dhis.android.core.data.database.migration.incremental_migration.TestMigration5InsertValue;
import org.hisp.dhis.android.core.data.database.migration.incremental_migration.TestMigration6RenameColumn;
import org.hisp.dhis.android.core.user.UserModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
public class MigrationsShould extends AbsStoreMigrationTester {

    @Before
    @Override
    public void setUp() throws IOException {
        super.setUp();

    }

    @Test
    public void have_new_column_when_migration_add_column() {
        putMigration(new TestMigration2AddColumn());
        forceUpgradeDataBase(2);
        assertThat(dbOpenHelper.getReadableDatabase().getVersion()).isEqualTo(2);
        assertThat(isFieldExist(UserModel.TABLE, "testColumn", dbOpenHelper.getWritableDatabase())).isTrue();
    }


    @Test(expected = IllegalArgumentException.class)
    public void throw_illegal_argument_exception_when_have_migration_without_migration_class() {
        HashMap<Integer, IMigration> migrations = new HashMap<>();
        dbOpenHelper.setMigrations(migrations);
        forceUpgradeDataBase(2);
    }

    @Test
    public void have_new_table_when_migration_add_table() {
        putMigration(new TestMigration2AddColumn());
        putMigration(new TestMigration3AddTable());
        forceUpgradeDataBase(3);
        assertThat(dbOpenHelper.getReadableDatabase().getVersion()).isEqualTo(3);
        assertThat(ifTableExist("TestTable", dbOpenHelper.getWritableDatabase())).isTrue();
    }
    @Test
    public void have_renamed_table_when_migration_rename_table() {
        putMigration(new TestMigration2AddColumn());
        putMigration(new TestMigration3AddTable());
        putMigration(new TestMigration4RenameTable());
        forceUpgradeDataBase(4);
        assertThat(dbOpenHelper.getReadableDatabase().getVersion()).isEqualTo(4);
        assertThat(ifTableExist("TestTableRenamed", dbOpenHelper.getWritableDatabase())).isTrue();
        assertThat(ifTableExist("TestTable", dbOpenHelper.getWritableDatabase())).isFalse();
    }
    @Test
    public void have_inserted_value_when_migration_insert_value() {
        putMigration(new TestMigration2AddColumn());
        putMigration(new TestMigration3AddTable());
        putMigration(new TestMigration4RenameTable());
        putMigration(new TestMigration5InsertValue());
        forceUpgradeDataBase(5);
        assertThat(dbOpenHelper.getReadableDatabase().getVersion()).isEqualTo(5);
        assertThat(ifTableExist("TestTableRenamed", dbOpenHelper.getWritableDatabase())).isTrue();
        assertThat(ifTableExist("TestTable", dbOpenHelper.getWritableDatabase())).isFalse();
        assertThat(ifValueExist("TestTableRenamed", "testColumn", "not inserted value", dbOpenHelper.getWritableDatabase())).isFalse();
        assertThat(ifValueExist("TestTableRenamed", "testColumn", "1", dbOpenHelper.getWritableDatabase())).isTrue();
    }
    @Test
    public void have_drop_table_when_migration_delete_table() {
        putMigration(new TestMigration2AddColumn());
        putMigration(new TestMigration3AddTable());
        putMigration(new TestMigration4RenameTable());
        putMigration(new TestMigration5InsertValue());
        putMigration(new TestMigration6RenameColumn());
        forceUpgradeDataBase(6);
        assertThat(dbOpenHelper.getReadableDatabase().getVersion()).isEqualTo(6);
        assertThat(ifTableExist("TestTableRenamed", dbOpenHelper.getWritableDatabase())).isFalse();
        assertThat(ifTableExist("TestTable", dbOpenHelper.getWritableDatabase())).isFalse();
    }

    public void addMigration(HashMap<Integer, IMigration> migrations, IMigration migration) {
        migrations.put(migration.getVersion(), migration);
    }
}

