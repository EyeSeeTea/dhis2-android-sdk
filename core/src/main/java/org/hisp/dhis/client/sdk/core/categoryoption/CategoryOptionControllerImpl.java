
package org.hisp.dhis.client.sdk.core.categoryoption;

import org.hisp.dhis.client.sdk.core.common.Fields;
import org.hisp.dhis.client.sdk.core.common.network.ApiException;
import org.hisp.dhis.client.sdk.core.common.persistence.DbOperation;
import org.hisp.dhis.client.sdk.core.common.persistence.DbOperationImpl;
import org.hisp.dhis.client.sdk.core.common.persistence.TransactionManager;
import org.hisp.dhis.client.sdk.core.common.preferences.DateType;
import org.hisp.dhis.client.sdk.core.common.preferences.LastUpdatedPreferences;
import org.hisp.dhis.client.sdk.core.common.preferences.ResourceType;
import org.hisp.dhis.client.sdk.core.systeminfo.SystemInfoController;
import org.hisp.dhis.client.sdk.models.category.CategoryOption;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

public class CategoryOptionControllerImpl implements CategoryOptionController {

    private final SystemInfoController systemInfoController;
    private final CategoryOptionApiClient categoryOptionApiClient;
    private final CategoryOptionStore categoryOptionStore;

    private final LastUpdatedPreferences lastUpdatedPreferences;

    private final TransactionManager transactionManager;


    public CategoryOptionControllerImpl(SystemInfoController systemInfoController,
            CategoryOptionStore categoryOptionStore,
            CategoryOptionApiClient categoryOptionApiClient,
            LastUpdatedPreferences lastUpdatedPreferences,
            TransactionManager transactionManager) {
        this.systemInfoController = systemInfoController;
        this.categoryOptionStore = categoryOptionStore;
        this.categoryOptionApiClient = categoryOptionApiClient;
        this.lastUpdatedPreferences = lastUpdatedPreferences;
        this.transactionManager = transactionManager;
    }

    @Override
    public void pull() throws ApiException {

        DateTime serverTime = systemInfoController.getSystemInfo().getServerDate();
        List<CategoryOption> allCategoryOptions = categoryOptionApiClient
                .getCategoryOptions(Fields.ALL, null, null);

        List<DbOperation> dbOperations = new ArrayList<>();
        for (CategoryOption categoryOption : allCategoryOptions) {
            dbOperations.add(DbOperationImpl.with(categoryOptionStore)
                    .save(categoryOption));
        }
        transactionManager.transact(dbOperations);

        lastUpdatedPreferences.save(ResourceType.CATEGORYOPTIONS, DateType.SERVER, serverTime);
    }

    @Override
    public void pullDefault() throws ApiException {

        DateTime serverTime = systemInfoController.getSystemInfo().getServerDate();
        CategoryOption defaultCategoryOption = categoryOptionApiClient
                .getDefaultCategoryOption();

        List<DbOperation> dbOperations = new ArrayList<>();
        dbOperations.add(DbOperationImpl.with(categoryOptionStore)
                .save(defaultCategoryOption));

        transactionManager.transact(dbOperations);

        lastUpdatedPreferences.save(ResourceType.CATEGORYOPTIONS, DateType.SERVER, serverTime);
    }
}