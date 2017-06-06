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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.hisp.dhis.android.sdk.R;
import org.hisp.dhis.android.sdk.persistence.Dhis2Application;
import org.hisp.dhis.android.sdk.persistence.models.BaseValue;
import org.hisp.dhis.android.sdk.ui.fragments.dataentry.RowValueChangedEvent;

public class RadioButtonsRow extends Row {
    private static final String EMPTY_FIELD = "";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    public static final String FEMALE = "gender_female";
    public static final String MALE = "gender_male";
    public static final String OTHER = "gender_other";

    public RadioButtonsRow(String label, boolean mandatory, String warning, BaseValue baseValue, DataEntryRowTypes type) {
        if (!DataEntryRowTypes.GENDER.equals(type) && !DataEntryRowTypes.BOOLEAN.equals(type)) {
            throw new IllegalArgumentException("Unsupported row type");
        }
        mLabel = label;
        mMandatory = mandatory;
        mValue = baseValue;
        mRowType = type;
        mWarning = warning;

        checkNeedsForDescriptionButton();
    }

    @Override
    public View getView(FragmentManager fragmentManager, LayoutInflater inflater,
            View convertView, ViewGroup container) {
        View view;
        BooleanRowHolder holder;

        if (convertView != null && convertView.getTag() instanceof BooleanRowHolder) {
            view = convertView;
            holder = (BooleanRowHolder) convertView.getTag();
        } else {
            view = inflater.inflate(
                    R.layout.listview_row_radio_buttons, container, false);
            TextView label = (TextView)
                    view.findViewById(R.id.text_label);
            TextView mandatoryIndicator = (TextView) view.findViewById(R.id.mandatory_indicator);
            TextView warningLabel = (TextView) view.findViewById(R.id.warning_label);
            TextView errorLabel = (TextView) view.findViewById(R.id.error_label);
            CompoundButton firstButton = (CompoundButton)
                    view.findViewById(R.id.first_radio_button);
            CompoundButton secondButton = (CompoundButton)
                    view.findViewById(R.id.second_radio_button);
            RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group_row_radio_buttons);
//            detailedInfoButton =
//                    root.findViewById(R.id.detailed_info_button_layout);


            if (DataEntryRowTypes.BOOLEAN.equals(mRowType)) {
                firstButton.setText(R.string.yes);
                secondButton.setText(R.string.no);
            } else if (DataEntryRowTypes.GENDER.equals(mRowType)) {
                firstButton.setText(R.string.gender_male);
                secondButton.setText(R.string.gender_female);
            }

            holder = new BooleanRowHolder(mRowType, label, mandatoryIndicator, warningLabel, errorLabel, firstButton,
                    secondButton, radioGroup);

            view.setTag(holder);
        }

        CustomOnCheckedChangeListener onCheckedChangeListener = new CustomOnCheckedChangeListener();
        holder.radioGroup.setOnCheckedChangeListener(onCheckedChangeListener);
        holder.setRadioGroupCheckedChangeListener(onCheckedChangeListener);
        if(!isEditable()) {
            holder.firstButton.setEnabled(false);
            holder.secondButton.setEnabled(false);
        } else {
            holder.firstButton.setEnabled(true);
            holder.secondButton.setEnabled(true);
        }

//       detailedInfoButton.setOnClickListener(new OnDetailedInfoButtonClick(this));
        holder.label.setText(mLabel);
        updateRadioButtons(holder.radioGroup, mValue, holder.firstButton, holder.secondButton, mRowType);
//        if(isDetailedInfoButtonHidden()) {
//           detailedInfoButton.setVisibility(View.INVISIBLE);
//        }
//        else {
//           detailedInfoButton.setVisibility(View.VISIBLE);
//        }

        if(mWarning == null) {
            holder.warningLabel.setVisibility(View.GONE);
        } else {
            holder.warningLabel.setVisibility(View.VISIBLE);
            holder.warningLabel.setText(mWarning);
        }

        if(mError == null) {
            holder.errorLabel.setVisibility(View.GONE);
        } else {
            holder.errorLabel.setVisibility(View.VISIBLE);
            holder.errorLabel.setText(mError);
        }

        if(!mMandatory) {
            holder.mandatoryIndicator.setVisibility(View.GONE);
        } else {
            holder.mandatoryIndicator.setVisibility(View.VISIBLE);
        }

        return view;
    }

    public void updateRadioButtons(RadioGroup radioGroup, BaseValue baseValue, CompoundButton firstButton,
            CompoundButton secondButton, DataEntryRowTypes type) {

        CustomOnCheckedChangeListener
                onCheckedChangeListener = new CustomOnCheckedChangeListener();
        onCheckedChangeListener.setBaseValue(baseValue);

        radioGroup.setOnCheckedChangeListener(null);
        String value = baseValue.getValue();
        radioGroup.clearCheck();

        if (DataEntryRowTypes.BOOLEAN.equals(type)) {
            if (TRUE.equalsIgnoreCase(value)) {
                firstButton.setChecked(true);
            } else if (FALSE.equalsIgnoreCase(value)) {
                secondButton.setChecked(true);
            } else {
                if (secondButton.isChecked()) {
                    secondButton.setChecked(false);
                }
                if (firstButton.isChecked()) {
                    firstButton.setChecked(false);
                }
            }
        } else if (DataEntryRowTypes.GENDER.equals(type)) {
            if (MALE.equalsIgnoreCase(value)) {
                firstButton.setChecked(true);
            } else if (FEMALE.equalsIgnoreCase(value)) {
                secondButton.setChecked(true);
            } else {
                if (secondButton.isChecked()) {
                    secondButton.setChecked(false);
                }
                if (firstButton.isChecked()) {
                    firstButton.setChecked(false);
                }
            }
        }
        radioGroup.setOnCheckedChangeListener(onCheckedChangeListener);
    }

    @Override
    public int getViewType() {
        return mRowType.ordinal();
    }


    private static class BooleanRowHolder {
        final TextView label;
        final TextView mandatoryIndicator;
        final TextView warningLabel;
        final TextView errorLabel;
        final CompoundButton firstButton;
        final CompoundButton secondButton;
        RadioGroup radioGroup;
//        final View detailedInfoButton;
        CustomOnCheckedChangeListener radioGroupCheckedChangeListener;
        final DataEntryRowTypes type;

        public BooleanRowHolder(DataEntryRowTypes type, TextView label, TextView mandatoryIndicator, TextView warningLabel, TextView errorLabel, CompoundButton firstButton,
                CompoundButton secondButton, RadioGroup radioGroup) {
            this.type = type;
            this.label = label;
            this.mandatoryIndicator = mandatoryIndicator;
            this.warningLabel = warningLabel;
            this.errorLabel = errorLabel;
            this.firstButton = firstButton;
            this.secondButton = secondButton;
            this.radioGroup = radioGroup;
        }

        public void setRadioGroupCheckedChangeListener(
                CustomOnCheckedChangeListener radioGroupCheckedChangeListener) {
            this.radioGroupCheckedChangeListener = radioGroupCheckedChangeListener;
        }
    }

    private static class CustomOnCheckedChangeListener implements RadioGroup.OnCheckedChangeListener {

        BaseValue baseValue;

        public BaseValue getBaseValue() {
            return baseValue;
        }

        public void setBaseValue(BaseValue baseValue) {
            this.baseValue = baseValue;
        }

        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            String newValue;

            if (checkedId == org.hisp.dhis.android.sdk.R.id.first_radio_button) {
                newValue = TRUE;
            } else if (checkedId == org.hisp.dhis.android.sdk.R.id.second_radio_button) {
                newValue = FALSE;
            } else {
                newValue = EMPTY_FIELD;
            }
            if(!newValue.equals(baseValue.getValue())) {
                baseValue.setValue(newValue);
                Dhis2Application.getEventBus().post(new RowValueChangedEvent(baseValue, DataEntryRowTypes.BOOLEAN.toString()));
            }
            this.baseValue.setValue(baseValue.getValue());
        }
    }
}
