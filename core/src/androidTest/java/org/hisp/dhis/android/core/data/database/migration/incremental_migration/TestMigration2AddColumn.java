package org.hisp.dhis.android.core.data.database.migration.incremental_migration;

import android.database.sqlite.SQLiteDatabase;

import org.hisp.dhis.android.core.data.database.IMigration;
import org.hisp.dhis.android.core.user.UserModel;

public class TestMigration2AddColumn implements IMigration {
    final int version=2;

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public void upgrade(SQLiteDatabase writableDatabase) {
        writableDatabase.execSQL("ALTER TABLE " + UserModel.TABLE  + " ADD COLUMN testColumn INTEGER");
    }
}
