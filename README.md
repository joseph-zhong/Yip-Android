#Yip for Android


## Dependencies
- [JScience](https://java.net/projects/jscience/downloads)
- [Google Play](https://developers.google.com/android/guides/setup#add_google_play_services_to_your_project)
- [PubNub](https://www.pubnub.com/docs/android-java/pubnub-java-sdk)
- [Branch.io](https://github.com/BranchMetrics/Android-Deferred-Deep-Linking-SDK)

Note: 
- There is a Debug and Production Pububnub API: enter `compile 'com.pubnub:pubnub-android-debug:3.7.+'` in the `app/build.gradle` dependencies if you need logging, otherwise just use `compile 'com.pubnub:pubnub-android:3.7.7'`
- All dependencies except JScience should be compiled with gradle 
- Download the `.jar` and enter a new Module and add 
`compile project(':<nameOfModule>')` to the app's build.gradle file under dependencies
- `app/src/res/values/api.xml` is gitignored, and must be written mannually
- See `api_example.xml` for reference
- There are two API Keys for Branch: test and live; make sure to differentiate
- Include both Branch API Keys, but change the `io.branch.sdk.TestMode` meta-data XML Element value in the `application` element in `AndroidManifest.xml` to `false` or `true`

## Features
- Yip to Address
- Yip a Friend
- Remember Location

## Development Log
- Compass (Completed)
- Range Indicator (Not started)
- yip an Address (Completed)
- PubNub (Testing)
- yip a friend data transaction (started)
- Branch (Started)
- launcher activity
