package com.example.joseph.yipandroid3;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ImageView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

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
        GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener,
        PlaceSelectionListener, OnMapReadyCallback {
    /** Layout Elements */
    private ImageView compass;
    private MapFragment mapFragment;
    private CircleOverlay circleOverlay;
    private GoogleMap mMap;

    /** Device Sensor Manager */
    private SensorManager mSensorManager;

    /** Google API */
    private GoogleApiClient mGoogleApiClient;

    /** Tracks the user's yipType */
    private App.YipType yipType;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        App.currentContext = this.getApplicationContext();
        setContentView(R.layout.activity_compass);

//        todo: figure out how to make overlay
//        CircleOverlay circleOverlay = new CircleOverlay(getApplicationContext());
//        circleOverlay.setCircle(new RectF(0,0, 200, 300), 20);
//
//        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT);
//        RelativeLayout mainRL = (RelativeLayout) findViewById(R.id.activity_compass_main_rl);
//        mainRL.addView(circleOverlay, params);

        // init
        PubnubManager.init();
        this.compass = (ImageView) findViewById(R.id.compass);
        this.mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        this.mapFragment.getMapAsync(this);

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
                if(PubnubManager.isCurrentChannelNameValid()) {
                    PubnubManager.joinChannel();
                }
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

            LocationService.setTargetLocation(lat, lng, 0, 0);
            this.resetMarkers();
            this.mMap.addMarker(new MarkerOptions()
                    .position(LocationService.targLocToLatLng())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            updateBounds();
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
//            this.mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume () {
        super.onResume();

        if (Branch.isAutoDeepLinkLaunch(this)) {
            PubnubManager.init();
            try {
                String yip_channel = Branch.getInstance().getLatestReferringParams().getString("yip_channel");
                Log.i(this.getClass().getSimpleName(), "Channel Name Received onResume() -- " + yip_channel);
                if(!PubnubManager.isConnected() && !yip_channel.isEmpty()) {
                    Log.i(this.getClass().getSimpleName(), "Attempting to join channel: " + yip_channel);
                    PubnubManager.setCurrentChannelName(yip_channel);
//                    PubnubManager.joinChannel();
                }
                else {
                    // todo: figure what the heck is going on
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(this.getClass().getSimpleName(), "Launched by normal application flow");
        }

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
        this.mSensorManager.registerListener(
                this,
                this.mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_UI
        );
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    try {
                        Log.i(this.getClass().getSimpleName(), referringParams.toString(4));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String yip_channel = referringParams.optString("yip_channel", "");
                    Log.i(this.getClass().getSimpleName(), "Channel Name Received onStart() -- " + yip_channel);
                    if (!PubnubManager.isConnected() && !yip_channel.isEmpty()) {
                        PubnubManager.setCurrentChannelName(yip_channel);
                        PubnubManager.joinChannel();
                    } else {
                        // todo: figure what the heck is going on
                    }
                } else {
                    Log.i(this.getClass().getSimpleName(), "Error in Initializing Branch Session: " + error.getMessage());
                }
            }
        }, this.getIntent().getData(), this);

        this.mGoogleApiClient.connect();
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

    @Override
    /** Handles map updating when ready */
    public void onMapReady(GoogleMap googleMap) {
        Log.i(this.getLocalClassName(), "Map Ready");
        this.mMap = googleMap;

//        this.mMap.getUiSettings().setAllGesturesEnabled(false);

        if(LocationService.isReady()) {
            // update with current location
            // todo: add range and orientation
            this.mMap.addMarker(new MarkerOptions()
                    .position(LocationService.currLocToLatLng())
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
            // todo: zoom based on distance
            this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    LocationService.currLocToLatLng(), LocationService.ZOOM_LEVEL_STREET));

        }
        else {
            if(LocationService.hasCurrentLocation()) {
                this.mMap.addMarker(new MarkerOptions()
                        .position(new LatLng(LocationService.currentLocation.getLatitude(),
                                LocationService.currentLocation.getLongitude()))
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));

                this.mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LocationService.currLocToLatLng(), LocationService.ZOOM_LEVEL_STREET));
            }
        }
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
        boolean avg = false;
        SensorEvent prevEvt = null;
        float b1 = 0f;
        float b2 = 0f;
        if(LocationService.hasCurrentLocation() && LocationService.hasTargetLocation()
                && event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            CompassManager.rotationVals = CompassManager.lowPass(event.values.clone(), CompassManager.rotationVals);
//            Log.i(this.getLocalClassName(), "Bear_Rotation: " + CompassManager.getBearingFromRotEvent(event));
            prevEvt = event;
            avg = true;
            b1 = CompassManager.getBearingFromRotEvent(event);
//            update(b1);
        }
        if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            CompassManager.gravityVals = CompassManager.lowPass(event.values.clone(), CompassManager.gravityVals);
        }
        if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            CompassManager.geomagneticVals = CompassManager.lowPass(event.values.clone(), CompassManager.geomagneticVals);
        }
        if(CompassManager.isReady() && LocationService.isReady()) {
            CompassManager.setAzimuth();
            b2 = CompassManager.getBearing();
//            Log.i(this.getLocalClassName(), "Bear_Azimuth: " + b2);
            if(!avg) {
                avg = !avg;
            }
//            update(b2);
//            update();
        }

        if(avg && prevEvt != null) {
//            Log.i(this.getLocalClassName(), "Bear_Avg: " + ((b1 + b2) / 2));
            update(CompassManager.getBearingFromAvg(b1, b2));
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // todo: incorporate this into angle detection
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(this.getClass().getSimpleName(), "Google Location Services Connected");

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
        Log.i(this.getClass().getSimpleName(), "Current Location: " + location.getLatitude()
                + ", " + location.getLongitude());
        // update position and declination
        LocationService.currentLocation = location;
        CompassManager.setDeclination();

        if(PubnubManager.isConnected()) {
            try {
                PubnubManager.sendLocation(location);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i(this.getClass().getSimpleName(), "Not connected");
            if(PubnubManager.isCurrentChannelNameValid()) {
                PubnubManager.joinChannel();
            }
            else {
                // todo: figure what the heck is going on
            }
        }
    }

    /**
     * Update the Compass orientation based on sensor changes
     * pre: Location, Target, Declination must be set */
    private void update() {
//        Log.i(this.getLocalClassName(), "Dec:" + CompassManager.declination + " Azi: " + CompassManager.azimuth + " Bear: + " + CompassManager.getBearing());
        CompassManager.setAzimuth();
        CameraPosition oldPos = this.mMap.getCameraPosition();
        CameraPosition pos = CameraPosition.builder(oldPos)
                .bearing(CompassManager.getBearing())
                .target(LocationService.currLocToLatLng())
                .build();
        this.changeCamera(CameraUpdateFactory.newCameraPosition(pos), null);
    }

    private void update(float bearing) {
        CameraPosition oldPos = this.mMap.getCameraPosition();
        CameraPosition pos = CameraPosition.builder(oldPos)
                .bearing(bearing)
                .target(LocationService.currLocToLatLng())
                .build();
        this.changeCamera(CameraUpdateFactory.newCameraPosition(pos), null);
        if(!(isCurrentLocDisplayed() && isTargLocDisplayed())) {
            this.mMap.animateCamera(CameraUpdateFactory.zoomOut());
        }
    }

    /**
     * Change the camera position by moving or animating the camera depending on the state of the
     * animate toggle button.
     * pre: Duration must be strictly positive so we make it at least 1.
     */
    private void changeCamera(CameraUpdate update, GoogleMap.CancelableCallback callback) {
//        Log.i(this.getLocalClassName(), "Animating Camera");
        this.mMap.animateCamera(update, Math.max(210, 1), callback);
    }

    /** Helper to handle clearing target markers */
    private void resetMarkers() {
        this.mMap.clear();
        this.mMap.addMarker(new MarkerOptions()
                .position(LocationService.currLocToLatLng())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    /** Helper to update camera view Bounds*/
    private void updateBounds() {
        if(isCurrentLocDisplayed() && isTargLocDisplayed()) {
            Log.i(this.getLocalClassName(), "Points of interest captured");
        }
        else {
            Log.i(this.getLocalClassName(), "Points of interest not captured");
        }
    }

    /** Return whether map displays  */
    private boolean isTargLocDisplayed() {
        return this.mMap.getProjection().getVisibleRegion().latLngBounds.contains(LocationService.targLocToLatLng());
    }

    /** */
    private boolean isCurrentLocDisplayed() {
        return this.mMap.getProjection().getVisibleRegion().latLngBounds.contains(LocationService.currLocToLatLng());
    }
}