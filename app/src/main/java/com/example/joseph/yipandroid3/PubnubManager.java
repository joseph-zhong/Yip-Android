package com.example.joseph.yipandroid3;

import android.location.Location;
import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Joseph on 2/27/16.
 */
public class PubnubManager {

    private static Pubnub pubnub;

    /** Number used for UUID Generation */
    private static final int RANDOM_LARGE_NUMBER = 65535;

    /** Our received location */
    public static Location receivedLoc;

    /** Boolean tracking connection */
    public static boolean isConnected;

    /** Channel Name connected */
    private static String currentChannelName;

    /** */
    public static String uuid;

    /** Public initializer */
    public static void init() {
        pubnub = new Pubnub(App.currentContext.getString(R.string.pubnub_publish_key),
                App.currentContext.getString(R.string.pubnub_subscribe_key));
        pubnub.setResumeOnReconnect(true);
        isConnected = false;
    }

    /** @return boolean whether location has been received */
    public static boolean hasReceivedLoc() {
        return receivedLoc != null;
    }

    /** @return Location received */
    public static Location getReceivedLoc() {
        return receivedLoc;
    }

    /** @return Pubnub current client */
    public static Pubnub getPubnub() {
        return pubnub;
    }

    /** Joins Specified Channel
     * @param channelName Channel Name to join */
    public static void joinChannel(String channelName) {
        try {
            pubnub.subscribe(channelName, subscribeCallback());
        }
        catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /** Joins Channel */
    public static void joinChannel() {
        try {
            if (currentChannelName != null && !currentChannelName.isEmpty()) {
                pubnub.subscribe(currentChannelName, subscribeCallback());
            }
            else {
                pubnub.subscribe(getNewChannelName(), subscribeCallback());
            }
        }
        catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /** Joins Specified Channel with callback
     * @param channelName Channel Name to join
     * @param callback Callback Custom callback to use */
    public static void joinChannel(String channelName, Callback callback) {
        try {
            pubnub.subscribe(channelName, callback);
        }
        catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /** Helper method to unsubscribe */
    public static void terminate() {
        pubnub.unsubscribeAllChannels();
    }

    /** Method to send location across channel
     * @param location
     * @throws JSONException */
    public static void sendLocation(Location location, Callback callback) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("lat", location.getLatitude());
        obj.put("lng", location.getLongitude());
        obj.put("alt", location.getAltitude());
        obj.put("uuid", pubnub.uuid());

        pubnub.publish(currentChannelName, obj, callback);
        Log.i(PubnubManager.class.getSimpleName(), "Currently Subscribed to: "
                + pubnub.getCurrentlySubscribedChannelNames());
    }

    /**
     * Publish Callback
     * Success:
     * Error:
     * @return a S/E Callback */
    public static Callback publishCallback() {
        return new Callback() {
            public void successCallback(String channel, Object message) {
                Log.i(PubnubManager.class.getSimpleName(), "Successful publish to " + channel);
            }
            public void errorCallback(String channel, PubnubError error) {
                Log.e(PubnubManager.class.getSimpleName(), "Error publish to " + channel + " "
                        + error.getErrorString());
            }
        };
    }

    /**
     * Subscribe Callback
     * Connect: Logs
     * Disconnect: Logs
     * Reconnect: Logs
     * Success: Records location data received
     * Error: Logs
     * @return a S/E Callback */
    public static Callback subscribeCallback() {
        return new Callback() {
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
                    if (uuid.equals(pubnub.uuid())) {
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
    }

    /** Helper to return new name */
    public static String getNewChannelName() {
        currentChannelName = randomChannelName();
        return currentChannelName;
    }

    /** Return the current name -- CAN BE NULL */
    public static String getCurrentChannelName() {
        return currentChannelName;
    }

    /** Generate a random channel name. */ // TODO: Make this better lol
    private static String randomChannelName() {
        String channelName = "";
        for(int i = 0; i < 5; i++) {
            channelName += genMajorMinor();
        }
        // fixme: return new BigInteger(130, new SecureRandom()).toString(32);
        return channelName;
    }

    /** Generate a random major and minor. */
    private static int genMajorMinor() {
        return (int) (Math.random() * RANDOM_LARGE_NUMBER) + 1;
    }
}
