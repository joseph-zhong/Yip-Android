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
    /** Number used for UUID Generation */
    private static final int RANDOM_LARGE_NUMBER = 65535;

    /** Our received location */
    public static Location receivedLoc;

    /** Boolean tracking connection */
    public static boolean isConnected;

    /** Channel Name connected */
    private static String currentChannelName;

    public static Pubnub init() {
        return new Pubnub(App.currentContext.getString(R.string.pubnub_publish_key),
                App.currentContext.getString(R.string.pubnub_subscribe_key));
    }

    /** @return boolean whether location has been received */
    public static boolean hasReceivedLoc() {
        return receivedLoc != null;
    }

    /** @return Location received */
    public static Location getReceivedLoc() {
        return receivedLoc;
    }

    /** Joins Specified Channel
     * @param channelName Channel Name to join */
    public static void joinChannel(Pubnub client, String channelName) {
        try {
            client.subscribe(channelName, subscribeCallback(client));
        }
        catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /** Joins Random Channel */
    public static void joinChannel(Pubnub client) {
        try {
//            client.subscribe(randomChannelName(), subscribeCallback());
            client.subscribe(currentChannelName, subscribeCallback(client));
        }
        catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /** Joins Specified Channel
     * @param channelName Channel Name to join */
    public static void joinChannel(Pubnub client, String channelName, Callback callback) {
        try {
            client.subscribe(channelName, callback);
        }
        catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /** Helper method to unsubscribe */
    public static void terminate(Pubnub client) {
        client.unsubscribeAllChannels();
    }

    /** Method to send location across channel
     * @param location
     * @throws JSONException */
    public static void sendLocation(Pubnub client, Location location, Callback callback) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("lat", location.getLatitude());
        obj.put("lng", location.getLongitude());
        obj.put("alt", location.getAltitude());
        obj.put("uuid", client.uuid());

        client.publish(currentChannelName, obj, callback);
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
    public static Callback subscribeCallback(final Pubnub client) {
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
                    if (uuid.equals(client.uuid())) {
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
    public static String generateNewName() {
        currentChannelName = randomChannelName();
        return currentChannelName;
    }

    /** Return the current name if available, otherwise sets a new one */
    public static String getCurrentChannelName() {
        if (currentChannelName != null && !currentChannelName.isEmpty()) {
            return currentChannelName;
        }
        return generateNewName();
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
