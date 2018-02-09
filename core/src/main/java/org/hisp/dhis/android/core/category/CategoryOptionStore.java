package org.hisp.dhis.android.core.category;


import android.support.annotation.NonNull;

import org.hisp.dhis.android.core.common.SoftDeletableStore;

import java.util.List;

public interface CategoryOptionStore extends SoftDeletableStore {

    long insert(@NonNull CategoryOption categoryOption);

    int delete(@NonNull String uid);

    int update(@NonNull CategoryOption categoryOption);

    List<CategoryOption> queryAll();

    CategoryOption queryByUid(String uid);
}
