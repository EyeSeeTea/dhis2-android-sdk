package org.hisp.dhis.android.core.category;


import static org.hisp.dhis.android.core.utils.StoreUtils.parse;
import static org.hisp.dhis.android.core.utils.StoreUtils.sqLiteBind;
import static org.hisp.dhis.android.core.utils.Utils.isNull;

import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;

import org.hisp.dhis.android.core.data.database.DatabaseAdapter;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CategoryOptionComboStoreImpl implements CategoryOptionComboStore {

    public final DatabaseAdapter databaseAdapter;
    private final SQLiteStatement insertStatement;
    private final SQLiteStatement updateStatement;
    private final SQLiteStatement deleteStatement;


    private static final String FIELDS = CategoryOptionComboModel.Columns.UID + "," +
                    CategoryOptionComboModel.Columns.CODE
                    + "," +
                    CategoryOptionComboModel.Columns.NAME
                    + "," +
                    CategoryOptionComboModel.Columns.DISPLAY_NAME + "," +
                    CategoryOptionComboModel.Columns.CREATED
                    + "," +
                    CategoryOptionComboModel.Columns.LAST_UPDATED
                    + "," +
                    CategoryOptionComboModel.Columns.CATEGORY_COMBO;

    private static final String INSERT_STATEMENT =
            "INSERT INTO " + CategoryOptionComboModel.TABLE + " ("
                    +FIELDS+") " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?);";

    private static final String EQUAL_QUESTION_MARK = " =?";
    private static final String DELETE_STATEMENT = "DELETE FROM " + CategoryOptionComboModel.TABLE +
            " WHERE " + CategoryOptionComboModel.Columns.UID + EQUAL_QUESTION_MARK + ";";

    private static final String UPDATE_STATEMENT =
            "UPDATE " + CategoryOptionComboModel.TABLE + " SET " +
                    CategoryOptionComboModel.Columns.UID + EQUAL_QUESTION_MARK + ", " +
                    CategoryOptionComboModel.Columns.CODE + EQUAL_QUESTION_MARK + ", " +
                    CategoryOptionComboModel.Columns.NAME + EQUAL_QUESTION_MARK + ", " +
                    CategoryOptionComboModel.Columns.DISPLAY_NAME + EQUAL_QUESTION_MARK + ", "
                    +
                    CategoryOptionComboModel.Columns.CREATED + EQUAL_QUESTION_MARK + ", " +
                    CategoryOptionComboModel.Columns.LAST_UPDATED + EQUAL_QUESTION_MARK + ", " +
                    CategoryOptionComboModel.Columns.CATEGORY_COMBO + EQUAL_QUESTION_MARK
                    + " WHERE " +
                    CategoryOptionComboModel.Columns.UID + EQUAL_QUESTION_MARK + ";";

    private static final String QUERY_ALL_CATEGORY_OPTION_COMBOS = "SELECT " +
            FIELDS + " FROM " + CategoryOptionComboModel.TABLE;

    private static final String QUERY_CATEGORY_OPTION_COMBOS_BY_CATEGORY_COMBO_UID = "SELECT " +
            FIELDS + " FROM " + CategoryOptionComboModel.TABLE +
            " WHERE "+  CategoryOptionComboModel.Columns.CATEGORY_COMBO +"=?;";

    private static final String QUERY_BY_UID = "SELECT " +
            FIELDS + " FROM " + CategoryOptionComboModel.TABLE +
            " WHERE "+  CategoryOptionComboModel.Columns.UID +"=?;";

    public CategoryOptionComboStoreImpl(DatabaseAdapter databaseAdapter) {

        this.databaseAdapter = databaseAdapter;
        this.insertStatement = databaseAdapter.compileStatement(INSERT_STATEMENT);
        this.updateStatement = databaseAdapter.compileStatement(UPDATE_STATEMENT);
        this.deleteStatement = databaseAdapter.compileStatement(DELETE_STATEMENT);
    }

    @Override
    public long insert(@NonNull CategoryOptionCombo entity) {

        validate(entity);

        bind(insertStatement, entity);

        return executeInsert();
    }

    @Override
    public boolean delete(@NonNull CategoryOptionCombo entity) {

        validate(entity);

        bindForDelete(entity);

        return execute(deleteStatement);
    }

    @Override
    public boolean update(@NonNull CategoryOptionCombo entity) {

        validate(entity);

        bindUpdate(entity);

        return execute(updateStatement);
    }

    private void validate(@NonNull CategoryOptionCombo optionCombo) {
        isNull(optionCombo.uid());
    }

    private void bindForDelete(@NonNull CategoryOptionCombo optionCombo) {
        final int uidIndex = 1;

        sqLiteBind(deleteStatement, uidIndex, optionCombo.uid());
    }

    private void bindUpdate(@NonNull CategoryOptionCombo entity) {
        final int whereUidIndex = 8;
        bind(updateStatement, entity);

        sqLiteBind(updateStatement, whereUidIndex, entity.uid());
    }

    private void bind(SQLiteStatement sqLiteStatement, @NonNull CategoryOptionCombo newOptionCombo) {
        sqLiteBind(sqLiteStatement, 1, newOptionCombo.uid());
        sqLiteBind(sqLiteStatement, 2, newOptionCombo.code());
        sqLiteBind(sqLiteStatement, 3, newOptionCombo.name());
        sqLiteBind(sqLiteStatement, 4, newOptionCombo.displayName());
        sqLiteBind(sqLiteStatement, 5, newOptionCombo.created());
        sqLiteBind(sqLiteStatement, 6, newOptionCombo.lastUpdated());

        //noinspection ConstantConditions
        if (newOptionCombo.categoryCombo() != null
                && newOptionCombo.categoryCombo().uid() != null) {
            //noinspection ConstantConditions
            sqLiteBind(sqLiteStatement, 7, newOptionCombo.categoryCombo().uid());
        }

    }

    private boolean wasExecuted(int numberOfRows) {
        return numberOfRows >= 1;
    }

    private boolean execute(SQLiteStatement statement) {
        int rowsAffected = databaseAdapter.executeUpdateDelete(CategoryOptionComboModel.TABLE, statement);
        statement.clearBindings();

        return wasExecuted(rowsAffected);
    }

    private long executeInsert() {
        long lastId = databaseAdapter.executeInsert(CategoryOptionComboModel.TABLE, insertStatement);
        insertStatement.clearBindings();

        return lastId;
    }

    @Override
    public List<CategoryOptionCombo> queryAll() {
        Cursor cursor = databaseAdapter.query(QUERY_ALL_CATEGORY_OPTION_COMBOS);

        return mapCategoryOptionCombosFromCursor(cursor);
    }

    @Override
    public List<CategoryOptionCombo> queryByCategoryComboUId(String categoryComboUid) {
        Cursor cursor = databaseAdapter.query(QUERY_CATEGORY_OPTION_COMBOS_BY_CATEGORY_COMBO_UID, categoryComboUid);

        return mapCategoryOptionCombosFromCursor(cursor);
    }

    @Override
    public CategoryOptionCombo queryByUId(String uid) {
        Cursor cursor = databaseAdapter.query(QUERY_BY_UID, uid);
        CategoryOptionCombo categoryOptionCombo = null;
        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                categoryOptionCombo = mapCategoryOptionComboFromCursor(cursor);
            }
        } finally {
            cursor.close();
        }
        return categoryOptionCombo;
    }

    private List<CategoryOptionCombo> mapCategoryOptionCombosFromCursor(Cursor cursor) {
        List<CategoryOptionCombo> categoryOptionCombos = new ArrayList<>(cursor.getCount());

        try {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();

                do {
                    CategoryOptionCombo categoryOptionCombo = mapCategoryOptionComboFromCursor(cursor);

                    categoryOptionCombos.add(categoryOptionCombo);
                }
                while (cursor.moveToNext());
            }

        } finally {
            cursor.close();
        }
        return categoryOptionCombos;
    }

    private CategoryOptionCombo mapCategoryOptionComboFromCursor(Cursor cursor) {
        CategoryOptionCombo categoryOptionCombo;

        String uid = cursor.getString(0);
        String code = cursor.getString(1);
        String name = cursor.getString(2);
        String displayName = cursor.getString(3);
        Date created = cursor.getString(4) == null ? null : parse(cursor.getString(4));
        Date lastUpdated = cursor.getString(5) == null ? null : parse(cursor.getString(5));
        String categoryComboUid = cursor.getString(6);

        CategoryCombo categoryCombo = CategoryCombo.create(categoryComboUid, null, null,
                null, null, null, null, null, null);

        categoryOptionCombo = CategoryOptionCombo.create(
                uid, code, name, displayName, created, lastUpdated, categoryCombo, null);

        return categoryOptionCombo;
    }

    @Override
    public int delete() {
        return databaseAdapter.delete(CategoryOptionComboModel.TABLE);
    }

    public  DatabaseAdapter getAdapter(){
        return databaseAdapter;
    }

}

