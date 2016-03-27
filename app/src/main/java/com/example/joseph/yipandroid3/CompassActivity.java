package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;

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
import java.util.List;

import io.branch.referral.Branch;
import io.branch.referral.BranchError;

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
    /** Layout Elements */
    private ImageView compass;

    /** Device Sensor Manager */
    private SensorManager mSensorManager;

    /** Google API */
    private GoogleApiClient mGoogleApiClient;

    /** Tracks the user's yipType */
    private App.YipType yipType;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);
        App.currentContext = this.getApplicationContext();

        // init
        PubnubManager.init();
        this.compass = (ImageView) findViewById(R.id.compass);
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);
        this.mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.buildGoogleApiClient();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            App.YipType type = (App.YipType) extras.getSerializable("mode");
            if (type == App.YipType.ADDRESS_YIP) {

            }
            else if(type == App.YipType.TWO_USERS_YIP) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction fragmentTransaction = fm.beginTransaction();
                fragmentTransaction.hide(autocompleteFragment);
                fragmentTransaction.commit();
                PubnubManager.joinChannel();
            }
            else {

            }
        }
    }

    /**
     * Callback invoked when a place has been selected from the PlaceAutocompleteFragment.
     * @param place Place object of the user selected suggestion
     *              Contains Name, Id, Address, Phone, and Website */
    @Override
    public void onPlaceSelected(Place place) {
        Log.i(this.getClass().getSimpleName(), "Address Selected: " + place.getName() + " -- Located at "
                + place.getAddress());
        Geocoder geocoder = new Geocoder(this.getApplicationContext(), getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocationName(place.getAddress().toString(), 1);
            Double lat = addresses.get(0).getLatitude();
            Double lng = addresses.get(0).getLongitude();

            LocationService.setTargetLocation(lat, lng);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Callback invoked when PlaceAutocompleteFragment encounters an error. */
    @Override
    public void onError(Status status) {
        Log.e(this.getClass().getSimpleName(), "onError: Status = " + status.toString());
    }

    /** Initialize Google API Client
     * Adds Location Services API
     * Adds Places API */
    private synchronized void buildGoogleApiClient() {
        if (this.mGoogleApiClient == null) {
            this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .addApi(Places.GEO_DATA_API)
                    .build();
        }
    }

    @Override
    protected void onResume () {
        super.onResume();
        this.mSensorManager.registerListener(
                this,
                this.mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI
        );
        this.mSensorManager.registerListener(
                this,
                this.mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI
        );

        if (Branch.isAutoDeepLinkLaunch(this)) {
            try {
                String channelName = Branch.getInstance().getLatestReferringParams().getString("yip_channel");
                Log.i(this.getClass().getSimpleName(), "Channel Name Received -- " + channelName);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(this.getClass().getSimpleName(), "Launched by normal application flow");
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        this.mGoogleApiClient.connect();

        Branch branch = Branch.getInstance(getApplicationContext());
        branch.initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    // capture params here
                    String yip_channel = referringParams.optString("yip_channel", "");
                    Log.i(this.getClass().getSimpleName(), "Channel Name Received -- " + yip_channel);
                } else {
                    Log.i(this.getClass().getSimpleName(), "No yip_channel: " + error.getMessage());
                }
            }
        }, this.getIntent().getData(), this);

        super.onStart();
    }

    @Override
    protected void onStop() {
        this.mGoogleApiClient.disconnect();
        super.onStop();
        PubnubManager.terminate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        PubnubManager.terminate();
    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    /**
     * Install DeepLink Listener
     */
    public class InstallListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String rawReferrerString = intent.getStringExtra("referrer");   // use this for channelName?
            if(rawReferrerString != null) {
                Log.i(this.getClass().getSimpleName(), "Received the following intent " + rawReferrerString);
            }
        }
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
        LocationService.currentLocation = location;
        if(PubnubManager.isConnected) {
            try {
                PubnubManager.sendLocation(location, PubnubManager.publishCallback());
                Log.i(this.getClass().getSimpleName(), "Published new location");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i(this.getClass().getSimpleName(), "Not connected");
        }
        Log.i(this.getClass().getSimpleName(), "Current Location: " + location.getLatitude()
                + ", " + location.getLongitude());
        Log.i(this.getClass().getSimpleName(), "Current Channel Name: " + PubnubManager.getCurrentChannelName());
    }

    /**
     * Update the Compass orientation based on sensor changes
     * pre: Location, Target, and Azimuth must be set */
    private void update() {
        float direction = CompassManager.calculateDirection();
        RotateAnimation ra = new RotateAnimation(CompassManager.currentDegree, -direction, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(210);
        ra.setFillAfter(true);
        this.compass.startAnimation(ra);
        CompassManager.currentDegree = -direction;
    }

    /**
     * Publish Callback
     * @return A callback with success and error to log */
    private Callback publishCallback() {
        return new Callback() {
            public void successCallback(String channel, Object message) {
                Log.i(PubnubManager.class.getSimpleName(), "Successful publish to " + channel);
            }

            public void errorCallback(String channel, PubnubError error) {
                Log.e(PubnubManager.class.getSimpleName(), "Error publish to " + channel + " "
                        + error.getErrorString() + "code: " + error.errorCode);
            }
        };
    }
}