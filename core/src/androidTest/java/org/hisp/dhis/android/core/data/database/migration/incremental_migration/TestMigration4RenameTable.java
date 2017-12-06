package org.hisp.dhis.android.core.data.database.migration.incremental_migration;

import android.database.sqlite.SQLiteDatabase;

import org.hisp.dhis.android.core.data.database.IMigration;

public class TestMigration4RenameTable implements IMigration {
    final int version=4;

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void upgrade(SQLiteDatabase writableDatabase) {
        writableDatabase.execSQL("Alter table TestTable RENAME TO TestTableRenamed;");
    }
}
