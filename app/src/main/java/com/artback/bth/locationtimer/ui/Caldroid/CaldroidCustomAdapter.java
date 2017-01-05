package com.artback.bth.locationtimer.ui.Caldroid;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.artback.bth.locationtimer.R;
import com.artback.bth.locationtimer.ui.CalendarActivity;
import com.roomorama.caldroid.CaldroidGridAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;

import hirondelle.date4j.DateTime;

public class CaldroidCustomAdapter extends CaldroidGridAdapter {
    private final SimpleDateFormat watchTime = new SimpleDateFormat("HH:mm");
    public CaldroidCustomAdapter(Context context, int month, int year,
                                 Map<String, Object> caldroidData,
                                 Map<String, Object> extraData) {
		super(context, month, year, caldroidData, extraData);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View cellView = convertView;


        //Get the custom dataSet
        ArrayList<DateTime> date = (ArrayList<DateTime>) extraData.get(CalendarActivity.dateKey);
        ArrayList<Long> time= (ArrayList<Long>) extraData.get(CalendarActivity.timeKey);


		// For reuse
		if (convertView == null) {
			cellView = inflater.inflate(R.layout.custom_calendar_cell, null);
        }

		int topPadding = cellView.getPaddingTop();
		int leftPadding = cellView.getPaddingLeft();
		int bottomPadding = cellView.getPaddingBottom();
		int rightPadding = cellView.getPaddingRight();

		TextView tv1 = (TextView) cellView.findViewById(R.id.tv1);
		TextView tv2 = (TextView) cellView.findViewById(R.id.tv2);
        tv2.setTextColor(Color.WHITE);
		tv1.setTextColor(Color.WHITE);

		// Get dateTime of this cell
		DateTime dateTime = this.datetimeList.get(position);

		// Set color of the dates in previous / next month
		if (dateTime.getMonth() != month) {
			tv1.setTextColor(ContextCompat.getColor(context,
                    com.caldroid.R.color.caldroid_darker_gray));
		}
        int i=0;
        if(date != null && date.size() > 0) {
                while (!dateTime.isSameDayAs(date.get(i)) && i < date.size()-1) {
                    i++;
                }
                if (dateTime.isSameDayAs(date.get(i))) {
                        tv2.setText(watchTime.format(time.get(i)));
                }
        }

		boolean shouldResetDisabledView = false;
		boolean shouldResetSelectedView = false;

		// Customize for disabled dates and date outside min/max dates
		if ((minDateTime != null && dateTime.lt(minDateTime))
				|| (maxDateTime != null && dateTime.gt(maxDateTime))
				|| (disableDates != null && disableDates.indexOf(dateTime) != -1)) {

			if (dateTime.equals(getToday())) {
				cellView.setBackgroundResource(com.caldroid.R.drawable.red_border_dark);
			}

		} else {
			shouldResetDisabledView = true;
		}

		// Customize for selected dates
		if (selectedDates != null && selectedDates.indexOf(dateTime) != -1) {
			tv1.setTextColor(Color.BLACK);
		} else {
			shouldResetSelectedView = true;
		}

		if (shouldResetDisabledView && shouldResetSelectedView) {
			// Customize for today
			if (dateTime.equals(getToday())) {
                cellView.setBackgroundResource(com.caldroid.R.drawable.red_border_dark);
            }
		}

		tv1.setText("" + dateTime.getDay());

		// Somehow after setBackgroundResource, the padding collapse.
		// This is to recover the padding
		cellView.setPadding(leftPadding, topPadding, rightPadding,
				bottomPadding);

		// Set custom color if required
		setCustomResources(dateTime, cellView, tv1);

		return cellView;
	}

}
