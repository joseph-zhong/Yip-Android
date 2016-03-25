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

    /** Global Pubnub client */
    public static Pubnub client = new Pubnub("pub-c-1b3b7682-6fc8-40f4-b51f-d10e79987840",
            "sub-c-0df608f6-5430-11e5-85f6-0619f8945a4f");;

    /** Our received location */
    private static Location receivedLoc;

    /** Boolean tracking connection */
    private static boolean isConnected;

    /** Channel Name connected */
    private static String CHANNEL_NAME = "joseph-reported";

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
    public static void joinChannel(String channelName) {
        try {
            client.subscribe(channelName, subscribeCallback());
        }
        catch (PubnubException e) {
            e.printStackTrace();
        }
    }

    /** Helper method to unsubscribe */
    public static void terminate() {
        client.unsubscribeAllChannels();
    }

    /** Method to send location across channel
     * @param location
     * @throws JSONException */
    public static void sendLocation(Location location) throws JSONException {
        String uuid = client.uuid();

        JSONObject obj = new JSONObject();
        obj.put("lat", location.getLatitude());
        obj.put("lng", location.getLongitude());
        obj.put("alt", location.getAltitude());
        obj.put("uuid", uuid);

        client.publish(CHANNEL_NAME, obj, publishCallback());
    }

    /**
     * Publish Callback
     * Success:
     * Error:
     * @return a S/E Callback */
    private static Callback publishCallback() {
        return new Callback() {
            public void successCallback(String channel, Object message) {
                Log.i(PubnubManager.class.getSimpleName(), "Successful publish to " + channel);
                // todo: send success...
            }
            public void errorCallback(String channel, PubnubError error) {
                Log.e(PubnubManager.class.getSimpleName(), "Error publish to " + channel + " "
                        + error.getErrorString());
                // todo: send error ...
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
    private static Callback subscribeCallback() {
        return new Callback() {
            @Override
            public void connectCallback(String channel, Object message) {
                Log.i(getClass().getSimpleName(), "SUBSCRIBE : CONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            @Override
            public void disconnectCallback(String channel, Object message) {
                Log.i(getClass().getSimpleName(), "SUBSCRIBE : DISCONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            @Override
            public void reconnectCallback(String channel, Object message) {
                Log.i(getClass().getSimpleName(), "SUBSCRIBE : RECONNECT on channel:" + channel
                        + " : " + message.getClass() + " : "
                        + message.toString());
            }

            @Override
            public void successCallback(String channel, Object message) {
                JSONObject json;
                try {
                    json = new JSONObject(message.toString());
                    Log.i(getClass().getSimpleName(), "Received message: " + json);
                    double lat = json.getDouble("lat");
                    double lng = json.getDouble("lng");
                    double alt = json.getDouble("alt");
                    Location loc = new Location("");
                    loc.setLatitude(lat);
                    loc.setLongitude(lng);
                    loc.setAltitude(alt);
                    receivedLoc = loc;
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
