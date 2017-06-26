/*
 *  Copyright (c) 2016, University of Oslo
 *  * All rights reserved.
 *  *
 *  * Redistribution and use in source and binary forms, with or without
 *  * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright notice, this
 *  * list of conditions and the following disclaimer.
 *  *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *  * this list of conditions and the following disclaimer in the documentation
 *  * and/or other materials provided with the distribution.
 *  * Neither the name of the HISP project nor the names of its contributors may
 *  * be used to endorse or promote products derived from this software without
 *  * specific prior written permission.
 *  *
 *  * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 *  * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *  * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package org.hisp.dhis.android.sdk.ui.adapters.rows.dataentry;

import android.support.v4.app.FragmentManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.controllers.metadata.MetaDataController;
import org.hisp.dhis.android.sdk.persistence.models.BaseValue;
import org.hisp.dhis.android.sdk.persistence.models.DataElement;
import org.hisp.dhis.android.sdk.persistence.models.DataValue;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttribute;
import org.hisp.dhis.android.sdk.persistence.models.TrackedEntityAttributeValue;

import java.io.Serializable;

/**
 * Created by erling on 9/9/15.
 */
public abstract class Row implements DataEntryRow, Serializable {
    protected String mLabel;
    protected String mWarning;
    protected String mError;
    protected BaseValue mValue;
    protected String mDescription;
    protected DataEntryRowTypes mRowType;
    protected EditText focusableEditText;
    protected View scrollableView;
    //    protected View detailedInfoButton;
    private boolean hideDetailedInfoButton;
    private boolean editable = true;
    protected boolean mMandatory = false;
    private boolean shouldNeverBeEdited = false;

//    public View getDetailedInfoButton(){
//        return detailedInfoButton;
//    }

    public BaseValue getValue() {
        return mValue;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public abstract View getView(FragmentManager fragmentManager, LayoutInflater inflater,
            View convertView, ViewGroup container);

    @Override
    public abstract int getViewType();

    public String getItemId() {
        if (mValue instanceof DataValue) {
            return ((DataValue) mValue).getDataElement();
        } else if (mValue instanceof TrackedEntityAttributeValue) {
            return ((TrackedEntityAttributeValue) mValue).getTrackedEntityAttributeId();
        } else {
            return "";
        }
    }

    public String getDescription() {
        if (this instanceof CoordinatesRow) {
            mDescription = "";
        } else if (this instanceof StatusRow) {
            mDescription = "";
        } else if (this instanceof IndicatorRow) {
            mDescription = "";
        }

        String itemId = getItemId();
        DataElement dataElement = MetaDataController.getDataElement(itemId);
        if (dataElement != null) {
            mDescription = dataElement.getDescription();
        } else {
            TrackedEntityAttribute attribute = MetaDataController.getTrackedEntityAttribute(itemId);
            if (attribute != null) {
                mDescription = attribute.getDescription();
            }
        }

        return mDescription;
    }

    public void checkNeedsForDescriptionButton() {
        mDescription = getDescription();
        if (mDescription == null || mDescription.equals("")) {
            setHideDetailedInfoButton(true);
        } else {
            setHideDetailedInfoButton(false);
        }
    }

    public boolean isDetailedInfoButtonHidden() {
        return hideDetailedInfoButton;
    }

    public void setHideDetailedInfoButton(boolean hideDetailedInfoButton) {
        this.hideDetailedInfoButton = hideDetailedInfoButton;
    }

    public String getWarning() {
        return mWarning;
    }

    public void setWarning(String mWarning) {
        this.mWarning = mWarning;
    }

    public String getError() {
        return mError;
    }

    public void setError(String mError) {
        this.mError = mError;
    }

    public boolean isShouldNeverBeEdited() {
        return shouldNeverBeEdited;
    }

    public void setShouldNeverBeEdited(boolean shouldNeverBeEdited) {
        this.shouldNeverBeEdited = shouldNeverBeEdited;
    }

    public static class CustomOnEditorActionListener implements TextView.OnEditorActionListener {

        ListView mListView;

        public CustomOnEditorActionListener(ListView listView) {
            mListView = listView;
        }

        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            final TextView view = v;
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                final int position = mListView.getPositionForView(v);
                mListView.setSelection(position);
                int nextPosition = 1;
                Row nextField = (Row) mListView.getItemAtPosition(position + nextPosition);
                if (nextField == null) {
                    return false;
                }
                while (nextField.isShouldNeverBeEdited()) {
                    nextPosition++;
                    Row nextRow = (Row) mListView.getItemAtPosition(position + nextPosition);
                    if (nextRow == null) {
                        nextPosition--;
                        break;
                    } else {
                        nextField = nextRow;
                    }
                }
                mListView.postDelayed(new Runnable() {
                    public void run() {
                        int position = mListView.getPositionForView(view);
                        int nextPosition = 1;
                        Row nextField = (Row) mListView.getItemAtPosition(position + nextPosition);
                        mListView.scrollTo(0, nextField.scrollableView.getTop());
                        while (nextField != null && nextField.isShouldNeverBeEdited()) {
                            nextPosition++;
                            nextField = (Row) mListView.getItemAtPosition(position + nextPosition);
                        }
                        if (nextField != null && nextField.focusableEditText != null) {
                            nextField.focusableEditText.requestFocus();
                        }
                    }
                }, 200);
                return true;
            }
            return false;
        }

        private boolean isNotSupportedEditText(Row nextField) {
            if (nextField instanceof EditTextRow) {
                if (DataEntryRowTypes.INVALID_DATA_ENTRY.equals(nextField.getViewType()))
                {
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public void setFocusableEditText(EditText editText) {
        focusableEditText = editText;
    }

    public void setScrollableView(View scrollableView) {
        this.scrollableView = scrollableView;
    }
}
