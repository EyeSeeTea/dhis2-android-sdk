package org.hisp.dhis.android.core.category;

import android.util.Log;

import org.hisp.dhis.android.core.audit.AuditType;
import org.hisp.dhis.android.core.audit.MetadataAudit;
import org.hisp.dhis.android.core.audit.MetadataAuditHandler;

import java.util.HashSet;
import java.util.Set;

public class CategoryOptionComboMetadataAuditHandler implements MetadataAuditHandler {

    private final CategoryComboFactory categoryComboFactory;

    public CategoryOptionComboMetadataAuditHandler(CategoryComboFactory categoryComboFactory) {
        this.categoryComboFactory = categoryComboFactory;
    }

    public void handle(MetadataAudit metadataAudit) throws Exception {
        //dhis server only send update auditType events for the CategoryOptionCombo Element.
        if (metadataAudit.getType() == AuditType.UPDATE) {
            Set<String> uIds = new HashSet<>();
            CategoryOptionCombo categoryOptionCombo =
                    categoryComboFactory.getCategoryOptionComboStore().queryByUId(
                            metadataAudit.getUid());
            if (categoryOptionCombo == null) {
                Log.e(this.getClass().getSimpleName(),
                        "MetadataAudit Error: "+ this.getClass().getSimpleName()
                                +" updated on server but does not exists in local: "
                                + metadataAudit);
            } else {
                uIds.add(categoryOptionCombo.categoryCombo().uid());
                categoryComboFactory.newEndPointCall(CategoryComboQuery.defaultQuery(uIds),
                        metadataAudit.getCreatedAt()).call();
            }
        }
    }
}