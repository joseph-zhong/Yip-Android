package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
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
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;
import com.pubnub.api.PubnubUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
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
    private Pubnub pubnub;

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
//        pubnub = PubnubManager.init();
        pubnub = new Pubnub(App.currentContext.getString(R.string.pubnub_publish_key),
                App.currentContext.getString(R.string.pubnub_subscribe_key));
        pubnub.setResumeOnReconnect(true);

        this.compass = (ImageView) findViewById(R.id.compass);
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setOnPlaceSelectedListener(this);
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
//                    pubnub.subscribe(PubnubManager.generateNewName(), subscribeCallback);
                PubnubManager.joinChannel(pubnub, PubnubManager.generateNewName(), subscribeCallback);
            }
            else {

            }


        }
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.buildGoogleApiClient();
    }

    /**
     * Subscribe Callback
     * Connect: Logs
     * Disconnect: Logs
     * Reconnect: Logs
     * Success: Records location data received
     * Error: Logs
     * @return a S/E Callback */
    public Callback subscribeCallback = new Callback() {
        @Override
        public void connectCallback(String channel, Object message) {
            Log.i(getClass().getSimpleName(), "SUBSCRIBE : CONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
            PubnubManager.isConnected = true;
        }

        @Override
        public void disconnectCallback(String channel, Object message) {
            Log.i(getClass().getSimpleName(), "SUBSCRIBE : DISCONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
            PubnubManager.isConnected = false;
        }

        @Override
        public void reconnectCallback(String channel, Object message) {
            Log.i(getClass().getSimpleName(), "SUBSCRIBE : RECONNECT on channel:" + channel
                    + " : " + message.getClass() + " : "
                    + message.toString());
        }

        @Override
        public void successCallback(String channel, Object message) {
            try {
                JSONObject json = new JSONObject(message.toString());
                Log.i(getClass().getSimpleName(), "Received message: " + json);
                String uuid = json.getString("uuid");
                if(uuid.equals(pubnub.uuid())) {
                    double lat = json.getDouble("lat");
                    double lng = json.getDouble("lng");
                    double alt = json.getDouble("alt");
                    Location loc = new Location("");
                    loc.setLatitude(lat);
                    loc.setLongitude(lng);
                    loc.setAltitude(alt);
                    LocationService.targetLocation = loc;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.i(getClass().getSimpleName(), "Successful subscription to " + channel);
        }

        @Override
        public void errorCallback(String channel, PubnubError error) {
            Log.e(getClass().getSimpleName(), "Error occured in " + channel + " "
                    + error.getErrorString());
        }
        };


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
        LocationService.currentLocation = location;
        if(PubnubManager.isConnected) {
            try {
                PubnubManager.sendLocation(this.pubnub, location, publishCallback);
//                JSONObject locJson = new JSONObject();
//                locJson.put("uuid", pubnub.uuid());
//                locJson.put("lat", location.getLatitude());
//                locJson.put("lng", location.getLongitude());
//                locJson.put("alt", location.getAltitude());
//                pubnub.publish(PubnubManager.getCurrentChannelName(), locJson, publishCallback);
                Log.i(this.getClass().getSimpleName(), "Published new location");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i(this.getClass().getSimpleName(), "Not connected");
        }

//        pubnub.publish(PubnubManager.currentChannelName, "Location : " + location.getLatitude() +
//                ", " + location.getLongitude(), publishCallback);
        Log.i(this.getClass().getSimpleName(), "Current Location: " + location.getLatitude() +
                ", " + location.getLongitude());
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

    private Callback publishCallback = new Callback() {
        public void successCallback(String channel, Object message) {
            Log.i(PubnubManager.class.getSimpleName(), "Successful publish to " + channel);
            // todo: send success...
        }
        public void errorCallback(String channel, PubnubError error) {
            Log.e(PubnubManager.class.getSimpleName(), "Error publish to " + channel + " "
                    + error.getErrorString());
            Log.e(PubnubManager.class.getSimpleName(), "code: " + error.errorCode);
            // todo: send error ...
        }
    };
}