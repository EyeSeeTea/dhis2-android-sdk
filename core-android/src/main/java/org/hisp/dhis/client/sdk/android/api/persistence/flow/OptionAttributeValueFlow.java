package org.hisp.dhis.client.sdk.android.api.persistence.flow;

/**
 * Created by idelcano on 15/11/2016.
 */


import com.raizlabs.android.dbflow.annotation.Table;

import org.hisp.dhis.client.sdk.android.api.persistence.DbDhis;

/**
 * This class will be disappeared soon.
 * This is a SDK Pojo and is created to fix the Queries,
 * is in the app side and with hardcoded methods.
 * It makes the project compile and centralized the necessary methods
 * and necessary sdk new Pojos..
 */
@Table(database = DbDhis.class)
public class OptionAttributeValueFlow extends BaseIdentifiableObjectFlow {


    public OptionAttributeValueFlow() {
    }

    public String getValue() {
        return null;
    }

    public AttributeFlow getAttribute() {
        return null;
    }

}
