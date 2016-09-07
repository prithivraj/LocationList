package com.extremeboredom.locationlist;

import android.Manifest.permission;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.LinearLayout;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.Builder;
import com.afollestad.materialdialogs.MaterialDialog.SingleButtonCallback;
import com.afollestad.materialdialogs.internal.MDButton;
import com.extremeboredom.locationlist.model.LocationItem;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraIdleListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LocationActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location lastKnownLocation;
    Geocoder geocoder;
    LocationItem details = null;
    LocationActivity locationActivity;

    @BindView(R.id.selectLocation)
    Button confirmLocation;

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        if(mGoogleApiClient!=null){
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationActivity = this;
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        geocoder = new Geocoder(this, Locale.getDefault());
        Dexter.initialize(getApplicationContext());
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            Object edit = extras.get("edit");
            if(edit instanceof LocationItem){
                details = (LocationItem) edit;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        Dexter.checkPermissions(new MultiplePermissionsListener() {
            @Override
            public void onPermissionsChecked(MultiplePermissionsReport report) {
                if (report.areAllPermissionsGranted()) {
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    map.setMyLocationEnabled(true);
                }
                if(details != null){
                    zoomCameraAt(details.getLatitude(), details.getLongitude());
                }
                else{
                    details = new LocationItem();
                    mGoogleApiClient = new GoogleApiClient.Builder(locationActivity)
                            .addApi(LocationServices.API)
                            .addConnectionCallbacks(locationActivity)
                            .addOnConnectionFailedListener(locationActivity)
                            .build();
                    mGoogleApiClient.connect();
                }
                map.setOnCameraIdleListener(new OnCameraIdleListener() {
                    StringBuilder address = new StringBuilder();
                    @Override
                    public void onCameraIdle() {
                        List<android.location.Address> addresses = null;
                        double latitude = map.getCameraPosition().target.latitude;
                        double longitude = map.getCameraPosition().target.longitude;
                        try {
                            addresses = geocoder.getFromLocation(latitude, longitude, 1); // Here 1 represent max lastKnownLocation result to returned, by documents it recommended 1 to 5
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        clear();
                        if(addresses !=null && !addresses.isEmpty()){
                            addToAddress(addresses.get(0).getAddressLine(0));
                            addToAddress(addresses.get(0).getLocality());
                            addToAddress(addresses.get(0).getAdminArea());
                            addToAddress(addresses.get(0).getCountryName());
                            addToAddress(addresses.get(0).getPostalCode());
                        }
                        confirmLocation.setText(address.toString());
                    }
                    void clear(){
                        address = new StringBuilder("");
                    }
                    void addToAddress(String s){
                        if(s!=null){
                            address.append(s + " ");
                        }
                    }
                });
            }

            @Override
            public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                token.continuePermissionRequest();
            }
        }, permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onLocationChanged(Location location) {
        this.lastKnownLocation = location;
        zoomCameraAt(location.getLatitude(), location.getLongitude());
    }

    private void zoomCameraAt(Double lat, Double lon) {
        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(lat, lon));
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(15);
        map.moveCamera(center);
        map.animateCamera(zoom);
    }

    @OnClick(R.id.selectLocation)
    void confirmLocation(){
        Builder builder = new Builder(this)
                .title("")
                .customView(R.layout.map_details, true)
                .positiveText("Save")
                .onPositive(new SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        LinearLayout customView = (LinearLayout) dialog.getCustomView();
                        TextInputEditText landmark = (TextInputEditText) customView.findViewById(R.id.textview_landmark);
                        TextInputEditText phone = (TextInputEditText) customView.findViewById(R.id.textview_phone);
                        if(details.getId() == -1){
                            details.setId(System.currentTimeMillis());
                        }
                        details.setAddress(confirmLocation.getText().toString());
                        details.setLandmark(landmark.getText().toString());
                        details.setPhone(phone.getText().toString());
                        details.setLatitude(map.getCameraPosition().target.latitude);
                        details.setLongitude(map.getCameraPosition().target.longitude);
                        //All set, time to put this back to the Realm
                        Intent intent = new Intent();
                        intent.putExtra("edit",details);
                        setResult(200, intent);
                        finish();

                    }
                })
                .onNegative(new SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .negativeText("Back")
                .title("More details");
        MaterialDialog show = builder.show();

        final MDButton save = show.getActionButton(DialogAction.POSITIVE);
        LinearLayout customView = (LinearLayout) show.getCustomView();
        final TextInputEditText landmark = (TextInputEditText) customView.findViewById(R.id.textview_landmark);
        final TextInputEditText phone = (TextInputEditText) customView.findViewById(R.id.textview_phone);
        if(details.getId() == -1){
            save.setEnabled(false);
        }
        else{
            landmark.setText(details.getLandmark());
            phone.setText(details.getPhone());
        }
        landmark.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 0 || phone.length() == 0){
                    save.setEnabled(false);
                }
                if(s.length() >0 && phone.length() == 10){
                    save.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        phone.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() != 10 || landmark.length() == 0){
                    save.setEnabled(false);
                }
                if(s.length() == 10 && landmark.length() >0){
                    save.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //dust
    }

    @Override
    public void onConnectionSuspended(int i) {
        //dust
    }
}
