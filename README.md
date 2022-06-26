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

## Microservices / Server
There are three directory, one for service. Every service has his gradle project so you can use usual commands for build local project and test:

Build with ```./gradlew build```

Run tests with ```./gradlew test```

Furthermore, it is possible to build local version using Docker, and the command is slightly more complicated:

For auth service: ```docker build . -t auth-ms``` and then ```docker run auth-ms -p 8080:8080```

For other services: ```docker build . -t other-ms``` and then ```docker run other-ms -p 3001:3000```, being careful not to use the same socket twice.
