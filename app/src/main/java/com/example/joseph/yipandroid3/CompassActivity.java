package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static java.util.Locale.getDefault;

/**
 * This activity is the main Compass Activity that handles the general user interaction in Yipping
 * various targets.
 *
 *
 *
 */
public class CompassActivity extends Activity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener, PlaceSelectionListener {
    /** Number used for UUID Generation */
    private static final int RANDOM_LARGE_NUMBER = 65535;

    /** Layout Elements */
    private ImageView compass;

    /** Device Sensor Manager */
    private SensorManager mSensorManager;

    /** Google API */
    private GoogleApiClient mGoogleApiClient;

    // todo: pubnub streaming
    /** Randomly generated channelName */
    private static String channelName;
    private PubnubManager pubnubManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        // init
        Bundle extras = getIntent().getExtras();
        this.compass = (ImageView) findViewById(R.id.compass);
        if (extras != null) {
            double lat;
            double lng;
            App.YipType type;

            lat = extras.getDouble("lat");
            lng = extras.getDouble("lng");
            type = (App.YipType) extras.getSerializable("mode");

            if (type == App.YipType.ADDRESS_YIP) {
                PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                        getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
                autocompleteFragment.setOnPlaceSelectedListener(this);
            }

        }
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.buildGoogleApiClient();
    }


    /**
     * Callback invoked when a place has been selected from the PlaceAutocompleteFragment.
     * @param place Place object of the user selected suggestion
     *              Contains Name, Id, Address, Phone #, and Website
     */
    @Override
    public void onPlaceSelected(Place place) {
        Log.i(this.getClass().getSimpleName(), "Address Selected: " + place.getName() + " -- Located at "
                + place.getAddress());

        // search targetLocation location
        Geocoder geocoder = new Geocoder(this.getApplicationContext(), getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocationName(place.getAddress().toString(), 1);
            Double lat = addresses.get(0).getLatitude();
            Double lng = addresses.get(0).getLongitude();


            LocationService.setTargetLocation(lat, lng);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Callback invoked when PlaceAutocompleteFragment encounters an error.
     */
    @Override
    public void onError(Status status) {
        Log.e(this.getClass().getSimpleName(), "onError: Status = " + status.toString());
    }

    /** Initialize Google API Client
     * Adds Location Services API
     * Adds Places API */
    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .build();
        }
    }

    private AdapterView.OnItemClickListener onItemClickListener() {
        return new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String description = (String) parent.getItemAtPosition(position);
                // search...
            }
        };
    }

    @Override
    protected void onResume () {
            super.onResume();
            mSensorManager.registerListener(
                    this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_UI
            );
            mSensorManager.registerListener(
                    this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    SensorManager.SENSOR_DELAY_UI
            );
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            CompassManager.gravityVals = CompassManager.lowPass(event.values.clone(), CompassManager.gravityVals);
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            CompassManager.geomagneticVals = CompassManager.lowPass(event.values.clone(), CompassManager.geomagneticVals);
        if (CompassManager.isReady() && LocationService.isReady()) {
            update();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }



    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(this.getClass().getSimpleName(), "Google Location Services Connected");
        LocationRequest mLocationRequest = LocationService.createLocationRequest();
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(this.getClass().getSimpleName(), "Connection to Google API suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(this.getClass().getSimpleName(), "Error in connecting Google API");
    }

    @Override
    /** Location Changed Handler */
    public void onLocationChanged(Location location) {
//        broadcastLocation(location);
        LocationService.currentLocation = location;
        Log.i(this.getClass().getSimpleName(), "Current Location: " + location.getLatitude() + ", " + location.getLongitude());
    }

    /**
     * Update the Compass orientation based on sensor changes
     * pre: Location, Target, and Azimuth must be set */
    private void update() {
        float direction = CompassManager.calculateDirection();
        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(CompassManager.currentDegree, -direction, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(210);
        ra.setFillAfter(true);
        compass.startAnimation(ra);
        CompassManager.currentDegree = -direction;
    }

    /**
     * Broadcasts Location
     * Callback: Publish Callback
     * @param location Updated Location
     * pre: Called when user location updates
     * post: Broadcast Location to Pubnub Channel */
    private void broadcastLocation(Location location) {
        JSONObject message = new JSONObject();
        try {
            message.put("lat", location.getLatitude());
            message.put("lng", location.getLongitude());
            message.put("alt", location.getAltitude());
        } catch (JSONException e) {
            Log.e(this.getClass().getSimpleName(), e.toString());
        }
        pubnubManager.client.publish(channelName, message, this.publishCallback());
    }

    /**
     * Publish Callback
     * Success: Logs
     * Error: Logs
     * @return Callback Post Publishing */
    private Callback publishCallback() {
        return new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
                Log.d("PUBNUB", response.toString());
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                Log.e("PUBNUB", error.toString());
            }
        };
    }

    /** Generate a random channel name. */ // TODO: Make this better lol
    private String randomChannelName() {
        String channelName = "";
        for(int i = 0; i < 5; i++) {
            channelName += genMajorMinor();
        }
        // fixme: return new BigInteger(130, new SecureRandom()).toString(32);
        return channelName;
    }

    /** Generate a random major and minor. */
    private int genMajorMinor() {
        return (int) (Math.random() * RANDOM_LARGE_NUMBER) + 1;
    }
}