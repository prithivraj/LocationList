package com.extremeboredom.locationlist;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback;
import com.extremeboredom.locationlist.LocationRealmAdapter.ViewHolder;
import com.extremeboredom.locationlist.model.LocationItem;

import io.realm.RealmBasedRecyclerViewAdapter;
import io.realm.RealmResults;
import io.realm.RealmViewHolder;

public class LocationRealmAdapter
        extends RealmBasedRecyclerViewAdapter<LocationItem, ViewHolder> {

    private final ListActivity listActivity;

    public class ViewHolder extends RealmViewHolder {

        public TextView address;
        public TextView landmark;
        public TextView phone;
        public FrameLayout container;
        public ViewHolder(FrameLayout container) {
            super(container);
            this.address = (TextView) container.findViewById(R.id.location_address);
            this.landmark = (TextView) container.findViewById(R.id.location_landmark);
            this.phone = (TextView) container.findViewById(R.id.location_phone);
            this.container = container;
        }
    }

    public LocationRealmAdapter(
            Context context,
            RealmResults<LocationItem> realmResults,
            boolean automaticUpdate,
            boolean animateResults, ListActivity listactivity) {
        super(context, realmResults, automaticUpdate, animateResults);
        this.listActivity = listactivity;
    }

    @Override
    public ViewHolder onCreateRealmViewHolder(ViewGroup viewGroup, int viewType) {
        View v = inflater.inflate(R.layout.list_item, viewGroup, false);
        ViewHolder vh = new ViewHolder((FrameLayout) v);
        return vh;
    }

    @Override
    public void onBindRealmViewHolder(ViewHolder viewHolder, int position) {
        final LocationItem locationItem = realmResults.get(position);
        viewHolder.address.setText(locationItem.getAddress());
        viewHolder.landmark.setText(locationItem.getLandmark());
        viewHolder.phone.setText(locationItem.getPhone());
        viewHolder.container.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                new MaterialDialog.Builder(getContext())
                        .title("Are you sure?")
                        .content("Do you want to edit this location?")
                        .positiveText("Yes")
                        .negativeText("No")
                        .onPositive(new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                LocationItem locationItem1 = new LocationItem();
                                locationItem1.setId(locationItem.getId());
                                locationItem1.setPhone(locationItem.getPhone());
                                locationItem1.setAddress(locationItem.getAddress());
                                locationItem1.setLongitude(locationItem.getLongitude());
                                locationItem1.setLatitude(locationItem.getLatitude());
                                locationItem1.setLandmark(locationItem.getLandmark());
                                listActivity.openMapWithData(locationItem1);
                            }
                        })
                        .onNegative(new SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    public void onItemSwipedDismiss(final int position) {
        new MaterialDialog.Builder(getContext())
                .title("Are you sure?")
                .content("Selecting 'Yes' will delete " + realmResults.get(position).getAddress())
                .positiveText("Yes")
                .negativeText("No")
                .canceledOnTouchOutside(false)
                .onPositive(new SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        deleteItemAt(position);
                    }
                })
                .onNegative(new SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        notifyDataSetChanged();
                    }
                })
                .show();
    }

    void deleteItemAt(int position){
        super.onItemSwipedDismiss(position);
    }
}