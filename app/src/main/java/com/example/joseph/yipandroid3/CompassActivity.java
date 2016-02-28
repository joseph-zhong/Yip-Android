package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.AdapterView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ImageView;
import android.widget.SimpleAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;


public class CompassActivity extends Activity implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, LocationListener, GoogleApiClient.OnConnectionFailedListener {
    /** Number used for UUID Generation */
    private static final int RANDOM_LARGE_NUMBER = 65535;

    /** Layout Elements */
    private ImageView compass;
    private MultiAutoCompleteTextView multiAutoCompleteTextView;

    private String[] locations;

    /** Current Compass Orientation */
    private float currentDegree;

    /** Device Sensor Manager */
    private SensorManager mSensorManager;

    /** Google API */
    private GoogleApiClient mGoogleApiClient;
    private PlaceAutocompleteAdapter mAdapter;
    PlacesTask placesTask;
    ParserTask parserTask;

    // todo: pubnub streaming
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

        /** init */
        locations = new String[5];
        if(LocationService.hasCurrentLocation()) {
            mAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, new LatLngBounds(
                    new LatLng(LocationService.currentLocation.getLatitude() - 30, LocationService.currentLocation.getLongitude() - 30),
                    new LatLng(LocationService.currentLocation.getLatitude() + 30, LocationService.currentLocation.getLongitude() + 30)
            ), null);
        }
        else {
            mAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient, new LatLngBounds(
                    new LatLng(0, 0),
                    new LatLng(100, 100)
            ), null);
        }
        this.compass = (ImageView) findViewById(R.id.compass);

        this.multiAutoCompleteTextView = (MultiAutoCompleteTextView) findViewById(R.id.addressSearch);
        this.multiAutoCompleteTextView.setOnItemClickListener(mAutocompleteClickListener);
        this.multiAutoCompleteTextView.addTextChangedListener(onSearchAddressChange());
        this.multiAutoCompleteTextView.setAdapter(mAdapter);

        currentDegree = 0f;

        if (extras != null) {
            lat = extras.getDouble("lat");
            lng = extras.getDouble("lng");
            type = (App.YipType) extras.getSerializable("mode");

            if (type != App.YipType.ADDRESS_YIP) {
                this.multiAutoCompleteTextView.setVisibility(View.INVISIBLE);
            }

            LocationService.targetLocation = new Location("");
            LocationService.targetLocation.setLatitude(lat);
            LocationService.targetLocation.setLongitude(lng);

            Log.i(this.getClass().getSimpleName(), "Target Location: " + lat + ", " + lng);
        }
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        this.buildGoogleApiClient();
    }

    /** Returns the place description corresponding to the selected item */
    protected CharSequence convertSelectionToString(Object selectedItem) {
        /** Each item in the autocompetetextview suggestion list is a hashmap object */
        HashMap<String, String> hm = (HashMap<String, String>) selectedItem;
        return hm.get("description");
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

    private TextWatcher onSearchAddressChange() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                placesTask = new PlacesTask();
                placesTask.execute(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
                // do nothing
            }
        };
    }

    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }
        catch(Exception e){
            Log.e(this.getClass().getSimpleName(), "Exception while downloading url:" + e.toString());
        }
        finally{
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches all places from GooglePlaces AutoComplete Web Service
    private class PlacesTask extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... place) {
            // For storing data from web service
            String data = "";

            // Obtain browser key from https://code.google.com/apis/console
            String key = "key=AIzaSyDRv4boxFuC4TIF8H-tGOUYJbylvUoB-qM";

            String input="";

            try {
                input = "input=" + URLEncoder.encode(place[0], "utf-8");
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }

            // place type to be searched
            String types = "types=geocode";

            // Sensor enabled
            String sensor = "sensor=false";

            // Building the parameters to the web service
            String parameters = input+"&"+types+"&"+sensor+"&"+key;

            // Output format
            String output = "json";

            // Building the url to the web service
            String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"+output+"?"+parameters;

            try{
                // Fetching the data from we service
                data = downloadUrl(url);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            // Creating ParserTask
            parserTask = new ParserTask();

            // Starting Parsing the JSON string returned by Web Service
            parserTask.execute(result);
        }
    }
    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<HashMap<String,String>>> {

        JSONObject jObject;

        @Override
        protected List<HashMap<String, String>> doInBackground(String... jsonData) {

            List<HashMap<String, String>> places = null;

            PlaceJSONParser placeJsonParser = new PlaceJSONParser();

            try{
                jObject = new JSONObject(jsonData[0]);

                // Getting the parsed data as a List construct
                places = placeJsonParser.parse(jObject);

            }catch(Exception e){
                Log.d("Exception",e.toString());
            }
            return places;
        }

        @Override
        protected void onPostExecute(List<HashMap<String, String>> result) {

            String[] from = new String[] { "description"};
            int[] to = new int[] { android.R.id.text1 };

            // Creating a SimpleAdapter for the AutoCompleteTextView
            SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result, android.R.layout.simple_list_item_1, from, to);

            // Setting the adapter
            multiAutoCompleteTextView.setAdapter(adapter);
        }
    }

    /**
     * Listener that handles selections from suggestions from the multiAutoCompleteTextView that
     * displays Place suggestions.
     * Gets the place id of the selected item and issues a request to the Places Geo Data API
     * to retrieve more details about the place.
     *
     * @see com.google.android.gms.location.places.GeoDataApi#getPlaceById(com.google.android.gms.common.api.GoogleApiClient,
     * String...)
     */
    private AdapterView.OnItemClickListener mAutocompleteClickListener
            = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            /*
             Retrieve the place ID of the selected item from the Adapter.
             The adapter stores each Place suggestion in a AutocompletePrediction from which we
             read the place ID and title.
              */
            final AutocompletePrediction item = mAdapter.getItem(position);
            final String placeId = item.getPlaceId();
            final CharSequence primaryText = item.getPrimaryText(null);

            Log.i(this.getClass().getSimpleName(), "Autocomplete item selected: " + primaryText);

            /*
             Issue a request to the Places Geo Data API to retrieve a Place object with additional
             details about the place.
              */
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);

            Log.i(this.getClass().getSimpleName(), "Called getPlaceById to get Place details for " + placeId);
        }
    };

    /**
     * Callback for results from a Places Geo Data API query that shows the first place result in
     * the details view on screen.
     */
    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback
            = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                // Request did not complete successfully
                Log.e(this.getClass().getSimpleName(), "Place query did not complete. Error: " + places.getStatus().toString());
                places.release();
                return;
            }
            for (int i = 0; i < 5; i++) {
                if (i < places.getCount()) {
                    locations[i] = String.valueOf(places.get(i).getAddress());
                } else {
                    break;
                }
            }
            places.release();
        }
    };

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
        RotateAnimation ra = new RotateAnimation(currentDegree, -direction, Animation.RELATIVE_TO_SELF,
                0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        ra.setDuration(210);
        ra.setFillAfter(true);
        compass.startAnimation(ra);
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