package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

/**
 * Created by Joseph on 12/19/15.
 * Updated by Joseph on 12/19/15.
 *
 *  */
public class LocationService extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {
    /** Request code for approving location permissions */
    public static final int REQUEST_LOCATION_ID = 2;

    public static final int ZOOM_LEVEL_WORLD = 1;
    public static final int ZOOM_LEVEL_CONT = 5;
    public static final int ZOOM_LEVEL_CITY = 10;
    public static final int ZOOM_LEVEL_STREET = 15;
    public static final int ZOOM_LEVEL_BUILDING = 20;

    /** Location Requesting Manager */
    private LocationRequest locationRequest;

    /** Google Play Client */
    private GoogleApiClient googleApiClient;

    /** Activity accessor for Android specific information */
    private Activity activity;

    /** User's most recent location*/
    private Location mostRecentLoc;

    /** Other user's most recent location*/
    private Location oMostRecentLoc;

    /** Globals for Updating Compass */
    public static Location targetLocation;
    public static Location currentLocation;

    /**
     * Constructor
     * @param activity The activity that's accessing the LocationService */
    public LocationService(Activity activity) {
        this.activity = activity;
        this.googleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        this.locationRequest = new LocationRequest();
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Tries to get location
     * @return Most recent user location available */
    public Location getLocation() {
        if (ActivityCompat.checkSelfPermission(this.activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this.activity,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestLocPermissions(this.activity);
        }
        this.mostRecentLoc = LocationServices.FusedLocationApi.getLastLocation(this.googleApiClient);
        return getMostRecentLoc();
    }

    /** @return the most recently saved Location
     * pre: Location must be set */
    public static Location getMostRecentLoc() {
        if(hasCurrentLocation()) {
            return currentLocation;
        }
        String errorMessage = "Current Location not set yet...";
        Log.e("LocationService", errorMessage);
//      throw new IllegalStateException(errorMessage);
        return null;
    }

    public static boolean hasCurrentLocation() {
        return currentLocation != null;
    }

    public static boolean hasTargetLocation() {
        return targetLocation != null;
    }

    public static boolean isReady() {
        return LocationService.currentLocation != null && LocationService.targetLocation != null;
    }

    /** Starts Location Updating service */
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this.activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            this.requestLocPermissions(this.activity);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(this.googleApiClient, this.locationRequest, this);
    }

    /**  */
    protected static void attemptLocationUpdates(GoogleApiClient mGoogleApiClient, Activity activity) {
        LocationRequest mLocationRequest = LocationService.createLocationRequest();
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LocationService.REQUEST_LOCATION_ID);
            requestLocPermissions(activity);
        }
        else {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest,
                    (LocationListener) activity);
        }
    }

    /** Create Location Request */
    public static LocationRequest createLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
//        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    /** Stops Location Updating service */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(this.googleApiClient, this);
    }

    /** Requests for location permissions if necessary */
    private static void requestLocPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                REQUEST_LOCATION_ID);
    }

    /** Upon connecting to Google Play Services */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        this.startLocationUpdates();
    }

    /** Upon suspending Google Play Services */
    @Override
    public void onConnectionSuspended(int i) {}

    /** Upon failing connection with Google Play Services */
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {}

    /** Start Location gathering */
    public void startServices() {
        this.googleApiClient.connect();
    }

    /** Stop Location gathering */
    public void stopServices() {
        this.googleApiClient.disconnect();
    }

    /** Handler for Location Changes */
    @Override
    public void onLocationChanged(Location location) {
        this.mostRecentLoc = location;
    }

    public void setOMostRecentLoc(Location l) {
        this.oMostRecentLoc = l;
    }

    public static void setTargetLocation(Location l) {
        targetLocation = l;
    }

    public static void setTargetLocation(double lat, double lng, double alt, float acc) {
        Location l = new Location("Target Location");
        l.setLatitude(lat);
        l.setLongitude(lng);
        l.setAltitude(alt);
        l.setAccuracy(acc);
        setTargetLocation(l);
    }

    public static LatLng currLocToLatLng() {
        return LocToLatLng(currentLocation);
    }

    public static LatLng targLocToLatLng() {
        return LocToLatLng(targetLocation);
    }

    /** Helper to quickly convert from Location to LatLng
     * @param loc Location to retrieve LatLng Position*/
    public static LatLng LocToLatLng(Location loc) {
        return new LatLng(loc.getLatitude(), loc.getLongitude());
    }

    /** Helper to convert Address to Location */
    public static Location AddressToLocation(Address add) {
        Location l = new Location("Unknown");
        l.setLatitude(add.getLatitude());
        l.setLongitude(add.getLatitude());
        return l;
    }
}
