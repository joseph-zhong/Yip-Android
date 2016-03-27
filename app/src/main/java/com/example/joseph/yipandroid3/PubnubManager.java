package com.example.joseph.yipandroid3;

import android.location.Location;
import android.util.Log;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.IllegalFormatCodePointException;

/**
 * Created by Joseph on 2/27/16.
 */
public class PubnubManager {
    /** Arbitrary used for UUID Generation
     * todo: this is unnecessary... */
    private static final int RANDOM_LARGE_NUMBER = 65535;

    /** Default Pubnub Client*/
    private static Pubnub pubnub;

    /** Channel Name connected */
    private static String currentChannelName;

    /** Public initializer */
    public static void init() {
        pubnub = new Pubnub(App.currentContext.getString(R.string.pubnub_publish_key),
                App.currentContext.getString(R.string.pubnub_subscribe_key));
        pubnub.setResumeOnReconnect(true);
    }

    /** @param newChannelName String New Channel Name to use */
    public static void setCurrentChannelName(String newChannelName) {
        currentChannelName = newChannelName;
    }

    /** @return Pubnub current client */
    public static Pubnub getPubnub() {
        return pubnub;
    }

    /** @return boolean on whether current channel is valid */
    public static boolean isCurrentChannelNameValid() {
        return currentChannelName != null && !currentChannelName.isEmpty();
    }

    /** @return boolean on whether client is connected */
    public static boolean isConnected() {
        return pubnub.getCurrentlySubscribedChannelNames() != "no channels.";
    }

    /** Joins Channel
     * @throws IllegalStateException if channel name is not valid */
    public static void joinChannel() {
        if(!isCurrentChannelNameValid()) {
            throw new IllegalStateException("Current Channel name cannot be null or empty");
        }
        try {
            pubnub.subscribe(currentChannelName, subscribeCallback());
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
                try {
                    JSONObject json = new JSONObject(message.toString());
                    Log.i(getClass().getSimpleName(), "Received message: " + json);
                    String uuid = json.getString("uuid");
                    if (uuid.equals(pubnub.uuid())) {
                        double lat = json.getDouble("lat");
                        double lng = json.getDouble("lng");
                        double alt = json.getDouble("alt");
                        Location loc = new Location("Pubnub Message");
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

    /** @return The current channel name
     * @throws IllegalStateException if not available
     * */
    public static String getCurrentChannelName() {
        if(currentChannelName == null || currentChannelName.isEmpty()) {
            throw new IllegalArgumentException("currentChannelName is not valid: It cannot be null or empty.");
        }
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
