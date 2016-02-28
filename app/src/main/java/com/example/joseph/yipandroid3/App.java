package com.example.joseph.yipandroid3;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;

import com.pubnub.api.Pubnub;

/**
 * Created by Joseph on 12/19/15.
 * Updated by Joseph on 12/19/15.
 *
 * This class extends Android's base Application class to allow us to keep track of important globals */
public class App extends Application {
    /** Global Pubnub client */
    public static Pubnub pubnub;

    /**  */
    public static Activity activity;

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

//    var LOCATION_FACTORY_GLOBAL : LocationFactory! // THE TOKEN LOCATION FACTORY OF THE APP. IMMEDIATELY INITIALIZE IT ON APP OPEN

    // GLOBAL CONSTANTS
//    let ACCENT_COLOR_DARK : UIColor = UIColor(red: 107.0/255.0, green: 141.0/255.0, blue: 105.0/255.0, alpha: 1.0)

//    let SIDE_MARGIN : CGFloat = 40.0 // this should be no less than 10

//    let COMPASS_BORDER_DRAW_DURATION : Double = 0.85
//    let DISPLACEMENT_DURATION : Double = 0.65
//    let MAP_FADE_ANIMATION_DURATION : Double = 1.00 // 0.30 is old value
//    var MAP_WARMUP_DELAY : Double = 0.4 // the delay in map fading in after compass layer starts drawing

//    let BUTTON_BORDER_WIDTH : CGFloat = 1.0
//    let BUTTON_BORDER_RADIUS : CGFloat = 12.0

//    let MAP_LOCKED_ZOOM_LEVEL : Float = 20.0 //18.5

//    let MARKED_LOCATION_KEY : String = "markedLocation"

//    let GPS_WARM_UP_TIME : Double = 5.0

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
