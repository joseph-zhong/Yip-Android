package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;


public class CompassActivity extends Activity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    /** Number used for UUID Generation */
    private static final int RANDOM_LARGE_NUMBER = 65535;

    /** Compass Image */
    private ImageView image;

    /** Current Compass Orientation */
    private float currentDegree;

    /** Device Sensor Manager */
    private SensorManager mSensorManager;

    /** Sensor Data */
    private float[] mGravity;
    private float[] mGeomagnetic;

    /** Globals for Updating Compass */
    private float azimuth;
    private Location location;
    private Location target;

    /** Google API */
    private GoogleApiClient mGoogleApiClient;

    /** Randomly generated channelName */
    private static String channelName;

    private PubnubManager pubnubManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        Bundle extras = getIntent().getExtras();
        double lat;
        double lng;
        App.YipType type;

        if(extras != null) {
            lat = extras.getDouble("lat");
            lng = extras.getDouble("lng");
            type = (App.YipType) extras.getSerializable("mode");
            target = new Location("");
            target.setLatitude(lat);
            target.setLongitude(lng);
            Log.i(this.getClass().getSimpleName(), "Target Location: " + lat + ", " + lng);
        }

        /** init */
        image = (ImageView) findViewById(R.id.compass);
        currentDegree = 0f;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.buildGoogleApiClient();
    }

    @Override
    protected void onResume() {
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

        // to stop the listener and save battery
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
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            this.mGravity = event.values;
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            this.mGeomagnetic = event.values;
        if (this.mGravity != null && this.mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];
            boolean success = SensorManager.getRotationMatrix(R, I, this.mGravity, this.mGeomagnetic);
            if (success) {
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);
                this.azimuth = orientation[0]; // orientation contains: azimut, pitch and roll
            }
        }
        update();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }

    /** Helper method to initialize Google API Client for Location Services */
    private synchronized void buildGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(this.getClass().getSimpleName(), "Google Location Services Connected");
        LocationRequest mLocationRequest = createLocationRequest();
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

    /** Create Location Request */
    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    /** Location Changed Handler */
    public void onLocationChanged(Location location) {
//        broadcastLocation(location);
        this.location = location;
    }

    /**
     * Update the Compass orientation based on sensor changes
     * todo: see if location changes should update
     * pre: Location, Target, and Azimuth must be set */
    private void update() {
        if(this.location == null || this.target == null) {
            return;
        }
        float newAzimuth = (float) Math.toDegrees((double) this.azimuth);
        GeomagneticField geoField = new GeomagneticField(
                (float) this.location.getLatitude(),
                (float) this.location.getLongitude(),
                (float) this.location.getAltitude(),
                System.currentTimeMillis());
        newAzimuth += geoField.getDeclination(); // converts magnetic north into true north
        float bearing = this.location.bearingTo(this.target);
        float direction = newAzimuth - bearing;

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(currentDegree, -direction, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(210);
        ra.setFillAfter(true);
        image.startAnimation(ra);
        currentDegree = -direction;
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