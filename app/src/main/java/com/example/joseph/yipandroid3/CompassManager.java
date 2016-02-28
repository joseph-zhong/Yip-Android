package com.example.joseph.yipandroid3;

import android.location.Location;
import android.support.v7.app.AppCompatActivity;

import org.jscience.mathematics.number.Float64;
import org.jscience.mathematics.vector.Float64Matrix;
import org.jscience.mathematics.vector.Float64Vector;

/**
 * Created by Joseph on 2/27/16.
 */
public class CompassManager {
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

    /** Vector from user's location to friend's location
     * @param userHeading
     * @param anchorLocation
     * @param initialAnchorLocation */

    /** */
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

    LocationManager locationManager;

    /** main manager */
    private PubnubManager pubnubManager;

    private boolean sentFirstLocation;
    private boolean terminated;

    /** Constructor */
    public CompassManager(double userHeading, Location anchorLocation, Location initialAnchorLocation) {
        this.obtainedInitialAnchorLocation = false;
        this.obtainedInitialUserLocation = false;
        this.calculatedInitialDistanceBetween = false;

        this.userHeading = userHeading;
        this.anchorLocation = anchorLocation;
        this.initialAnchorLocation = initialAnchorLocation;
        this.sentFirstLocation = false;
        this.terminated = false;

        this.locationManager = new LocationManager((AppCompatActivity) App.activity);
        this.pubnubManager = new PubnubManager();
    }

    /**
     * Helper method to set the destination
     * @param loc Received Other Location */
    private void setDestination(Location loc) {
        initialAnchorLocation = loc;
        anchorLocation = loc;
    }

    /**
     * Helper method to join Yip Channel */
    private void joinYipChannel(String channel) {
        YIP_CHANNEL = channel;
        this.pubnubManager.joinChannel(YIP_CHANNEL);
    }

    /** Updates the current connection status and alerts delegate if status has changed. */
    private void updateStatus(App.CompassStatus compassStatus) {
        this.COMPASS_STATUS = compassStatus;
    }

    /** Terminates the instance of CompassLogic. Delegate updates will be stopped. */
    public void terminate() {
        if (YIP_TYPE == YipType.TWO_USERS_YIP) {
            pubnubManager.terminate();
            pubnubManager = null;
        }
    }

//------------------------------------------Vector Math--------------------------------------------------

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

        // todo: integration with delta
        // todo: also see if PID looping is relevant here
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

//------------------------------------------Data Stream--------------------------------------------------
//    func stream(statusChanged on: Bool) {
//    }
//
//    func stream(connected success: Bool) {
//        if success {
//            if userLocation != nil {
//                streamManager.sendLocation(userLocation!)
//            }
//        }
//    }
//
//    func stream(friendConnected success: Bool) {
//        if success {
//            print("Friend connected.")
//
//            if userLocation != nil {
//                streamManager.sendLocation(userLocation!)
//            }
//
//            // We haven't received a location yet, so we are the only "ready" member in the channel
//            if initialAnchorLocation == nil {
//                updateStatus(.WaitingForFriend)
//            }
//        }
//    }
//
//    func stream(friendDisconnected success: Bool) {
//        if success {
//            print("Friend disconnected.")
//        }
//    }
//
//    func stream(friendTimedOut success: Bool) {
//        if success {
//            print("Friend timed out")
//        }
//    }
//
//    func stream(locationReceived location: CLLocation) {
//        print("Received new location.")
//        if anchorLocation == nil {
//            initialAnchorLocation = location
//
////            if initialUserLocation != nil {
////                updateStatus(.READY)
////            }
//        }
//
//        // Save location to class
//        anchorLocation = location
//
//        if userLocation != nil && anchorLocation != nil {
//            // Compute new anchor vector and save to class
//            anchorVector = calculatePositionVector(userLocation!, anchorPosition: location)
//
//            // Compute new theta
//            calculateTheta(anchorVector, heading: userHeading)
//        }
//    }
}