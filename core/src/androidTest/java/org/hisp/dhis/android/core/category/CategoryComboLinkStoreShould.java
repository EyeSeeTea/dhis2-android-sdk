package org.hisp.dhis.android.core.category;

import static org.junit.Assert.assertEquals;

import android.support.test.runner.AndroidJUnit4;

import org.hisp.dhis.android.core.data.database.AbsStoreTestCase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class CategoryComboLinkStoreShould extends AbsStoreTestCase {

    private CategoryComboLinkStore store;
    private Category newCategory;
    private CategoryCombo newCategoryCombo;
    private long lastID;

    @Override
    @Before
    public void setUp() throws IOException {
        super.setUp();
        store = new CategoryComboLinkStoreImpl(databaseAdapter());

    }

    @Test
    public void insert_a_category_combo_link_in_the_DB() throws Exception {
        givenACategory();
        givenACategoryCombo();

        whenInsertNewCategory();
        whenInsertNewCategoryCombo();
        whenInsertNewCategoryComboLink();

        thenAssertLastIDIsOne();
    }

    private void givenACategory() {
        newCategory = generateCategory();
    }

    private Category generateCategory(){
        Date today = new Date();
        return Category.builder()
                .uid("KfdsGBcoiCa")
                .code("BIRTHS_ATTENDED")
                .created(today)
                .name("Births attended by")
                .shortName("Births attended by")
                .displayName("Births attended by")
                .dataDimensionType("DISAGGREGATION").build();
    }

    private void givenACategoryCombo() {
        Date today = new Date();

        newCategoryCombo = CategoryCombo.builder()
                .uid("m2jTvAj5kkm")
                .code("BIRTHS")
                .created(today)
                .name("Births")
                .displayName("Births")
                .categories(generateAListOfCategories())
                .build();
    }

    private void whenInsertNewCategory() {
        CategoryStoreImpl categoryStore = new CategoryStoreImpl(databaseAdapter());
        categoryStore.insert(newCategory);
    }

    private void whenInsertNewCategoryCombo() {
        CategoryComboStoreImpl comboStore = new CategoryComboStoreImpl(databaseAdapter());
        comboStore.insert(newCategoryCombo);
    }

    private void whenInsertNewCategoryComboLink() {
        CategoryComboLinkModel link = CategoryComboLinkModel.builder()
                .category("KfdsGBcoiCa")
                .combo("m2jTvAj5kkm")
                .build();
        lastID = store.insert(link);
    }

    private List<Category> generateAListOfCategories() {
        List<Category> list = new ArrayList<>();
        list.add(generateCategory());
        return list;
    }

    private void thenAssertLastIDIsOne() {
        assertEquals(lastID, 1);
    }
}