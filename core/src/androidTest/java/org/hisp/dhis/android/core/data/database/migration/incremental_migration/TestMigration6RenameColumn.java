package org.hisp.dhis.android.core.data.database.migration.incremental_migration;

import android.database.sqlite.SQLiteDatabase;

import org.hisp.dhis.android.core.data.database.IMigration;

public class TestMigration6RenameColumn implements IMigration {
    final int version=6;

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void upgrade(SQLiteDatabase writableDatabase) {
        writableDatabase.execSQL("ALTER TABLE TestTableRenamed RENAME TO Temp_TestTableRenamed;");
        writableDatabase.execSQL("CREATE TABLE NewTestTable(testColumn Integer);");
        writableDatabase.execSQL("INSERT INTO NewTestTable(testColumn) SELECT testColumn from Temp_TestTableRenamed;");
        writableDatabase.execSQL("DROP TABLE Temp_TestTableRenamed;");
    }
}
