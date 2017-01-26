package org.hisp.dhis.client.sdk.android.api.persistence.flow;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.Table;

import org.hisp.dhis.client.sdk.android.api.persistence.DbDhis;

/**
 * Created by idelcano on 14/11/2016.
 */


/**
 * This class will be disappeared soon.
 * This is a SDK Pojo and is created to fix the Queries,
 * is in the app side and with hardcoded methods.
 * It makes the project compile and centralized the necessary methods
 * and necessary sdk new Pojos..
 */
@Table(database = DbDhis.class)
public class AttributeFlow extends BaseIdentifiableObjectFlow {
    public AttributeFlow() {
    }

    @Column
    String code;

    public String getCode() {
        //return code;
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }


}
