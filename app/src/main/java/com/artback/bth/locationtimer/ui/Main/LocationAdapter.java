package com.artback.bth.locationtimer.ui.Main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.artback.bth.locationtimer.R;
import com.artback.bth.locationtimer.db.GeoFenceLocation;

import java.text.SimpleDateFormat;
import java.util.List;

;

public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
        private  List<GeoFenceLocation> mGeofenceLocations;
        private final SimpleDateFormat hourMin = new SimpleDateFormat("HH");

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public static class ViewHolder extends RecyclerView.ViewHolder {

            private TextView locationTextView;
            private TextView timeTextView;

            public ViewHolder(View itemView) {
                super(itemView);
                locationTextView = (TextView) itemView.findViewById(R.id.location_text);
                timeTextView = (TextView) itemView.findViewById(R.id.time_text);
            }
        }

        public LocationAdapter(List<GeoFenceLocation> GeofenceLocations) {
            mGeofenceLocations = GeofenceLocations;
       }

        // Create new views (invoked by the layout manager)
        @Override
        public LocationAdapter.ViewHolder onCreateViewHolder(final ViewGroup parent,
                                                             final int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.location_view, parent, false);
            // set the view's size, margins, paddings and layout parameters
            return new ViewHolder(v);
        }
    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        GeoFenceLocation location = mGeofenceLocations.get(position);
        TextView textView = holder.locationTextView;
        TextView timeView = holder.timeTextView;
        textView.setText(location.getId());
    }
    @Override
    public int getItemCount() {
        return mGeofenceLocations.size();
    }

}


