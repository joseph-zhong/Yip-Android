package com.example.joseph.yipandroid3;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.location.Location;

import com.pubnub.api.Pubnub;

/**
 * Created by Joseph on 12/19/15.
 *
 * This class extends Android's base Application class to allow us to keep track of important globals */
public class App extends Application {
    /** Yip Types */
    public enum YipType {
        REMEMBERED_LOCATION_YIP,
        ADDRESS_YIP,
        TWO_USERS_YIP
    }

    /** Connection Status */
    public enum ConnectionStatus {
        COLD,
        WARMING_UP,
        GOOD,
        POOR,
        WEAK,
        NONE
    }

    /** Compass Status */
    enum CompassStatus {
        LOADING,
        WAITING_FOR_FRIEND,
        READY
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }
}
