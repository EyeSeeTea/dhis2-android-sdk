package org.hisp.dhis.android.core.category;


import android.support.annotation.NonNull;

import org.hisp.dhis.android.core.common.DeletableStore;

import java.util.List;

public interface CategoryOptionComboCategoryLinkStore extends DeletableStore {
    long insert(@NonNull CategoryOptionComboCategoryLinkModel element);

    int delete(@NonNull CategoryOptionComboCategoryLinkModel element);

    boolean update(
            @NonNull CategoryOptionComboCategoryLinkModel oldCategoryOptionComboCategoryLinkModel,
            @NonNull CategoryOptionComboCategoryLinkModel newCategoryOptionComboCategoryLinkModel);

    @NonNull
    List<CategoryOptionComboCategoryLinkModel> queryAll();

}
