package org.hisp.dhis.android.core.data.database.migration.incremental_migration;

import android.database.sqlite.SQLiteDatabase;

import org.hisp.dhis.android.core.data.database.IMigration;

public class TestMigration3AddTable implements IMigration {
    final int version=3;

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void upgrade(SQLiteDatabase writableDatabase) {
        writableDatabase.execSQL("create table if not exists TestTable (testColumn Integer);");
    }
}
