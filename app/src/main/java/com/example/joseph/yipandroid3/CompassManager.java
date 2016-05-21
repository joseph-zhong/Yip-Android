package com.example.joseph.yipandroid3;

import android.hardware.GeomagneticField;
import android.hardware.SensorManager;
import android.location.Location;

import org.jscience.mathematics.number.Float64;
import org.jscience.mathematics.vector.Float64Matrix;
import org.jscience.mathematics.vector.Float64Vector;

/**
 * Created by Joseph on 2/27/16.
 */
public class CompassManager {
    /** Data arrays for each sensor */
    public static float[] gravityVals;
    public static float[] geomagneticVals;

    /** Instantaneous degress of current position */
    private static float declination;

    /** Heading Degree to north */
    public static float azimuth;

    /** Current Orientation */
    public static float currentDegree = 0f;

    /** Constant frequency for Low-Pass Filter */
    private static final float ALPHA = 0.25f; // if ALPHA = 1 OR 0, no filter applies

    /** Declination
     * @return float representing degrees of instantaneous Location position */
    public static void setDeclination() {
        declination = new GeomagneticField(
                (float) LocationService.currentLocation.getLatitude(),
                (float) LocationService.currentLocation.getLongitude(),
                (float) LocationService.currentLocation.getAltitude(),
                System.currentTimeMillis()).getDeclination();
    }

    /** Calculates Azimuth */
    public static void setAzimuth() {
        float R[] = new float[9];
        float I[] = new float[9];
        boolean success = SensorManager.getRotationMatrix(R, I, CompassManager.gravityVals,
                CompassManager.geomagneticVals);
        if (success) {
            float orientation[] = new float[3];
            SensorManager.getOrientation(R, orientation);
            azimuth = orientation[0]; // orientation contains: azimuth, pitch and roll
        }
    }

    /** Bearing
     * @return float representing bearing from curent position */
    public static float getBearing() {
        return (float) Math.toDegrees((double) azimuth) + declination
                - LocationService.currentLocation.bearingTo(LocationService.targetLocation);
    }

    /** Ready
     * @return true when both accelerometer and geomagnetic data has been collected */
    public static boolean isReady() {
        return CompassManager.gravityVals != null && CompassManager.geomagneticVals != null;
    }

    /** Yip Types */
    private enum YipType {
        REMEMBERED_LOCATION_YIP, ADDRESS_YIP, TWO_USERS_YIP
    };

    /** Initial Anchor Location */
    Location initialAnchorLocation;

    /** Other user's most recent location */
    Location anchorLocation;

    /** Initial User Location */
    Location initialUserLocation;

    /** User's most recent location */
    Location userLocation;

    /** User's most recent heading, in degrees */
    double userHeading;

    Float64Vector anchorVector;

    /** Radians for delta theta */
    double previousTheta;

    /** Initial difference between users */
    double initialDistanceDifference;

    private YipType YIP_TYPE;
    private String YIP_CHANNEL;

    private boolean obtainedInitialUserLocation;
    private boolean obtainedInitialAnchorLocation;
    private boolean calculatedInitialDistanceBetween;

    private boolean readyToTrack;

    private boolean friendIsReady;

    private static final double WARM_UP_TIME = 5.0;
    // Arrays to hold location points
    Location[] locationsCollection;
    Location[] validPoints;
    Location[] ignoredPoints;

//    var connectionStatus : ConnectionStatus!
//    //var lastConnectionStatus : ConnectionStatus = .NONE


    private App.CompassStatus COMPASS_STATUS;

    LocationService locationManager;

    private boolean sentFirstLocation;
    private boolean terminated;

    /**
     * Lowpass Filter
     * @param input
     * @param output
     * @return
     */
    protected static float[] lowPass(float[] input, float[] output) {
        if(output == null) return input;
        for(int i = 0; i < input.length; i++) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    /**
     * Helper method to set the destination
     * @param loc Received Other Location */
    private void setDestination(Location loc) {
        initialAnchorLocation = loc;
        anchorLocation = loc;
    }

    /** Updates the current connection status and alerts delegate if status has changed. */
    private void updateStatus(App.CompassStatus compassStatus) {
        this.COMPASS_STATUS = compassStatus;
    }

    /**
     * Calculates Position Vector from User to Other location
     * @param userLoc Location of the current user
     * @param oLoc Location of the other user
     * @return Position Vector between User and Other User */
    public static Float64Vector getPositionVector(Location userLoc, Location oLoc) {
        if(userLoc == null || oLoc == null) {
            return null;
        }
        else {
            double lat1 = Math.toRadians(userLoc.getLatitude());
            double lng1 = Math.toRadians(userLoc.getLongitude());
            double lat2 = Math.toRadians(oLoc.getLatitude());
            double lng2 = Math.toRadians(oLoc.getLongitude());

            double x = ((Math.cos(lat1))*(Math.sin(lat2)))
                    - ((Math.sin(lat1))*(Math.cos(lat2))*(Math.cos(lng2 - lng1)));
            double y = (Math.sin(lng2 - lng1)) * (Math.cos(lat2));

            return Float64Vector.valueOf(x, y);
        }
    }

    /**
     * Calculates the angle between the heading and position vector
     * @param vector
     * @param heading
     */
    public static double calculateTheta(Float64Vector vector, double heading) {
        Float64Vector uAnchorVector = normalizeVector(vector);
        Float64Vector uHeadingVector = Float64Vector.valueOf(Math.cos(Math.toRadians(heading)),
                Math.sin(Math.toRadians(heading)));
        Float64 dotProduct = uAnchorVector.times(uHeadingVector);
        Float64 determinant = determinant(uAnchorVector, uHeadingVector);

        return Math.atan2(determinant.doubleValue(), dotProduct.doubleValue());
    }

    /**
     * Returns the unit vector of the input vector
     * @param v vector to normalize
     * @return Unit Vector of the input
     * pre: Vector v is of two dimensions */
    private static Float64Vector normalizeVector(Float64Vector v) {
        return Float64Vector.valueOf(v.getValue(0) / v.normValue(), v.getValue(1) / v.normValue());
    }

    /**
     *
     * @param v1
     * @param v2
     * @return Determinant of v1 and v2 */
    private static Float64 determinant(Float64Vector v1, Float64Vector v2) {
        Float64Matrix m = Float64Matrix.valueOf(v1, v2);
        return m.determinant();
    }
}