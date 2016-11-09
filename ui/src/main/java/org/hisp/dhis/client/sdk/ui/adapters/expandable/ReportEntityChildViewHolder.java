package org.hisp.dhis.client.sdk.ui.adapters.expandable;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bignerdranch.expandablerecyclerview.ChildViewHolder;

import org.hisp.dhis.client.sdk.ui.R;
import org.hisp.dhis.client.sdk.ui.models.ReportEntity;
import org.hisp.dhis.client.sdk.ui.views.CircleView;

public class ReportEntityChildViewHolder<C> extends ChildViewHolder<C> {

    // Event map keys:
    public static final String EVENT_DATE_KEY = "eventDate";
    public static final String EVENT_STATUS = "status";
    public static final String EVENT_DATE_LABEL = "Event date";
    public static final String STATUS_LABEL = "SyncStatus";
    public static final String ORG_UNIT = "OrgUnit";
    public static final String EVENT_LOCKED = "eventLocked";

    View itemView;

    final ImageView statusIcon;
    final CircleView statusBackground;
    final FrameLayout statusLockLayout;
    final TextView label;
    final TextView date;
    final ImageButton syncButton;

    final Drawable drawableActive;
    final Drawable drawableCompleted;
    final Drawable drawableSchedule;
    final Drawable drawableSkipped;
    final Drawable drawableOverdue;

    final int colorGray;
    final int colorGreen;
    final int colorYellow;
    final int colorBrown;
    final int colorRed;

    final Context context;

    public ReportEntityChildViewHolder(View itemView) {
        super(itemView);

        this.itemView = itemView;
        context = itemView.getContext();
        statusIcon = (ImageView) itemView.findViewById(R.id.status_icon);
        statusBackground = (CircleView) itemView.findViewById(R.id.circleview_status_background);
        statusLockLayout = (FrameLayout) itemView.findViewById(R.id.status_lock_container);
        syncButton = (ImageButton) itemView.findViewById(R.id.refresh_button);
        label = (TextView) itemView.findViewById(R.id.event_name);
        date = (TextView) itemView.findViewById(R.id.date_text);

        drawableActive = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_event_note_white);

        drawableCompleted = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_event_available_white);
        drawableSchedule = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_event_white);
        drawableSkipped = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_date_range_white);
        drawableOverdue = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_event_busy_white);

        colorGray = ContextCompat.getColor(itemView.getContext(), R.color.color_gray_400);
        colorGreen = ContextCompat.getColor(itemView.getContext(), R.color.color_green_300);
        colorYellow = ContextCompat.getColor(itemView.getContext(), R.color.color_amber_300);
        colorBrown = ContextCompat.getColor(itemView.getContext(), R.color.color_brown_300);
        colorRed = ContextCompat.getColor(itemView.getContext(), R.color.color_red_300);
    }

    public void bind(ReportEntity reportEntity, final ExpandableAdapter expandableAdapter, boolean selected) {
        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), android.R.color.white));
        label.setText(reportEntity.getId());

        //Map<String, String> dataElementToValueMap =
        //dataElementToValueMap.put(Event.EVENT_STATUS, event.getSyncStatus().toString());

        date.setText(reportEntity.getValueForDataElement(EVENT_DATE_KEY));

        //Display the EventSyncStatus:
        switch (reportEntity.getSyncStatus()) {
            case SENT: { //no-op.
                break;
            }
            case TO_POST: {
                syncButton.setImageResource(R.drawable.ic_refresh_gray);
                syncButton.setVisibility(View.VISIBLE);
                syncButton.setClickable(true);
                break;
            }
            case TO_UPDATE: {
                syncButton.setImageResource(R.drawable.ic_refresh_gray);
                syncButton.setVisibility(View.VISIBLE);
                syncButton.setClickable(true);
                break;
            }
            case ERROR: {
                //errorView.setVisibility(View.VISIBLE);
                syncButton.setImageResource(R.drawable.ic_sync_problem_black);
                syncButton.setVisibility(View.VISIBLE);
                syncButton.setClickable(true);
                break;
            }
        }

        //Display the event status
        String status = reportEntity.getValueForDataElement(EVENT_STATUS);
        switch (status) {
            case "ACTIVE":
                statusBackground.setFillColor(colorYellow);
                statusIcon.setImageDrawable(drawableActive);
                break;
            case "COMPLETED":

                statusBackground.setFillColor(colorGray);
                statusIcon.setImageDrawable(drawableCompleted);
                break;
            case "SCHEDULED":
                statusBackground.setFillColor(colorGreen);
                statusIcon.setImageDrawable(drawableSchedule);
                break;
            case "SKIPPED":
                statusBackground.setFillColor(colorBrown);
                statusIcon.setImageDrawable(drawableSkipped);
                break;
            case "OVERDUE":
                statusBackground.setFillColor(colorRed);
                statusIcon.setImageDrawable(drawableOverdue);
                break;
        }

        //Display lock status: TODO: When Event locking becomes available doulbe check that:
        if (!reportEntity.getValueForDataElement(EVENT_LOCKED).equals("none")) {
            statusLockLayout.setVisibility(View.VISIBLE);
        } else {
            statusLockLayout.setVisibility(View.GONE);
        }

        //On click listener & draw selected (for selection) ???
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.color_gray_300));
            }
        });

        //label.setText(reportEntity.getValueForDataElement(ORG_UNIT));

    }
}
