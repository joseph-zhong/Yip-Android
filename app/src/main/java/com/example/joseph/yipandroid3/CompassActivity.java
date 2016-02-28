package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

public class CompassActivity extends Activity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener {

    // define the display assembly compass picture
    private ImageView image;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

//    TextView tvHeading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        Bundle extras = getIntent().getExtras();
        double lat;
        double lng;

        if (extras != null) {
            lat = extras.getDouble("lat");
            lng = extras.getDouble("lng");
            Log.i(this.getClass().getSimpleName(), "lat: " + lat + " lng: " + lng);
        }

        image = (ImageView) findViewById(R.id.compass);


        // TextView that will tell the user what degree is the heading
//        tvHeading = (TextView) findViewById(R.id.tvHeading);

        // initialize your android device sensor capabilities
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // for the system's orientation sensor registered listeners
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION),
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // to stop the listener and save battery
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);

        Log.i(this.getClass().getSimpleName(), "Heading: " + Float.toString(degree) + " degrees");
//        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");

        // create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF,
                0.5f);

        // how long the animation will take place
        ra.setDuration(210);

        // set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);
        currentDegree = -degree;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // not in use
    }


    /** Number used for UUID Generation */
    private static final int RANDOM_LARGE_NUMBER = 65535;

    /** Google API */
    private GoogleApiClient mGoogleApiClient;

    /** Randomly generated channelName */
    private static String channelName;

    private PubnubManager pubnubManager;

    private synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this).addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
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
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, (LocationListener) this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(this.getClass().getSimpleName(), "Connection to Google API suspended");
    }

    /** Create Location Request */
    private LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    /** Location Changed Handler */
    public void onLocationChanged(Location location) {
        broadcastLocation(location);
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