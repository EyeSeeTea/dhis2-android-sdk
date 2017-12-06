package org.hisp.dhis.android.core.data.database;

import android.database.sqlite.SQLiteDatabase;

public interface IMigration {
    int getVersion();
    void upgrade(SQLiteDatabase writableDatabase);
}
