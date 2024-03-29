package com.example.joseph.yipandroid3;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;


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
public class MainActivity extends Activity implements
        ConnectionCallbacks, LocationListener, OnConnectionFailedListener {
    /** API Client */
    private GoogleApiClient mGoogleApiClient;

    private Button yipAFriendBtn;
    private Button yipAnAddressBtn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initElements();
        App.currentContext = this.getApplicationContext();

        ArrayList<String> permissionsToReq = new ArrayList<>();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToReq.add(Manifest.permission_group.LOCATION);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToReq.add(Manifest.permission_group.CONTACTS);
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToReq.add(Manifest.permission.SEND_SMS);
        }
        if(!permissionsToReq.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToReq.toArray(new String[0]), LocationService.REQUEST_LOCATION_ID);
        }
    }

    /** Helper to initialize elements */
    private void initElements() {
        yipAFriendBtn = (Button) findViewById(R.id.yipAFriend);
        yipAFriendBtn.setOnClickListener(yipAFriendListener);
        yipAFriendBtn.setVisibility(View.INVISIBLE);

        yipAnAddressBtn = (Button) findViewById(R.id.yipAnAddress);
        yipAnAddressBtn.setOnClickListener(yipAnAddressListener);
        yipAnAddressBtn.setVisibility(View.INVISIBLE);

        Button rememberLocationBtn = (Button) findViewById(R.id.rememberLocation);
        rememberLocationBtn.setOnClickListener(rememberLocationListener);
        rememberLocationBtn.setVisibility(View.INVISIBLE);

        buildGoogleApiClient();
    }

    /** Initialize Google API Client
     * Adds Location Services API */
    private synchronized void buildGoogleApiClient() {
        if (this.mGoogleApiClient == null) {
            this.mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            this.mGoogleApiClient.connect();
        }
    }

    /** Select Yip A Friend Mode */
    private View.OnClickListener yipAFriendListener = new View.OnClickListener() {
        public void onClick(View v) {
            startActivityForResult(new Intent(getApplicationContext(), ContactsPickerActivity.class),
                    getResources().getInteger(R.integer.contact_success));
        }
    };

    /** Activity Result Listener to catch return data */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "No one selected", Toast.LENGTH_SHORT).show();
        } else if (requestCode == getResources().getInteger(R.integer.contact_success)) {
            String contact = (String) data.getExtras().get(ContactsPickerActivity.KEY_CONTACT_NAME);
            String number = (String) data.getExtras().get(ContactsPickerActivity.KEY_PHONE_NUMBER);
            Toast.makeText(this, "yip request sent to : " + contact + "!", Toast.LENGTH_SHORT).show();

            sendSMSHelper(number);

            Intent intent = new Intent(getApplicationContext(), CompassActivity.class);
            intent.putExtra("mode", App.YipType.TWO_USERS_YIP);

            startActivity(intent);
        }
        //Checking if the previous activity is launched on Branch Auto deep link.
        else if (requestCode == getResources().getInteger(R.integer.deeplink_success)) {
            //Decide here where to navigate when an auto deep linked activity finishes.
            finish();
        }
    }

    /**
     * Helper to create the custom */
    private void sendSMSHelper(String number) {
        BranchUniversalObject branchUniversalObject = new BranchUniversalObject()
                .setCanonicalIdentifier("Yip Request")
                .setTitle("Yip me!")
                .setContentDescription("Request to Yip")
                .setContentIndexingMode(BranchUniversalObject.CONTENT_INDEX_MODE.PUBLIC)
                .addContentMetadata("yip_channel", PubnubManager.getNewChannelName())
                .addContentMetadata("uuid", PubnubManager.getPubnub().uuid());

        // set fallback here
        LinkProperties linkProperties = new LinkProperties()
                .setChannel("facebook")
                .setFeature("sharing")
//                .addControlParameter("$fallback_url", getString(R.string.fallback_url));
                .addControlParameter("$fallback_url", "http://josephzhong.me");

        branchUniversalObject.generateShortUrl(this, linkProperties, new Branch.BranchLinkCreateListener() {
            @Override
            public void onLinkCreate(String url, BranchError error) {
                if (error == null) {
                    Log.i(this.getClass().getSimpleName(), "got my Branch link to share: " + url);
                    // todo: dynamic number
                    SmsService.sendSMS("4256287248", "Yip me! \n " + url);
//                    SmsService.sendSMS(number, "Yip me! \n " + url);
                }
            }
        });
    }

    /** Select Set Location Mode */
    private View.OnClickListener rememberLocationListener = new View.OnClickListener() {
        public void onClick(View v) {
            // todo: set location
        }
    };

    /** Select Yip an Address Mode */
    private View.OnClickListener yipAnAddressListener = new View.OnClickListener() {
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(), CompassActivity.class);
            intent.putExtra("mode", App.YipType.ADDRESS_YIP);
            startActivity(intent);
        }
    };


    @Override
    public void onConnected(Bundle bundle) {
        Log.i(this.getClass().getSimpleName(), "Google Location Services Connected");
        LocationService.attemptLocationUpdates(this.mGoogleApiClient, this);
    }

    @Override
    /** Captures Request Permission Results */
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case LocationService.REQUEST_LOCATION_ID: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    LocationService.attemptLocationUpdates(mGoogleApiClient, this);
                } else {
                    // todo: ??? what to do w/o permissions :(
                }
                return;
            }
        }
    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(this.getClass().getSimpleName(), "Connection to Google API suspended");
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(this.getClass().getSimpleName(), "Location set: "
                + location.getLatitude() + " " + location.getLongitude());
        LocationService.currentLocation = location;

        yipAFriendBtn.setVisibility(View.VISIBLE);
        yipAnAddressBtn.setVisibility(View.VISIBLE);
        TextView warmUpLabel = (TextView) findViewById(R.id.warm_up_label);
        warmUpLabel.setVisibility(View.INVISIBLE);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(this.getClass().getSimpleName(), "Error in connecting Google API");
    }

    @Override
    public void onStart() {
        super.onStart();
        PubnubManager.init();
        /**
         * Deep link via push notif
         */
//        Intent resultIntent = new Intent(this, TargetClass.class);
//        intent.putExtra("branch","http://bnc.lt/testlink");
//        PendingIntent resultPendingIntent =  PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Branch.getInstance().initSession(new Branch.BranchReferralInitListener() {
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    try {
                        Log.i(this.getClass().getSimpleName(), referringParams.toString(4));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.i(this.getClass().getSimpleName(), error.getMessage());
                }
            }
        }, this.getIntent().getData(), this);

    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }
}
