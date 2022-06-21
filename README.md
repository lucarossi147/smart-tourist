# smart-tourist
Smart app for tourism

The Android application is developed in the ```android_app``` branch whilst the server and all the microservices it is composed of is developed under the ```server``` branch.

## Prerequisites
A working installation of ```java``` and ```gradle```.
If you work with Android make sure to install the latest SDK and 

## Android App

To work with the Android app simply move to the ```SmartTourist``` directory.

Build with ```./gradlew build```

Run tests with ```./gradlew test```

If you want to try out the app on your smartphone you can either install the **debug** or **release** APK.

First connect your smartphone and enable it to transfer files.

For the Debug installation ```./gradlew installDebug```

For the Release installation ```./gradlew installRelease```
