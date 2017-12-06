package org.hisp.dhis.android.core.data.database.migration.incremental_migration;

import android.database.sqlite.SQLiteDatabase;

import org.hisp.dhis.android.core.data.database.IMigration;

public class TestMigration5InsertValue implements IMigration {
    final int version=5;

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void upgrade(SQLiteDatabase writableDatabase) {
        writableDatabase.execSQL("INSERT INTO TestTableRenamed (testColumn) VALUES (1);");
    }
}
