package com.example.joseph.yipandroid3;

import android.app.Activity;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import static java.util.Locale.*;

/**
 * This is the main activity class for the Yip App
 *
 * Upon opening, the user will be brought to the main page with three options:
 * 1. Yip a friend
 * 2. Remember current location
 * 3. Yip an Address
 *
 *
 * When the user taps Yip a Friend, the app will open the contacts list for the user to select which
 * friend to yip
 *
 * When the user taps Yip an Address, the app will open
 *
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button yipAFriendBtn = (Button) findViewById(R.id.yipAFriend);
        yipAFriendBtn.setOnClickListener(yipAFriend);

        Button yipAnAddressBtn = (Button) findViewById(R.id.yipAnAddress);
        yipAnAddressBtn.setOnClickListener(yipAnAddress);

        Button rememberLocationBtn = (Button) findViewById(R.id.rememberLocation);
        rememberLocationBtn.setOnClickListener(rememberLocation);
    }

    // Create an anonymous implementation of OnClickListener
    private View.OnClickListener yipAFriend = new View.OnClickListener() {
        public void onClick(View v) {

        }
    };

    // Create an anonymous implementation of OnClickListener
    private View.OnClickListener rememberLocation = new View.OnClickListener() {
        public void onClick(View v) {

        }
    };

    // Create an anonymous implementation of OnClickListener
    private View.OnClickListener yipAnAddress = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(), CompassActivity.class);

            // search target location
            Geocoder geocoder = new Geocoder(v.getContext(), getDefault());
            List<Address> addresses = null;
            try {
                addresses = geocoder.getFromLocationName("United States", 1);

                Double lat = (double) (addresses.get(0).getLatitude());
                Double lng = (double) (addresses.get(0).getLongitude());

                // send other location as extra
                intent.putExtra("lat", lat);
                intent.putExtra("lng", lng);
                intent.putExtra("mode", App.YipType.ADDRESS_YIP);

                Log.i(this.getClass().getSimpleName(), "Switching... lat: " + lat + ", lng: " + lng);

                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


}
