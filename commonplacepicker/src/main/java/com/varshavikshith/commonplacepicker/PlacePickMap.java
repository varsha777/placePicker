package com.varshavikshith.commonplacepicker;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlacePickMap extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;
    private AppCompatTextView mLocationMarkerText;
    private LatLng mCenterLatLong;
    private String mCurrentLocation = "";

    private static final int REQUEST_CODE_AUTOCOMPLETE = 1;

    private FusedLocationProviderClient mFusedLocationClient = null;

    private ReverseGeocodingTask reverseGeocodingTask = null;
    private ActionBar actionBar = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_place_pick_map);

        String mapsKey = getIntent().getStringExtra("GOOGLE_MAPS_KEY");

        if (mapsKey != null) {
            if (mapsKey.isEmpty()) {
                Toast.makeText(mContext, "Google maps key should not be empty", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

        Places.initialize(getApplicationContext(), mapsKey);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.getTitle();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Select Location");
            actionBar.setSubtitle("Address");
        }

        mContext = this;
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.placepicker_library_map);

        mLocationMarkerText = (AppCompatTextView) findViewById(R.id.placepicker_library_locationMarkertext);

        mapFragment.getMapAsync(this);

        if (CommonFunc.isGooglePlayServicesAvailable(PlacePickMap.this)) {
            if (!CommonFunc.isGpsOn(mContext)) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
                dialog.setMessage(getString(R.string.pp_dialog_message));
                dialog.setPositiveButton(getString(R.string.pp_dialog_positive_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                });
                dialog.setNegativeButton(getString(R.string.pp_dialog_negitive_btn), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Toast.makeText(mContext, getString(R.string.pp_fail_gps), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
                dialog.show();
            }
            buildGoogleApiClient();
        } else {
            Toast.makeText(mContext, getString(R.string.pp_no_location_support), Toast.LENGTH_SHORT).show();
            finish();
        }

        ((AppCompatButton) findViewById(R.id.placepicker_library_select_location)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("SELECT_LOCATION_LATITUDE", mCenterLatLong.latitude);
                intent.putExtra("SELECT_LOCATION_LONGITUDE", mCenterLatLong.longitude);
                intent.putExtra("PLACE_PICKER_ADDRESS", mCurrentLocation);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }


    private void setAddress(String address) {
        mCurrentLocation = address;
        if (actionBar != null) {
            actionBar.setSubtitle(address);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (item.getItemId() == R.id.search_data_menu) {
            openAutocompleteActivity();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
            @Override
            public void onCameraMove() {
                mCenterLatLong = mMap.getCameraPosition().target;
                mMap.clear();
                try {
                    Location mLocation = new Location("");
                    mLocation.setLatitude(mCenterLatLong.latitude);
                    mLocation.setLongitude(mCenterLatLong.longitude);

                    mLocationMarkerText.setText("Lat : " + CommonFunc.returnDecimals(mCenterLatLong.latitude, 4) + "," + "Long : " + CommonFunc.returnDecimals(mCenterLatLong.longitude, 4));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mCenterLatLong = mMap.getCameraPosition().target;
                mMap.clear();
                try {
                    Location mLocation = new Location("");
                    mLocation.setLatitude(mCenterLatLong.latitude);
                    mLocation.setLongitude(mCenterLatLong.longitude);
                    saveLocToServer(mCenterLatLong);
                    mLocationMarkerText.setText("Lat : " + CommonFunc.returnDecimals(mCenterLatLong.latitude, 4) + "," + "Long : " + CommonFunc.returnDecimals(mCenterLatLong.longitude, 4));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
    }

    protected synchronized void buildGoogleApiClient() {

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                                && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        getLastLocation();
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        mGoogleApiClient.connect();
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                    }
                }).build();
        mGoogleApiClient.connect();


    }

    private LocationRequest mLocationRequest = null;

    @SuppressWarnings("MissingPermission")
    private void getLastLocation() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(10);

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(PlacePickMap.this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            try {
                if (locationResult != null) {
                    if (locationResult.getLastLocation() != null) {
                        changeMap(locationResult.getLastLocation());
                    }
                }
                stopLocationUpdates();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private void stopLocationUpdates() {
        if (mFusedLocationClient != null)
            mFusedLocationClient.removeLocationUpdates(locationCallback);
    }


    @Override
    protected void onStart() {
        super.onStart();
        try {
            mGoogleApiClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    private void changeMap(Location location) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if (mMap != null) {
            mMap.getUiSettings().setZoomControlsEnabled(false);
            LatLng latLong;
            latLong = new LatLng(location.getLatitude(), location.getLongitude());

            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLong).zoom(19f)/*.tilt(70)*/.build();

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.animateCamera(CameraUpdateFactory
                    .newCameraPosition(cameraPosition));
            saveLocToServer(latLong);
            mLocationMarkerText.setText("Lat : " + CommonFunc.returnDecimals(location.getLatitude(), 4) + "," + "Long : " + CommonFunc.returnDecimals(location.getLongitude(), 4));

        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.pp_map_unable_to_create), Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void openAutocompleteActivity() {
        try {
            List<Place.Field> fields = Arrays.asList(Place.Field.NAME, Place.Field.LAT_LNG);
            Intent intent = new Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN, fields)
                    .setCountry("IN")
                    .build(this);

            startActivityForResult(intent, REQUEST_CODE_AUTOCOMPLETE);
        } catch (Exception e) {
            Toast.makeText(mContext, getString(R.string.pp_play_services_not_available) + e.getMessage()
                    , Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AUTOCOMPLETE) {
            if (resultCode == RESULT_OK) {

                Place place = Autocomplete.getPlaceFromIntent(data);

                LatLng latLong;
                latLong = place.getLatLng();

                setAddress(place.getAddress());
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(latLong).zoom(19f)/*.tilt(70)*/.build();

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
                mMap.animateCamera(CameraUpdateFactory
                        .newCameraPosition(cameraPosition));
            }

        } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {

            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(mContext, getString(R.string.pp_play_services_not_available) + status.getStatusMessage(), Toast.LENGTH_SHORT).show();

        } else if (resultCode == RESULT_CANCELED) {

        }
    }

    private void saveLocToServer(LatLng location) {
        if (reverseGeocodingTask == null) {
            reverseGeocodingTask = new ReverseGeocodingTask();
            reverseGeocodingTask.execute(location);
        } else if (reverseGeocodingTask.getStatus() == AsyncTask.Status.RUNNING ||
                reverseGeocodingTask.getStatus() != AsyncTask.Status.RUNNING) {

            reverseGeocodingTask.cancel(true);
            reverseGeocodingTask = new ReverseGeocodingTask();
            reverseGeocodingTask.execute(location);
        }
    }

    private class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {
        double _latitude, _longitude;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected String doInBackground(LatLng... params) {

            Geocoder geocoder = new Geocoder(mContext);
            _latitude = params[0].latitude;
            _longitude = params[0].longitude;

            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(
                        _latitude,
                        _longitude,
                        1);
            } catch (IOException ioException) {
            }

            // Handle case where no address was found.
            if (addresses == null || addresses.size() == 0) {
                return "NA";

            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<String>();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                return TextUtils.join(System.getProperty("line.separator"),
                        addressFragments);

            }
            ////////////////////////////////
        }

        @Override
        protected void onPostExecute(String addressText) {
            if (addressText == null)
                addressText = "NA";
            else if (addressText.isEmpty())
                addressText = "NA";

            if (!addressText.equals("NA")) {
                setAddress(addressText);
            }

        }
    }


}
