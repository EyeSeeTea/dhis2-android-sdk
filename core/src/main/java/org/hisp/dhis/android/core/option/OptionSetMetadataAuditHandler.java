package org.hisp.dhis.android.core.option;

import org.hisp.dhis.android.core.audit.AuditType;
import org.hisp.dhis.android.core.audit.MetadataAudit;
import org.hisp.dhis.android.core.audit.MetadataAuditHandler;

import java.util.HashSet;
import java.util.Set;

public class OptionSetMetadataAuditHandler implements MetadataAuditHandler {

    private final OptionSetFactory optionSetFactory;

    public OptionSetMetadataAuditHandler(OptionSetFactory optionSetFactory) {
        this.optionSetFactory = optionSetFactory;
    }

    public void handle(MetadataAudit metadataAudit) throws Exception {
        OptionSet optionSet = (OptionSet) metadataAudit.getValue();

        if (metadataAudit.getType() == AuditType.UPDATE) {
            //metadataAudit of UPDATE type does not return payload
            //It's necessary sync by metadata call

            Set<String> uIds = new HashSet<>();
            uIds.add(metadataAudit.getUid());

            optionSetFactory.newEndPointCall(uIds, metadataAudit.getCreatedAt()).call();
        } else {
            if (metadataAudit.getType() == AuditType.DELETE) {
                optionSet = optionSet.toBuilder().deleted(true).build();
            }

            optionSetFactory.getHandler().handleOptionSet(optionSet);
        }
    }
}
